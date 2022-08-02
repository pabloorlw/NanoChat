package es.um.redes.nanoChat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import es.um.redes.nanoChat.messageML.NCRoomMessage;
import es.um.redes.nanoChat.messageML.NCThreeParameterMessage;
import es.um.redes.nanoChat.messageML.NCControlMessage;
import es.um.redes.nanoChat.messageML.NCListMessage;
import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoom;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;
import es.um.redes.nanoChat.server.roomManager.NCRoomManager;

/**
 * A new thread runs for each connected client
 */
public class NCServerThread extends Thread {
	
	private static final boolean VERBOSE_MODE = true;
	private static final SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yy HH:mm:ss.SSSS"); 
	
	private Socket socket = null;
	//Manager global compartido entre los Threads
	private NCServerManager serverManager = null;
	//Input and Output Streams
	private DataInputStream dis;
	private DataOutputStream dos;
	//Usuario actual al que atiende este Thread
	String user;
	//RoomManager actual (dependerá de la sala a la que entre el usuario)
	NCRoomManager roomManager;
	//Sala actual
	String currentRoom;

	//Inicialización de la sala
	public NCServerThread(NCServerManager manager, Socket socket) throws IOException {
		super("NCServerThread");
		this.socket = socket;
		this.serverManager = manager;
	}

	//Main loop
	public void run() {
		try {
			//Se obtienen los streams a partir del Socket
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
			//En primer lugar hay que recibir y verificar el nick
			receiveAndVerifyNickname();
			//Mientras que la conexión esté activa entonces...
			while (true) {
				//TODO Obtenemos el mensaje que llega y analizamos su código de operación
				NCMessage message = NCMessage.readMessageFromSocket(dis);
				switch (message.getOpcode()) {
					//TODO 1) si se nos pide la lista de salas se envía llamando a sendRoomList();
				case NCMessage.OP_ROOM_LIST_QUERY:			
				{
					sendRoomList();
					break;
				}
				//TODO 2) (hecho) Si se nos pide entrar en la sala entonces obtenemos el RoomManager de la sala,
				//TODO 2) (hecho) notificamos al usuario que ha sido aceptado y procesamos mensajes con processRoomMessages()
				//TODO 2) (hecho) Si el usuario no es aceptado en la sala entonces se le notifica al cliente
				case NCMessage.OP_ENTER_ROOM:
				{
					NCRoomMessage mensaje = (NCRoomMessage)message;
					String sala = mensaje.getName();
					roomManager = this.serverManager.enterRoom(user, sala, this.socket);
					if (roomManager != null) {
						NCControlMessage messageResp = (NCControlMessage) NCMessage.makeControlMessage(NCMessage.OP_ENTER_ROOM_OK);
						String rawMessageResp = messageResp.toEncodedString();
						showMessageInConsole(rawMessageResp);
						dos.writeUTF(rawMessageResp);
						currentRoom = sala;
						//Para informar a los demas de la entrada
						this.roomManager.broadcastIONotification(user, 1);
						processRoomMessages();
					}else {
						NCControlMessage messageResp = (NCControlMessage) NCMessage.makeControlMessage(NCMessage.OP_ENTER_ROOM_FAIL);
						String rawMessageResp = messageResp.toEncodedString();
						showMessageInConsole(rawMessageResp);
						dos.writeUTF(rawMessageResp);
					}
					break;
					
				}
				case NCMessage.OP_CREATE_ROOM:
				{
					serverManager.registerRoomManager(new NCRoom());
					break;
				}
				}
			}
		} catch (IOException e) {
			//If an error occurs with the communications the user is removed from all the managers and the connection is closed
			System.out.println("* User "+ user + " disconnected.");
			serverManager.leaveRoom(user, currentRoom);
			serverManager.removeUser(user);
		}
		finally {
			if (!socket.isClosed())
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		}
	}

	//Obtenemos el nick y solicitamos al ServerManager que verifique si está duplicado
	private void receiveAndVerifyNickname() throws IOException{
		//La lógica de nuestro programa nos obliga a que haya un nick registrado antes de proseguir
		//TODO (hecho) Entramos en un bucle hasta comprobar que alguno de los nicks proporcionados no está duplicado
		boolean userOk = false;
		while (!userOk) {
			NCRoomMessage message = (NCRoomMessage) NCMessage.readMessageFromSocket(dis);
			if (message != null) {
				byte opCode = message.getOpcode();
				if (opCode == NCMessage.OP_NICK) {
					//TODO (hecho) Extraer el nick del mensaje
					String nick = message.getName();
					//TODO (hecho) Validar el nick utilizando el ServerManager - addUser()
					userOk = serverManager.addUser(nick);
					//TODO (hecho)Contestar al cliente con el resultado (éxito o duplicado)
					NCControlMessage messageResp;
					if (userOk) {
						user = nick;
						messageResp = (NCControlMessage) NCMessage.makeControlMessage(NCMessage.OP_NICK_OK);
					} else {
						messageResp = (NCControlMessage) NCMessage.makeControlMessage(NCMessage.OP_DUPLICATED_NICK);
					}
					String rawMessageResp = messageResp.toEncodedString();
					showMessageInConsole(rawMessageResp);
					dos.writeUTF(rawMessageResp);
				}
			}
			
		}
		
	}

	//Mandamos al cliente la lista de salas existentes
	private void sendRoomList() throws IOException  {
		//TODO La lista de salas debe obtenerse a partir del RoomManager y después enviarse mediante su mensaje correspondiente
		List<NCRoomDescription> lista = this.serverManager.getRoomList();
		NCListMessage messageResp = (NCListMessage) NCMessage.makeListMessage(NCMessage.OP_ROOM_LIST, lista);
		String rawMessageResp = messageResp.toEncodedString();
		showMessageInConsole(rawMessageResp);
		dos.writeUTF(rawMessageResp);
		
	}

	private void processRoomMessages() throws IOException{
		//TODO Comprobamos los mensajes que llegan hasta que el usuario decida salir de la sala
		boolean exit = false;
		while (!exit) {
			//TODO Se recibe el mensaje enviado por el usuario
			NCMessage message = NCMessage.readMessageFromSocket(dis);
			//Refrescamos cada cierto tiempo el nombre de la sala por si alguien la hubiese cambiado.
			currentRoom = this.roomManager.getName();
			//TODO Se analiza el código de operación del mensaje y se trata en consecuencia
			byte opCode = message.getOpcode();
			switch (opCode) {
			case NCMessage.OP_INFO_QUERY:
			{
				NCRoomDescription descripcion = roomManager.getDescription();
				List<NCRoomDescription> info = new ArrayList<> ();
				info.add(descripcion);
				NCListMessage messageResp = (NCListMessage) NCMessage.makeListMessage(NCMessage.OP_INFO, info);
				String rawMessageResp = messageResp.toEncodedString();
				showMessageInConsole(rawMessageResp);
				dos.writeUTF(rawMessageResp);
				break;
			}
			case NCMessage.OP_SEND:
			{
				NCRoomMessage mensaje = (NCRoomMessage) message;
				String texto = mensaje.getName();
				this.roomManager.broadcastMessage(user, texto);
				break;
			}
			case NCMessage.OP_SEND_PRIVATE:
			{
				NCThreeParameterMessage mensaje = (NCThreeParameterMessage) message;
				String u = mensaje.getName();
				String texto = mensaje.getText();
				this.roomManager.sendMessage(this.user,u,texto);
				break;
			}
			case NCMessage.OP_EXIT:
			{
				//Para informar a los demas de la salida
				this.roomManager.broadcastIONotification(user, 2);
				this.serverManager.leaveRoom(user, currentRoom);
				currentRoom = null;
				roomManager = null;
				exit = true;
				break;
			}
			case NCMessage.OP_ROOM_RENAME:
			{
				NCRoomMessage mensaje = (NCRoomMessage) message;
				boolean resultado = this.serverManager.renameRoom(this.roomManager.getName(), mensaje.getName());
				NCControlMessage messageResp; 
				if (resultado) {
					currentRoom = mensaje.getName();
					messageResp = (NCControlMessage) NCMessage.makeControlMessage(NCMessage.OP_ROOM_RENAME_OK);
					String rawMessageResp = messageResp.toEncodedString();
					showMessageInConsole(rawMessageResp);
					dos.writeUTF(rawMessageResp);
				} else {
					messageResp = (NCControlMessage) NCMessage.makeControlMessage(NCMessage.OP_ROOM_RENAME_FAIL);
					String rawMessageResp = messageResp.toEncodedString();
					showMessageInConsole(rawMessageResp);
					dos.writeUTF(rawMessageResp);
				}
				
				break;
			}
			}
		}
	}
	
	//Metodo para ir viendo los mensajes que se van enviando. PAra depuracion. Se pone la llamada antes de cada vez que se haga dos.writeUTF
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
