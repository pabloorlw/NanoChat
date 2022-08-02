package es.um.redes.nanoChat.client.application;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import es.um.redes.nanoChat.client.comm.NCConnector;
import es.um.redes.nanoChat.client.shell.NCCommands;
import es.um.redes.nanoChat.client.shell.NCShell;
import es.um.redes.nanoChat.directory.connector.DirectoryConnector;
import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.messageML.NCRoomMessage;
import es.um.redes.nanoChat.messageML.NCThreeParameterMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

public class NCController {
	//Diferentes estados del cliente de acuerdo con el autómata
	private static final byte PRE_CONNECTION = 1;
	private static final byte PRE_REGISTRATION = 2;
	//Estado de cuando no está en ninguna sala pero si tiene nick ya:
	private static final byte OFF_ROOM = 3;
	//Estado de cuando estas dentro de una sala:
	private static final byte IN_ROOM = 4;
	//Código de protocolo implementado por este cliente
	//TODO Cambiar para cada grupo
	private static final int PROTOCOL = 104;
	//Conector para enviar y recibir mensajes del directorio
	private DirectoryConnector directoryConnector;
	//Conector para enviar y recibir mensajes con el servidor de NanoChat
	private NCConnector ncConnector;
	//Shell para leer comandos de usuario de la entrada estándar
	private NCShell shell;
	//Último comando proporcionado por el usuario
	private byte currentCommand;
	//Nick del usuario
	private String nickname;
	//Sala de chat en la que se encuentra el usuario (si está en alguna)
	private String room;
	//Mensaje enviado o por enviar al chat
	private String chatMessage;
	//Variable con el nombre con el que renombrar la sala
	private String roomName;
	//Usuario destinatario del mensaje privado:
	private String privateMessageUser;
	//Mensaje privado
	private String privateMessage;
	//Dirección de internet del servidor de NanoChat
	private InetSocketAddress serverAddress;
	//Estado actual del cliente, de acuerdo con el autómata
	private byte clientStatus = PRE_CONNECTION;

	//Constructor
	public NCController() {
		shell = new NCShell();
	}

	//Devuelve el comando actual introducido por el usuario
	public byte getCurrentCommand() {		
		return this.currentCommand;
	}

	//Establece el comando actual
	public void setCurrentCommand(byte command) {
		currentCommand = command;
	}

	//Registra en atributos internos los posibles parámetros del comando tecleado por el usuario
	public void setCurrentCommandArguments(String[] args) {
		//Comprobaremos también si el comando es válido para el estado actual del autómata
		switch (currentCommand) {
		case NCCommands.COM_NICK:
			if (clientStatus == PRE_REGISTRATION)
				nickname = args[0];
			break;
		case NCCommands.COM_ENTER:
			room = args[0];
			break;
		case NCCommands.COM_SEND:
			chatMessage = args[0];
			break;
		case NCCommands.COM_RENAME:
			roomName = args[0];
			break;
		case NCCommands.COM_SENDPRIVATE:
			privateMessageUser = args[0];
			privateMessage = args[1];
			break;
		default:
		}
	}

	//Procesa los comandos introducidos por un usuario que aún no está dentro de una sala
	public void processCommand() {
		switch (currentCommand) {
		case NCCommands.COM_NICK:
			if (clientStatus == PRE_REGISTRATION)
				registerNickName();
			else
				System.out.println("* You have already registered a nickname ("+nickname+")");
			break;
		case NCCommands.COM_ROOMLIST:
			//TODO (hecho) LLamar a getAndShowRooms() si el estado actual del autómata lo permite
			//TODO (hecho) Si no está permitido informar al usuario
			if (clientStatus == OFF_ROOM) {
				getAndShowRooms();
			} else {
				System.out.println("* That command is only valid if you are not in a room");
			}
			break;
		case NCCommands.COM_ENTER:
			//TODO (hecho) LLamar a enterChat() si el estado actual del autómata lo permite
			//TODO (hecho) Si no está permitido informar al usuario
			if (clientStatus == OFF_ROOM) {
				enterChat();
			} else {
				System.out.println("* That command is only valid if you are not in a room");
			}
			break;
		case NCCommands.COM_QUIT:
			//Cuando salimos tenemos que cerrar todas las conexiones y sockets abiertos
			ncConnector.disconnect();			
			directoryConnector.close();
			break;
		case NCCommands.COM_CREATEROOM:
			if (clientStatus == OFF_ROOM) {
				createRoom();
			} else {
				System.out.println("* That command is only valid if you are not in a room");
			}
			break;
		default:
		}
	}
	
	//Método para registrar el nick del usuario en el servidor de NanoChat
	private void registerNickName() {
		try {
			//Pedimos que se registre el nick (se comprobará si está duplicado)
			boolean registered = ncConnector.registerNickname(nickname);	//antes estaba con el unformated_message, que es para pruebas.
			//TODO: (hecho) Cambiar la llamada anterior a registerNickname() al usar mensajes formateados 
			if (registered) {
				//TODO (hecho)Si el registro fue exitoso pasamos al siguiente estado del autómata
				clientStatus = OFF_ROOM;
				System.out.println("* Your nickname is now "+nickname);
			}
			else
				//En este caso el nick ya existía
				System.out.println("* The nickname is already registered. Try a different one.");			
		} catch (IOException e) {
			System.out.println("* There was an error registering the nickname");
		}
	}

	//Método que solicita al servidor de NanoChat la lista de salas e imprime el resultado obtenido
	private void getAndShowRooms(){
		//TODO Lista que contendrá las descripciones de las salas existentes
		List<NCRoomDescription> salas;
		//TODO Le pedimos al conector que obtenga la lista de salas
		try {
			salas = ncConnector.getRooms();
			//TODO Una vez recibidas iteramos sobre la lista para imprimir información de cada sala
			for (NCRoomDescription descripcion: salas) {
				System.out.println(descripcion.toPrintableString());
			}
		} catch (IOException e) {
			System.out.println("* There was an error getting the list of rooms");
		}
		
	}

	//Método para tramitar la solicitud de acceso del usuario a una sala concreta
	private void enterChat() {
		//TODO (hecho) Se solicita al servidor la entrada en la sala correspondiente ncConnector.enterRoom()
		try {
			boolean resp = ncConnector.enterRoom(room);
			//TODO (hecho) Si la respuesta es un rechazo entonces informamos al usuario y salimos
			//TODO (hecho) En caso contrario informamos que estamos dentro y seguimos
			//TODO (hecho) Cambiamos el estado del autómata para aceptar nuevos comandos
			if (resp) {
				System.out.println("You have entered the room");
				clientStatus = IN_ROOM;
			} else {
				System.out.println("* Not able to enter the room specified.");	
				return;
			}		
			do {
				//Pasamos a aceptar sólo los comandos que son válidos dentro de una sala
				readRoomCommandFromShell();
				processRoomCommand();
			} while (currentCommand != NCCommands.COM_EXIT);
			System.out.println("* You are out of the room");
			//TODO (hecho) Llegados a este punto el usuario ha querido salir de la sala, cambiamos el estado del autómata
			clientStatus = OFF_ROOM;
		}catch (IOException e) {
			System.out.println("* There was an error entering the room");
		}
	}

	private void createRoom() {
		try {
			ncConnector.createRoom();
		} catch (IOException e) {
			System.out.println("* There was an error creating the next room");
		}
	}
	
	//Método para procesar los comandos específicos de una sala
	private void processRoomCommand() {
		switch (currentCommand) {
		case NCCommands.COM_ROOMINFO:
			//El usuario ha solicitado información sobre la sala y llamamos al método que la obtendrá
			getAndShowInfo();
			break;
		case NCCommands.COM_SEND:
			//El usuario quiere enviar un mensaje al chat de la sala
			sendChatMessage();
			break;
		case NCCommands.COM_SOCKET_IN:
			//En este caso lo que ha sucedido es que hemos recibido un mensaje desde la sala y hay que procesarlo
			processIncommingMessage();
			break;
		case NCCommands.COM_EXIT:
			//El usuario quiere salir de la sala
			exitTheRoom();
			break;
		case NCCommands.COM_RENAME:
			renameTheRoom();
			break;
		case NCCommands.COM_SENDPRIVATE:
			sendPrivateMessage();
			break;
		}	
	}

	//Método para solicitar al servidor la información sobre una sala y para mostrarla por pantalla
	private void getAndShowInfo() {
		try {
			//TODO Pedimos al servidor información sobre la sala en concreto
			NCRoomDescription descripcion = ncConnector.getRoomInfo();
			//TODO Mostramos por pantalla la información
			String s = descripcion.toPrintableString();
			System.out.println(s);
		} catch (IOException e) {
			System.out.println("* There was an error getting the room info");
		}
		
	}

	//Método para notificar al servidor que salimos de la sala
	private void exitTheRoom() {
		try {
			//TODO Mandamos al servidor el mensaje de salida
			ncConnector.leaveRoom();
			//TODO Cambiamos el estado del autómata para indicar que estamos fuera de la sala
			clientStatus = OFF_ROOM;
		} catch (IOException e) {
			System.out.println("* There was an error leaving the room");
		}
		
	}
	
	private void renameTheRoom() {
		try {
			boolean resultado = ncConnector.renameRoom(roomName);
			if (resultado) {
				room = roomName;
				System.out.println("You have renamed the current room to "+ roomName);
			} else {
				System.out.println("It wasn't possible to rename the room. Maybe there is already a room with that name");
			}
		}catch (IOException e) {
			System.out.println("* There was an error renaming the room");
		}
	}

	//Método para enviar un mensaje al chat de la sala
	private void sendChatMessage() {
		//TODO Mandamos al servidor un mensaje de chat
		try {
			ncConnector.sendMessage(chatMessage);				
		} catch (IOException e) {
			System.out.println("* There was an error sending the message");
		}
	}
	
	private void sendPrivateMessage() {
		//TODO Mandamos al servidor un mensaje de chat
		try {
			ncConnector.sendPrivateMessage(privateMessageUser, privateMessage);				
		} catch (IOException e) {
			System.out.println("* There was an error sending the message");
		}
	}

	//Método para procesar los mensajes recibidos del servidor mientras que el shell estaba esperando un comando de usuario
	private void processIncommingMessage() {
		try {
			//TODO Recibir el mensaje
			NCMessage mensaje = ncConnector.receiveMessage();
			//TODO En función del tipo de mensaje, actuar en consecuencia
			byte opCode = mensaje.getOpcode();
			switch (opCode) {
			case NCMessage.OP_SEND_IN:
			{
				NCThreeParameterMessage threeParameterMensaje = (NCThreeParameterMessage) mensaje;
				//TODO (Ejemplo) En el caso de que fuera un mensaje de chat de broadcast mostramos la información de quién envía el mensaje y el mensaje en sí
				System.out.println(threeParameterMensaje.getName() + ": " + threeParameterMensaje.getText());
				break;
			}
			case NCMessage.OP_SEND_IN_PRIVATE:
			{
				NCThreeParameterMessage threeParameterMensaje = (NCThreeParameterMessage) mensaje;
				System.out.println(threeParameterMensaje.getName() + "(private): " + threeParameterMensaje.getText());
				break;
			}
			case NCMessage.OP_SEND_PRIVATE_FAIL:
			{
				System.out.println("It wasn't possible to send the private message. Maybe the user is not in the room");
				break;
			}
			case NCMessage.OP_USER_IN:
			{
				NCRoomMessage roomMensaje = (NCRoomMessage) mensaje;
				System.out.println("The user " + roomMensaje.getName() + " has entered the room");
				break;
			}
			case NCMessage.OP_USER_OUT:
			{
				NCRoomMessage roomMensaje = (NCRoomMessage) mensaje;
				System.out.println("The user " + roomMensaje.getName() + " has left the room");
				break;
			}
			}
			
		} catch (IOException e) {
			System.out.println("* There was an error receiving a message");
		}
		
	}

	//MNétodo para leer un comando de la sala 
	public void readRoomCommandFromShell() {
		//Pedimos un nuevo comando de sala al shell (pasando el conector por si nos llega un mensaje entrante)
		shell.readChatCommand(ncConnector);
		//Establecemos el comando tecleado (o el mensaje recibido) como comando actual
		setCurrentCommand(shell.getCommand());
		//Procesamos los posibles parámetros (si los hubiera)
		setCurrentCommandArguments(shell.getCommandArguments());
	}

	//Método para leer un comando general (fuera de una sala)
	public void readGeneralCommandFromShell() {
		//Pedimos el comando al shell
		shell.readGeneralCommand();
		//Establecemos que el comando actual es el que ha obtenido el shell
		setCurrentCommand(shell.getCommand());
		//Analizamos los posibles parámetros asociados al comando
		setCurrentCommandArguments(shell.getCommandArguments());
	}

	//Método para obtener el servidor de NanoChat que nos proporcione el directorio
	public boolean getServerFromDirectory(String directoryHostname) {
		//Inicializamos el conector con el directorio y el shell
		System.out.println("* Connecting to the directory...");
		//Intentamos obtener la dirección del servidor de NanoChat que trabaja con nuestro protocolo
		try {
			directoryConnector = new DirectoryConnector(directoryHostname);
			serverAddress = directoryConnector.getServerForProtocol(PROTOCOL);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			serverAddress = null;
		}
		if (serverAddress == null) {
			System.out.println("* Check your connection, the directory is not available.");		
			return false;
		}
		else return true;
	}
	
	//Método para establecer la conexión con el servidor de Chat (a través del NCConnector)
	public boolean connectToChatServer() {
			try {
				//Inicializamos el conector para intercambiar mensajes con el servidor de NanoChat (lo hace la clase NCConnector)
				ncConnector = new NCConnector(serverAddress);
			} catch (IOException e) {
				System.out.println("* Check your connection, the game server is not available.");
				serverAddress = null;
			}
			//Si la conexión se ha establecido con éxito informamos al usuario y cambiamos el estado del autómata
			if (serverAddress != null) {
				System.out.println("* Connected to "+serverAddress);
				clientStatus = PRE_REGISTRATION;
				return true;
			}
			else return false;
	}

	//Método que comprueba si el usuario ha introducido el comando para salir de la aplicación
	public boolean shouldQuit() {
		return currentCommand == NCCommands.COM_QUIT;
	}

}
