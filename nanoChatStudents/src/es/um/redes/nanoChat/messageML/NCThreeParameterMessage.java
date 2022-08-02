package es.um.redes.nanoChat.messageML;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NCThreeParameterMessage extends NCMessage{

	//Constantes asociadas a las marcas específicas de este tipo de mensaje
	private static final String RE_NAME = "<name>(.*?)</name>";
	private static final String NAME_MARK = "name";
	
	private static final String RE_TEXT = "<text>(.*?)</text>";
	private static final String TEXT_MARK = "text";
	
	private String name;
	private String text;
	
	public NCThreeParameterMessage(byte opcode, String name, String text) {
		this.opcode = opcode;
		this.name = name;
		this.text = text;
	}
	
	//TODO HACER, ESTA COPIADO DE NCRROMMESSAGE, TERMINAR
	public static NCThreeParameterMessage readFromString(byte code, String message) {
		String found_name = null;
		String found_text = null;

		Pattern pat_name = Pattern.compile(RE_NAME);
		Matcher mat_name = pat_name.matcher(message);
		if (mat_name.find()) {
			found_name = mat_name.group(1);
		} else {
			System.out.println("Error en RoomMessage: no se ha encontrado parametro.");
			return null;
		}
		Pattern pat_text = Pattern.compile(RE_TEXT);
		Matcher mat_text = pat_text.matcher(message);
		if (mat_text.find()) {
			found_text = mat_text.group(1);
		} else {
			System.out.println("Error en RoomMessage: no se ha encontrado parametro.");
			return null;
		}
		
		return new NCThreeParameterMessage(code, found_name, found_text);
	}
	
	
	@Override
	public String toEncodedString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("<"+MESSAGE_MARK+">"+END_LINE);
		sb.append("<"+OPERATION_MARK+">"+opcodeToString(opcode)+"</"+OPERATION_MARK+">"+END_LINE); //Construimos el campo
		sb.append("<"+NAME_MARK+">"+name+"</"+NAME_MARK+">"+END_LINE);
		sb.append("<"+TEXT_MARK+">"+text+"</"+TEXT_MARK+">"+END_LINE);
		sb.append("</"+MESSAGE_MARK+">"+END_LINE);

		return sb.toString(); //Se obtiene el mensaje
	}
	
	public String getName() {
		return name;
	}
	
	public String getText() {
		return text;
	}

}
