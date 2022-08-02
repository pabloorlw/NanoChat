package es.um.redes.nanoChat.directory.connector;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * Cliente con métodos de consulta y actualización específicos del directorio
 */
public class DirectoryConnector {
	//Tamaño máximo del paquete UDP (los mensajes intercambiados son muy cortos)
	private static final int PACKET_MAX_SIZE = 128;
	//Puerto en el que atienden los servidores de directorio
	private static final int DEFAULT_PORT = 6868;
	//Valor del TIMEOUT
	private static final int TIMEOUT = 1000;

	private DatagramSocket socket; // socket UDP
	private InetSocketAddress directoryAddress; // dirección del servidor de directorio

	public DirectoryConnector(String agentAddress) throws IOException {
		//TODO (HECHO) A partir de la dirección y del puerto generar la dirección de conexión para el Socket
		directoryAddress = new InetSocketAddress(InetAddress.getByName(agentAddress), DEFAULT_PORT);
		//TODO (HECHO) Crear el socket UDP
		socket = new DatagramSocket();
	}
	
	public String convertToUpper(String strToConvert) throws IOException {
		byte[] bufSend = strToConvert.getBytes();
		DatagramPacket dpSend = new DatagramPacket (bufSend, bufSend.length, directoryAddress);
		byte[] bufRec = new byte[PACKET_MAX_SIZE];
		DatagramPacket dpRec = new DatagramPacket(bufRec, bufRec.length);
		int i = 0;
		while (i < 5) {
			socket.send(dpSend);
			socket.setSoTimeout(TIMEOUT);
			try {
				socket.receive(dpRec);
			} catch (SocketTimeoutException e){
				i++;
				System.out.println("Aumentando el contador a " + i);
				System.out.println();
				continue;
			}
			break;
		}
		String strConverted;
		if (i == 5) {
			strConverted = new String ("ERROR");
		} else {
			strConverted = new String(dpRec.getData());
		}
		return strConverted;
	}

	/**
	 * Envía una solicitud para obtener el servidor de chat asociado a un determinado protocolo
	 * 
	 */
	public InetSocketAddress getServerForProtocol(int protocol) throws IOException {

		//TODO (hecho)Generar el mensaje de consulta llamando a buildQuery()
		byte[] men = buildQuery(protocol);
		//TODO (hecho) Construir el datagrama con la consulta
		DatagramPacket dpSend = new DatagramPacket(men, men.length, directoryAddress);
		//TODO (hecho) Enviar datagrama por el socket
		//TODO (hecho) preparar el buffer para la respuesta
		byte[] res = new byte[PACKET_MAX_SIZE];
		DatagramPacket dpRec = new DatagramPacket(res, res.length);
		//TODO (hecho) Establecer el temporizador para el caso en que no haya respuesta
		//TODO (hecho) Recibir la respuesta
		int i = 0;
		while (i < 3){
			socket.send(dpSend);
			socket.setSoTimeout(TIMEOUT);
			try {
				socket.receive(dpRec);
			} catch (SocketTimeoutException e) {
				i++;
				System.out.println("Aumentando el contador a " + i);
				continue;
			}
			break;
		}
		//TODO (hecho) Procesamos la respuesta para devolver la dirección que hay en ella
		if (i == 3) {
			throw new IOException("It wasn't possible to get the server for the protocol wanted");
		}
		return getAddressFromResponse(dpRec);
	}


	//Método para generar el mensaje de consulta (para obtener el servidor asociado a un protocolo)
	private byte[] buildQuery(int protocol) {
		//TODO Devolvemos el mensaje codificado en binario según el formato acordado
		ByteBuffer bb = ByteBuffer.allocate(2);
		byte opCode = 3;
		bb.put(opCode);
		byte protocolId = ((Integer)protocol).byteValue();
		bb.put(protocolId);
		byte[] men = bb.array();
		return men;
	}

	//Método para obtener la dirección de internet a partir del mensaje UDP de respuesta
	private InetSocketAddress getAddressFromResponse(DatagramPacket packet) throws UnknownHostException {
		//TODO Analizar si la respuesta no contiene dirección (devolver null)
		ByteBuffer bb = ByteBuffer.wrap(packet.getData());
		byte opCode = bb.get();
		if (opCode == 5) {
			return null;
		}
		//TODO Si la respuesta no está vacía, devolver la dirección (extraerla del mensaje)
		int ip1 = Byte.toUnsignedInt(bb.get());
		int ip2 = Byte.toUnsignedInt(bb.get());
		int ip3 = Byte.toUnsignedInt(bb.get());
		int ip4 = Byte.toUnsignedInt(bb.get());
		int port = bb.getInt();
		String ip = Integer.toString(ip1) + "." + Integer.toString(ip2) + "." + Integer.toString(ip3) + "." + Integer.toString(ip4);
		InetSocketAddress resultado = new InetSocketAddress(ip, port);
		return resultado;
	}
	
	/**
	 * Envía una solicitud para registrar el servidor de chat asociado a un determinado protocolo
	 * 
	 */
	public boolean registerServerForProtocol(int protocol, int port) throws IOException {
		boolean resp = false;
		//TODO Construir solicitud de registro (buildRegistration)
		byte[] men =  buildRegistration(protocol, port);
		//TODO Enviar solicitud
		DatagramPacket dpSend = new DatagramPacket(men, men.length, directoryAddress);
		byte[] res = new byte[PACKET_MAX_SIZE];
		DatagramPacket dpRec = new DatagramPacket(res, res.length);
		int i = 0;
		while (i < 3) {
			socket.send(dpSend);
			socket.setSoTimeout(TIMEOUT);
			try {
				socket.receive(dpRec);
			} catch (SocketTimeoutException e) {
				i++;
				System.out.println("Aumentando el contador a " + i);
				continue;
			}
			break;
		}
		if (i == 3) {
			return resp;
		}
		//TODO Procesamos la respuesta para ver si se ha podido registrar correctamente
		ByteBuffer ret = ByteBuffer.wrap(dpRec.getData());
		byte opCode = ret.get();
		if (opCode == 2) {
			resp = true;
		}
		return resp;
	}


	//Método para construir una solicitud de registro de servidor
	//OJO: No hace falta proporcionar la dirección porque se toma la misma desde la que se envió el mensaje
	private byte[] buildRegistration(int protocol, int port) {
		//TODO Devolvemos el mensaje codificado en binario según el formato acordado
		ByteBuffer bb = ByteBuffer.allocate(6);
		byte opCode = 1;
		byte protocolId = ((Integer)protocol).byteValue();
		bb.put(opCode);
		bb.put(protocolId);
		bb.putInt(port);
		byte[] men = bb.array();
		return men;
	}

	public void close() {
		socket.close();
	}
}
