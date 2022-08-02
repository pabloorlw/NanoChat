package es.um.redes.nanoChat.messageML;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

public abstract class NCMessage {
	protected byte opcode;

	// TODO IMPLEMENTAR TODAS LAS CONSTANTES RELACIONADAS CON LOS CODIGOS DE
	// OPERACION
	public static final byte OP_INVALID_CODE = 0;
	public static final byte OP_NICK = 1;
	public static final byte OP_NICK_OK = 2;
	public static final byte OP_DUPLICATED_NICK = 3;
	public static final byte OP_ROOM_LIST_QUERY = 4;
	public static final byte OP_ROOM_LIST = 5;
	public static final byte OP_ENTER_ROOM = 6;
	public static final byte OP_ENTER_ROOM_OK = 7;
	public static final byte OP_ENTER_ROOM_FAIL = 8;
	public static final byte OP_EXIT = 9;
	public static final byte OP_SEND = 10;
	public static final byte OP_SEND_IN = 11;
	public static final byte OP_INFO_QUERY = 12;
	public static final byte OP_INFO = 13;
	public static final byte OP_USER_IN = 14;
	public static final byte OP_USER_OUT = 15;
	public static final byte OP_ROOM_RENAME = 16;
	public static final byte OP_ROOM_RENAME_OK = 17;
	public static final byte OP_ROOM_RENAME_FAIL = 18;
	public static final byte OP_CREATE_ROOM = 19;
	public static final byte OP_SEND_PRIVATE = 20;
	public static final byte OP_SEND_IN_PRIVATE = 21;
	public static final byte OP_SEND_PRIVATE_FAIL = 22;

	public static final char DELIMITER = ':'; // Define el delimitador
	public static final char END_LINE = '\n'; // Define el carácter de fin de línea

	public static final String OPERATION_MARK = "operation";
	public static final String MESSAGE_MARK = "message";

	/**
	 * Códigos de los opcodes válidos El orden es importante para relacionarlos con
	 * la cadena que aparece en los mensajes
	 */
	private static final Byte[] _valid_opcodes = { OP_NICK, OP_NICK_OK, OP_DUPLICATED_NICK, OP_ROOM_LIST_QUERY,
			OP_ROOM_LIST, OP_ENTER_ROOM, OP_ENTER_ROOM_OK, OP_ENTER_ROOM_FAIL, OP_EXIT, OP_SEND, OP_SEND_IN,
			OP_INFO_QUERY, OP_INFO, OP_USER_IN, OP_USER_OUT, OP_ROOM_RENAME, OP_ROOM_RENAME_OK, OP_ROOM_RENAME_FAIL,
			OP_CREATE_ROOM, OP_SEND_PRIVATE, OP_SEND_IN_PRIVATE, OP_SEND_PRIVATE_FAIL };

	/**
	 * cadena exacta de cada orden
	 */
	private static final String[] _valid_operations_str = { "Nick", "Nick_OK", "Nick_Duplicated", "Room_List_Query",
			"Room_List", "Enter_Room", "Enter_Room_OK", "Enter_Room_Fail", "Exit", "Send", "Send_In", "Info_Query",
			"Info", "User_in", "User_out", "Room_Rename", "Room_Rename_Ok", "Room_Rename_Fail", "Create_Room",
			"Send_Private", "Send_In_Private", "Send_Private_Fail" };

	private static Map<String, Byte> _operation_to_opcode;
	private static Map<Byte, String> _opcode_to_operation;

	static {
		_operation_to_opcode = new TreeMap<>();
		_opcode_to_operation = new TreeMap<>();
		for (int i = 0; i < _valid_operations_str.length; ++i) {
			_operation_to_opcode.put(_valid_operations_str[i].toLowerCase(), _valid_opcodes[i]);
			_opcode_to_operation.put(_valid_opcodes[i], _valid_operations_str[i]);
		}
	}

	/**
	 * Transforma una cadena en el opcode correspondiente
	 */
	protected static byte stringToOpcode(String opStr) {
		return _operation_to_opcode.getOrDefault(opStr.toLowerCase(), OP_INVALID_CODE);
	}

	/**
	 * Transforma un opcode en la cadena correspondiente
	 */
	protected static String opcodeToString(byte opcode) {
		return _opcode_to_operation.getOrDefault(opcode, null);
	}

	// Devuelve el opcode del mensaje
	public byte getOpcode() {
		return opcode;
	}

	// Método que debe ser implementado por cada subclase de NCMessage
	protected abstract String toEncodedString();

	// Analiza la operación de cada mensaje y usa el método readFromString() de cada
	// subclase para parsear
	public static NCMessage readMessageFromSocket(DataInputStream dis) throws IOException {
		String message = dis.readUTF();
		String regexpr = "<" + MESSAGE_MARK + ">(.*?)</" + MESSAGE_MARK + ">";
		Pattern pat = Pattern.compile(regexpr, Pattern.DOTALL);
		Matcher mat = pat.matcher(message);
		if (!mat.find()) {
			System.out.println("Mensaje mal formado:\n" + message);
			return null;
			// Message not found
		}
		String inner_msg = mat.group(1); // extraemos el mensaje

		String regexpr1 = "<" + OPERATION_MARK + ">(.*?)</" + OPERATION_MARK + ">";
		Pattern pat1 = Pattern.compile(regexpr1);
		Matcher mat1 = pat1.matcher(inner_msg);
		if (!mat1.find()) {
			System.out.println("Mensaje mal formado:\n" + message);
			return null;
			// Operation not found
		}
		String operation = mat1.group(1); // extraemos la operación

		byte code = stringToOpcode(operation);
		if (code == OP_INVALID_CODE)
			return null;

		switch (code) {
		// TODO Parsear el resto de mensajes
		case OP_NICK: {
			return NCRoomMessage.readFromString(code, message);
		}
		case OP_NICK_OK: {
			return NCControlMessage.readFromString(code);
		}
		case OP_DUPLICATED_NICK: {
			return NCControlMessage.readFromString(code);
		}
		case OP_ROOM_LIST_QUERY: {
			return NCControlMessage.readFromString(code);
		}
		case OP_ROOM_LIST: {
			return NCListMessage.readFromString(code, message);
		}
		case OP_ENTER_ROOM: {
			return NCRoomMessage.readFromString(code, message);
		}
		case OP_ENTER_ROOM_OK: {
			return NCControlMessage.readFromString(code);
		}
		case OP_ENTER_ROOM_FAIL: {
			return NCControlMessage.readFromString(code);
		}
		case OP_EXIT: {
			return NCControlMessage.readFromString(code);
		}
		case OP_SEND: {
			return NCRoomMessage.readFromString(code, message);
		}
		case OP_SEND_IN: {
			return NCThreeParameterMessage.readFromString(code, message);
		}
		case OP_INFO_QUERY: {
			return NCControlMessage.readFromString(code);
		}
		case OP_INFO: {
			return NCListMessage.readFromString(code, message);
		}
		case OP_USER_IN: {
			return NCRoomMessage.readFromString(code, message);
		}
		case OP_USER_OUT: {
			return NCRoomMessage.readFromString(code, message);
		}
		case OP_ROOM_RENAME: {
			return NCRoomMessage.readFromString(code, message);
		}
		case OP_ROOM_RENAME_OK: {
			return NCControlMessage.readFromString(code);
		}
		case OP_ROOM_RENAME_FAIL: {
			return NCControlMessage.readFromString(code);
		}
		case OP_CREATE_ROOM: {
			return NCControlMessage.readFromString(code);
		}
		case OP_SEND_PRIVATE: {
			return NCThreeParameterMessage.readFromString(code, message);
		}
		case OP_SEND_IN_PRIVATE: {
			return NCThreeParameterMessage.readFromString(code, message);
		}
		case OP_SEND_PRIVATE_FAIL: {
			return NCControlMessage.readFromString(code);
		}
		default:
			System.err.println("Unknown message type received:" + code);
			return null;
		}

	}

	// TODO Programar el resto de métodos para crear otros tipos de mensajes

	public static NCMessage makeRoomMessage(byte code, String room) {
		return new NCRoomMessage(code, room);
	}

	public static NCMessage makeControlMessage(byte code) {
		return new NCControlMessage(code);
	}

	public static NCMessage makeListMessage(byte code, List<NCRoomDescription> lista) {
		return new NCListMessage(code, lista);
	}

	public static NCMessage makeThreeParameterMessage(byte code, String name, String text) {
		return new NCThreeParameterMessage(code, name, text);
	}
}
