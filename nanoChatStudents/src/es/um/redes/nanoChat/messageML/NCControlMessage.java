package es.um.redes.nanoChat.messageML;

/*
<message>
<operation>op_code</operation>
</message>
 */

public class NCControlMessage extends NCMessage{

	public NCControlMessage (byte op_code) {
		this.opcode = op_code;
	}
	
	@Override
	public String toEncodedString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("<"+MESSAGE_MARK+">"+END_LINE);
		sb.append("<"+OPERATION_MARK+">"+opcodeToString(opcode)+"</"+OPERATION_MARK+">"+END_LINE);
		sb.append("</"+MESSAGE_MARK+">"+END_LINE);

		return sb.toString();
	}

	public static NCControlMessage readFromString(byte code) {
		return new NCControlMessage(code);
	}
}
