package es.um.redes.nanoChat.server;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;
import es.um.redes.nanoChat.server.roomManager.NCRoomManager;

/**
 * Esta clase contiene el estado general del servidor (sin la lógica relacionada con cada sala particular)
 */
class NCServerManager {

	//Primera habitación del servidor
	final static int INITIAL_ROOM = 1;
	final static String ROOM_PREFIX = "Room";
	//Siguiente habitación que se creará
	int nextRoom;
	//Usuarios registrados en el servidor
	private Set<String> users = new HashSet<String>();
	//Habitaciones actuales asociadas a sus correspondientes RoomManagers
	private Map<String,NCRoomManager> rooms = new HashMap<String,NCRoomManager>();

	NCServerManager() {
		nextRoom = INITIAL_ROOM;
	}

	//Método para registrar un RoomManager
	public void registerRoomManager(NCRoomManager rm) {
		//TODO Dar soporte para que pueda haber más de una sala en el servidor
		String roomName = ROOM_PREFIX + nextRoom;
		//Se hace esto para no crear una sala RoomX que podria ya existir.
		while (this.rooms.containsKey(roomName)) {
			nextRoom++;
			roomName = ROOM_PREFIX + nextRoom;			
		}
		rooms.put(roomName, rm);
		rm.setRoomName(roomName);
		nextRoom++;
		
	}
	
	public boolean renameRoom(String nombreAntiguo, String nombreNuevo) {
		NCRoomManager manager  = this.rooms.get(nombreAntiguo);
		//Obtenemos el manager del nuevo nombre para confirmar que es null, que no hay nignuna sala con ese nombre.
		NCRoomManager nuevo = this.rooms.get(nombreNuevo);
		if (manager != null && nuevo == null){
			this.rooms.remove(nombreAntiguo);
			this.rooms.put(nombreNuevo, manager);
			manager.setRoomName(nombreNuevo);
			return true;
		}
		return false;
	}

	//Devuelve la descripción de las salas existentes
	public synchronized List<NCRoomDescription> getRoomList() {
		//TODO Pregunta a cada RoomManager cuál es la descripción actual de su sala
		//TODO Añade la información al ArrayList
		List<NCRoomDescription> descripciones = new ArrayList<>();
		for (String sala: rooms.keySet()) {
			descripciones.add(rooms.get(sala).getDescription());
		}
		
		return descripciones;
	}


	//Intenta registrar al usuario en el servidor.
	public synchronized boolean addUser(String user) {
		boolean resp = false;
		//TODO (hecho) Devuelve true si no hay otro usuario con su nombre
		if (!users.contains(user)) {
			users.add(user);
			resp = true;
			System.out.println("Users updated, values: " + users);
		}
		//TODO (hecho) Devuelve false si ya hay un usuario con su nombre
		return resp;
	}

	//Elimina al usuario del servidor
	public synchronized void removeUser(String user) {
		//TODO (hecho) Elimina al usuario del servidor
		users.remove(user);
		System.out.println("Users updated, values: " + users);
	}

	//Un usuario solicita acceso para entrar a una sala y registrar su conexión en ella
	public synchronized NCRoomManager enterRoom(String u, String room, Socket s) {
		//TODO Verificamos si la sala existe
		NCRoomManager sala = rooms.get(room);
		
		//TODO Si la sala existe y si es aceptado en la sala entonces devolvemos el RoomManager de la sala
		if (sala != null) {
			boolean acepted = sala.registerUser(u, s);
			if (acepted) {
				return sala;
			}
		} 
		return null;
	}

	//Un usuario deja la sala en la que estaba
	public synchronized void leaveRoom(String u, String room) {
		//TODO Verificamos si la sala existe
		NCRoomManager sala = rooms.get(room);
		if (sala != null) {
			//TODO Si la sala existe sacamos al usuario de la sala
			//TODO Decidir qué hacer si la sala se queda vacía. DECIDIMOS NO HACER NADA
			sala.removeUser(u);
		}
		
	}
}
