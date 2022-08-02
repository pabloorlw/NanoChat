package es.um.redes.nanoChat.client.comm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import es.um.redes.nanoChat.messageML.NCControlMessage;
import es.um.redes.nanoChat.messageML.NCListMessage;
import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.messageML.NCRoomMessage;
import es.um.redes.nanoChat.messageML.NCThreeParameterMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

//Esta clase proporciona la funcionalidad necesaria para intercambiar mensajes entre el cliente y el servidor de NanoChat
public class NCConnector {
	private static final boolean VERBOSE_MODE = false;
	private static final SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yy HH:mm:ss.SSSS"); 
	
	private Socket socket;
	protected DataOutputStream dos;
	protected DataInputStream dis;
	
	public NCConnector(InetSocketAddress serverAddress) throws UnknownHostException, IOException {
		//TODO (hecho)Se crea el socket a partir de la dirección proporcionada
		//INFO: Una instancia de NCConnector es creada desde el método "connectToChatServer"
		// del NCController asociado al cliente de chat (NanoChat). Aquí hay que hacer la 
		// solicitud de conexión, creando un socket contra la InetSocketAddress del servidor 
		// de chat.
		this.socket = new Socket (serverAddress.getAddress(), serverAddress.getPort());
		//TODO (hecho) Se extraen los streams de entrada y salida.
		// INFO: En "dos" (DataOutputStream) dejaremos (cuando proceda) todo lo que queramos 
		// enviar al servidor de chat a través de "socket". En "dis" (DataInputStream) iremos 
		// obteniendo (cuando proceda) todo lo que nos vaya enviando el servidor de chat.
		dos = new DataOutputStream(socket.getOutputStream());
		dis = new DataInputStream(socket.getInputStream());
	}


	//Método para registrar el nick en el servidor. Nos informa sobre si la inscripción se hizo con éxito o no.
	public boolean registerNickname_UnformattedMessage(String nick) throws IOException {
		//Funcionamiento resumido: SEND(nick) and RCV(NICK_OK) or RCV(NICK_DUPLICATED)
		//TODO (hecho) Enviamos una cadena con el nick por el flujo de salida
		dos.writeUTF(nick);
		//TODO (hecho) Leemos la cadena recibida como respuesta por el flujo de entrada
		String rcv = dis.readUTF();
		//TODO (hecho) Si la cadena recibida es NICK_OK entonces no está duplicado (en función de ello modificar el return)
		boolean resp = false;
		if (rcv.equals("NICK_OK")) {
			resp = true;
		}
		return resp;
	}

	
	//Método para registrar el nick en el servidor. Nos informa sobre si la inscripción se hizo con éxito o no.
	public boolean registerNickname(String nick) throws IOException {
		//Funcionamiento resumido: SEND(nick) and RCV(NICK_OK) or RCV(NICK_DUPLICATED)
		//Creamos un mensaje de tipo RoomMessage con opcode OP_NICK en el que se inserte el nick
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_NICK, nick);
		//Obtenemos el mensaje de texto listo para enviar
		String rawMessage = message.toEncodedString();
		//Escribimos el mensaje en el flujo de salida, es decir, provocamos que se envíe por la conexión TCP
		showMessageInConsole(rawMessage);
		dos.writeUTF(rawMessage);
		//TODO (hecho) Leemos el mensaje recibido como respuesta por el flujo de entrada 
		NCControlMessage messageResp = (NCControlMessage) NCMessage.readMessageFromSocket(dis);
		//TODO Analizamos el mensaje para saber si está duplicado el nick (modificar el return en consecuencia)
		byte opCode = messageResp.getOpcode();
		boolean resp = false;
		if (opCode == NCMessage.OP_NICK_OK) {
			resp = true;
		}
		return resp;
	}
	
	public void createRoom() throws IOException{
		NCControlMessage message = (NCControlMessage) NCMessage.makeControlMessage(NCMessage.OP_CREATE_ROOM);
		String rawMessage = message.toEncodedString();
		showMessageInConsole(rawMessage);
		dos.writeUTF(rawMessage);
	}
	
	//Método para obtener la lista de salas del servidor
	public List<NCRoomDescription> getRooms() throws IOException {
		//Funcionamiento resumido: SND(GET_ROOMS) and RCV(ROOM_LIST)
		//TODO (hecho) completar el método
		NCControlMessage message = (NCControlMessage) NCMessage.makeControlMessage(NCMessage.OP_ROOM_LIST_QUERY);
		String rawMessage = message.toEncodedString();
		showMessageInConsole(rawMessage);
		dos.writeUTF(rawMessage);
		NCListMessage messageResp = (NCListMessage) NCMessage.readMessageFromSocket(dis);
		return messageResp.getList();
	}
	
	//Método para solicitar la entrada en una sala
	public boolean enterRoom(String room) throws IOException {
		//Funcionamiento resumido: SND(ENTER_ROOM<room>) and RCV(IN_ROOM) or RCV(REJECT)
		//TODO (hecho) completar el método
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_ENTER_ROOM, room);
		String rawMessage = message.toEncodedString();
		showMessageInConsole(rawMessage);
		dos.writeUTF(rawMessage);
		NCControlMessage messageResp = (NCControlMessage) NCMessage.readMessageFromSocket(dis);
		byte opCode = messageResp.getOpcode();
		boolean resp = false;
		if (opCode == NCMessage.OP_ENTER_ROOM_OK) {
			resp = true;
		}
		return resp;
	}
	
	//Método para salir de una sala
	public void leaveRoom() throws IOException {
		//Funcionamiento resumido: SND(EXIT_ROOM)
		NCControlMessage message = (NCControlMessage) NCMessage.makeControlMessage(NCMessage.OP_EXIT);
		String rawMessage = message.toEncodedString();
		showMessageInConsole(rawMessage);
		dos.writeUTF(rawMessage);
	}
	
	//Método que utiliza el Shell para ver si hay datos en el flujo de entrada
	public boolean isDataAvailable() throws IOException {
		return (dis.available() != 0);
	}
	
	//IMPORTANTE!!
	//TODO Es necesario implementar métodos para recibir y enviar mensajes de chat a una sala
	
	public void sendMessage(String m) throws IOException{
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_SEND, m);
		String rawMessage = message.toEncodedString();
		showMessageInConsole(rawMessage);
		dos.writeUTF(rawMessage);
	}
	
	public NCMessage receiveMessage() throws IOException{
		NCMessage messageResp = NCMessage.readMessageFromSocket(dis);
		return messageResp;
	}
	
	public void sendPrivateMessage (String user, String mensaje) throws IOException{
		NCThreeParameterMessage message = (NCThreeParameterMessage) NCMessage.makeThreeParameterMessage(NCMessage.OP_SEND_PRIVATE, user, mensaje);
		String rawMessage = message.toEncodedString();
		showMessageInConsole(rawMessage);
		dos.writeUTF(rawMessage);
	}
	
	public boolean renameRoom(String name) throws IOException {
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_ROOM_RENAME, name);
		String rawMessage = message.toEncodedString();
		showMessageInConsole(rawMessage);
		dos.writeUTF(rawMessage);
		NCControlMessage messageResp = (NCControlMessage) NCMessage.readMessageFromSocket(dis);
		byte opCode = messageResp.getOpcode();
		if (opCode == NCMessage.OP_ROOM_RENAME_OK) {
			return true;
		}
		return false;
		
	}
	
	//Método para pedir la descripción de una sala
	public NCRoomDescription getRoomInfo() throws IOException {
		//Funcionamiento resumido: SND(GET_ROOMINFO) and RCV(ROOMINFO)
		//TODO Construimos el mensaje de solicitud de información de la sala específica
		NCControlMessage message = (NCControlMessage) NCMessage.makeControlMessage(NCMessage.OP_INFO_QUERY);
		String rawMessage = message.toEncodedString();
		showMessageInConsole(rawMessage);
		dos.writeUTF(rawMessage);
		//TODO Recibimos el mensaje de respuesta
		NCListMessage messageResp = (NCListMessage) NCMessage.readMessageFromSocket(dis);
		List<NCRoomDescription> descripcion = messageResp.getList();
		//TODO Devolvemos la descripción contenida en el mensaje
		return descripcion.get(0);
	}
	
	//Método para cerrar la comunicación con la sala
	//TODO (Opcional) Enviar un mensaje de salida del servidor de Chat
	public void disconnect() {
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
		} finally {
			socket = null;
		}
	}
	
	//Metodo para ir viendo los mensajes que se van enviando. PAra depuracion. Se pone la llamada cada vez que se haga dos.writeUTF
	private void showMessageInConsole(String message) {
		if (VERBOSE_MODE) {
			Date currentDateTime = new Date(System.currentTimeMillis());
			String currentDateTimeText = formatter.format(currentDateTime);
			System.out.println("\nMESSAGE (" + currentDateTimeText + ") ········");
			System.out.println(message);
			System.out.println("···································(end of message)\n");
		}
	}

}
