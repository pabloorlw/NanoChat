package es.um.redes.nanoChat.directory.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class DirectoryThread extends Thread {

	// Tamaño máximo del paquete UDP
	private static final int PACKET_MAX_SIZE = 128;
	// Estructura para guardar las asociaciones ID_PROTOCOLO -> Dirección del
	// servidor
	protected Map<Integer, InetSocketAddress> servers;

	// Socket de comunicación UDP
	protected DatagramSocket socket = null;
	// Probabilidad de descarte del mensaje
	protected double messageDiscardProbability;

	public DirectoryThread(String name, int directoryPort, double corruptionProbability) throws SocketException {
		super(name);
		// TODO (HECHO)Anotar la dirección en la que escucha el servidor de Directorio
		InetSocketAddress serverAddress = new InetSocketAddress(directoryPort);
		// TODO (HECHO) Crear un socket de servidor
		socket = new DatagramSocket(serverAddress);
		messageDiscardProbability = corruptionProbability;
		// Inicialización del mapa
		servers = new HashMap<Integer, InetSocketAddress>();
	}

	@Override
	public void run() {
		byte[] buf = new byte[PACKET_MAX_SIZE];

		System.out.println("Directory starting...");
		boolean running = true;
		while (running) {

			// TODO (HECHO) 1) Recibir la solicitud por el socket
			DatagramPacket dpRec = new DatagramPacket(buf, buf.length);
			try {
				socket.receive(dpRec);
			} catch (IOException e) {
				e.printStackTrace();
			}
			// TODO (HECHO) 2) Extraer quién es el cliente (su dirección)
			InetSocketAddress clientAddress = (InetSocketAddress) dpRec.getSocketAddress();
			// 3) Vemos si el mensaje debe ser descartado por la probabilidad de descarte

			double rand = Math.random();
			if (rand < messageDiscardProbability) {
				System.err.println("Directory DISCARDED corrupt request from... ");
				continue;
			}

			// TODO (HECHO) (Solo Boletín 2) Devolver una respuesta idéntica en contenido a
			// la solicitud
			// byte[] bufRec = dpRec.getData();
			// String strToConvert = new String(bufRec);
			// String strConverted = strToConvert.toUpperCase();
			// byte[] bufSend = strConverted.getBytes();
			// DatagramPacket dpSend = new DatagramPacket(bufSend, bufSend.length,
			// clientAdress);
			// try {
			// socket.send(dpSend);
			// } catch (IOException e){
			// e.printStackTrace();
			// }
			// buf = new byte[PACKET_MAX_SIZE]; //Resetear buffer para proxima recepcion.
			// TODO 4)(hecho) Analizar y procesar la solicitud (llamada a processRequestFromCLient)
			// TODO 5) (hecho) Tratar las excepciones que puedan producirse
			try {
				processRequestFromClient(dpRec.getData(), clientAddress);
			} catch (IOException e) {
				e.printStackTrace();
			}

			buf = new byte[PACKET_MAX_SIZE]; // Resetear buffer
		}
		socket.close();
	}

	// Método para procesar la solicitud enviada por clientAddr
	public void processRequestFromClient(byte[] data, InetSocketAddress clientAddr) throws IOException {
		// TODO 1) (hecho) Extraemos el tipo de mensaje recibido
		ByteBuffer ret = ByteBuffer.wrap(data);
		byte opCode = ret.get();
		// TODO 2) Procesar el caso de que sea un registro y enviar mediante sendOK
		// TODO 3) Procesar el caso de que sea una consulta
				// TODO 3.1) Devolver una dirección si existe un servidor (sendServerInfo)
				// TODO 3.2) Devolver una notificación si no existe un servidor (sendEmpty)
		switch (opCode) {
		case 1: {
			byte protocolId = ret.get();
			int port = ret.getInt();
			System.out.println("Incoming message, opCode = " + Byte.toString(opCode) + " (register chat server)"
					+ ", protocol = " + Byte.toString(protocolId) + ", port = " + Integer.toString(port));
			InetAddress chatserverAddress = clientAddr.getAddress();
			InetSocketAddress chatserverSocketAddress = new InetSocketAddress(chatserverAddress, port);
			//Si ya existe un servidor en el protocolo no se hace nada
			if (servers.containsKey((int)protocolId)) {
				return;
			}
			servers.put((int)protocolId, chatserverSocketAddress);
			System.out.println("Value of servers (Map):");
			for (Map.Entry<Integer, InetSocketAddress> entry : servers.entrySet()) {
				Integer key = entry.getKey();
				InetSocketAddress value = entry.getValue();
				String entry_address = value.getAddress().toString().substring(1);
				Integer entry_port = value.getPort();
				System.out.println(key.toString() + ": " + entry_address + " - " + entry_port.toString());
			}
			sendOK(clientAddr);
			break;
		}
		case 3: {
			byte protocolId = ret.get();
			InetSocketAddress serverAddress = servers.get((int)protocolId);
			if (serverAddress == null) {
				sendEmpty(clientAddr);
			} else {
				sendServerInfo(serverAddress, clientAddr);
			}
			System.out.println("Sending chat server info for protocol " + protocolId);
			break;
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + opCode);
		}
		
	}

	// Método para enviar una respuesta vacía (no hay servidor)
	private void sendEmpty(InetSocketAddress clientAddr) throws IOException {
		// TODO Construir respuesta
		ByteBuffer bb = ByteBuffer.allocate(1);
		byte opCode = 5;
		bb.put(opCode);
		byte[] men = bb.array();
		// TODO Enviar respuesta
		DatagramPacket dpSend = new DatagramPacket (men, men.length, clientAddr);
		socket.send(dpSend);
	}

	// Método para enviar la dirección del servidor al cliente
	private void sendServerInfo(InetSocketAddress serverAddress, InetSocketAddress clientAddr) throws IOException {
		// TODO Obtener la representación binaria de la dirección
		// TODO Construir respuesta
		InetAddress direccion = serverAddress.getAddress();
		int port = serverAddress.getPort();
		byte[] direccionSplit = direccion.getAddress();
		ByteBuffer bb = ByteBuffer.allocate(9);
		byte opCode = 4;
		bb.put(opCode);
		bb.put(direccionSplit[0]);
		bb.put(direccionSplit[1]);
		bb.put(direccionSplit[2]);
		bb.put(direccionSplit[3]);
		bb.putInt(port);
		byte[] men = bb.array();
		// TODO Enviar respuesta
		DatagramPacket dpSend = new DatagramPacket(men, men.length, clientAddr);
		socket.send(dpSend);
	}

	// Método para enviar la confirmación del registro
	private void sendOK(InetSocketAddress clientAddr) throws IOException {
		// TODO (hecho)Construir respuesta
		ByteBuffer bb = ByteBuffer.allocate(1);
		byte opCode = 2;
		bb.put(opCode);
		byte[] men = bb.array();
		// TODO (hecho)Enviar respuesta
		DatagramPacket dpSend = new DatagramPacket (men, men.length, clientAddr);
		socket.send(dpSend);
	}
}
