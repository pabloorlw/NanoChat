package es.um.redes.nanoChat.server.roomManager;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.um.redes.nanoChat.messageML.NCControlMessage;
import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.messageML.NCRoomMessage;
import es.um.redes.nanoChat.messageML.NCThreeParameterMessage;

public class NCRoom extends NCRoomManager{

	private Map<String, Socket> users;
	private long fecha;
	
	public NCRoom() {
		users = new HashMap<>();
	}
	
	@Override
	public boolean registerUser(String u, Socket s) {
		if (users.containsKey(u)) {
			return false;
		}
		users.put(u, s);
		return true;		//Siempre se podrá registrar, no puede haber 2 ususarios con el mismo nombre pues eso ya está controlado en el server.
	}

	@Override
	public void broadcastMessage(String u, String message) throws IOException {
		NCThreeParameterMessage mensaje = (NCThreeParameterMessage) NCMessage.makeThreeParameterMessage(NCMessage.OP_SEND_IN, u, message);
		String rawMessage = mensaje.toEncodedString();
		for (String user: users.keySet()) {
			if (!user.equals(u)) {
				DataOutputStream dis = new DataOutputStream (users.get(user).getOutputStream());
				dis.writeUTF(rawMessage);
			}
		}
		this.fecha = new Date().getTime();
		
	}

	@Override
	public void removeUser(String u) {
		users.remove(u);
		
	}

	@Override
	public void setRoomName(String roomName) {
		this.roomName = roomName;
		
		
	}

	@Override
	public NCRoomDescription getDescription() {
		List<String> usuarios = new ArrayList<>();
		for (String u: users.keySet()) {
			usuarios.add(u);
		}
		return new NCRoomDescription (roomName, usuarios, fecha);
	}

	@Override
	public int usersInRoom() {
		return users.size();
	}
	
	
	@Override
	//Si el codigo es 1 es entrada y si es 2 es salida
	public void broadcastIONotification(String u, int codigo) throws IOException{
		NCRoomMessage mensaje;
		if (codigo == 1) {
			mensaje = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_USER_IN, u);
		} else {
			mensaje = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_USER_OUT, u);
		}
		String rawMessage = mensaje.toEncodedString();
		for (String user: users.keySet()) {
			if (!user.equals(u)) {
				DataOutputStream dis = new DataOutputStream (users.get(user).getOutputStream());
				dis.writeUTF(rawMessage);
			}
		}
	}

	@Override
	public void sendMessage(String sender, String u, String message) throws IOException {
		NCThreeParameterMessage mensaje = (NCThreeParameterMessage) NCMessage.makeThreeParameterMessage(NCMessage.OP_SEND_IN_PRIVATE, sender, message);
		String rawMessage = mensaje.toEncodedString();
		Socket user = this.users.get(u);
		if (user != null) {
			DataOutputStream dis = new DataOutputStream (user.getOutputStream());
			dis.writeUTF(rawMessage);
		} else {
			NCControlMessage error = (NCControlMessage) NCMessage.makeControlMessage(NCMessage.OP_SEND_PRIVATE_FAIL);
			String errorRawMessage = error.toEncodedString();
			DataOutputStream dis = new DataOutputStream (this.users.get(sender).getOutputStream());
			dis.writeUTF(errorRawMessage);
		}
			
	}

}
