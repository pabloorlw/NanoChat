package es.um.redes.nanoChat.messageML;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

public class NCListMessage extends NCMessage{

	//Constantes asociadas a las marcas específicas de este tipo de mensaje
	private static final String RE_LIST = "<list>(.*?)</list>";
	private static final String LIST_MARK = "list";
	private static final String RE_ENTRY = "<room>(.*?)</room>";
	private static final String ENTRY_MARK = "room";
	private static final String RE_NAME = "<name>(.*?)</name>";
	private static final String NAME_MARK = "name";
	private static final String RE_ITEMS = "<items>(.*?)</items>";
	private static final String ITEMS_MARK = "items";
	private static final String RE_TIME = "<time>(.*?)</time>";
	private static final String TIME_MARK = "time";
	private static final String RE_ITEM = "<item>(.*?)</item>";
	private static final String ITEM_MARK = "item";
		
	private List<NCRoomDescription> list;
	

	public NCListMessage(byte opcode, List<NCRoomDescription> listOfDescriptions) {
		this.opcode = opcode;
		this.list = listOfDescriptions;
	}
	
	public static NCListMessage readFromString(byte code, String message) {
		 List<NCRoomDescription> list_of_rooms = null;
	        //Extraemos el contenido de la marca "entrylist", si lo hubiere
	        Pattern pat_entrylist = Pattern.compile(RE_LIST, Pattern.DOTALL);
	        Matcher mat_entrylist = pat_entrylist.matcher(message);
	        if (mat_entrylist.find()) {
	            String entries = mat_entrylist.group(1);
	            if (entries != null && !entries.isEmpty()) {
	                Pattern pat_entry = Pattern.compile(RE_ENTRY, Pattern.DOTALL);
	                Pattern pat_name = Pattern.compile(RE_NAME);
	                Pattern pat_time = Pattern.compile(RE_TIME);
	                Pattern pat_items = Pattern.compile(RE_ITEMS, Pattern.DOTALL);
	                Pattern pat_item = Pattern.compile(RE_ITEM);
	                Matcher mat_entry = pat_entry.matcher(entries);
	                boolean entryFound = true;
	                while (entryFound) {
	                    entryFound = mat_entry.find();
	                    if (entryFound) {
	                        String entry = mat_entry.group(1);
	                        Matcher mat_name = pat_name.matcher(entry);
	                        Matcher mat_time = pat_time.matcher(entry);
	                        if (mat_name.find() && mat_time.find()) {
	                            String roomName = new String(mat_name.group(1));
	                            long timeLastMessage = Long.parseLong(mat_time.group(1));
	                            List<String> members = new ArrayList<String>();
	                            Matcher mat_items = pat_items.matcher(entry);
	                            if (mat_items.find()) {
	                                String items = mat_items.group(1);
	                                if (items != null && !items.isEmpty()) {
	                                    Matcher mat_item = pat_item.matcher(items);
	                                    boolean itemFound = true;
	                                    while (itemFound) {
	                                        itemFound = mat_item.find();
	                                        if (itemFound) {
	                                            String member = new String(mat_item.group(1));
	                                            members.add(member);
	                                        }
	                                    }
	                                }
	                            }
	                            if (list_of_rooms == null)
	                                list_of_rooms = new ArrayList<NCRoomDescription>();
	                            list_of_rooms.add(new NCRoomDescription(
	                                    roomName, members, timeLastMessage));
	                        }
	                    }
	                }
	            }
	        }
	        return new NCListMessage(code, list_of_rooms);
	}

	@Override
	public String toEncodedString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("<"+MESSAGE_MARK+">"+END_LINE);
		sb.append("<"+OPERATION_MARK+">"+opcodeToString(opcode)+"</"+OPERATION_MARK+">"+END_LINE); //Construimos el campo
		sb.append("<"+LIST_MARK+">");
		for (NCRoomDescription descripcion: this.list) {
			sb.append("<"+ENTRY_MARK+">");
			sb.append("<"+NAME_MARK+">");
			sb.append(descripcion.roomName);
			sb.append("</"+NAME_MARK+">");
			sb.append("<"+TIME_MARK+">");
			sb.append(descripcion.timeLastMessage);
			sb.append("</"+TIME_MARK+">");
			sb.append("<"+ITEMS_MARK+">");
			for (String item: descripcion.members) {
				sb.append("<"+ITEM_MARK+">");
				sb.append(item);
				sb.append("</"+ITEM_MARK+">");
			}
			sb.append("</"+ITEMS_MARK+">");
			sb.append("</"+ENTRY_MARK+">");
		}
		sb.append("</"+LIST_MARK+">"+END_LINE);
		sb.append("</"+MESSAGE_MARK+">"+END_LINE);

		return sb.toString(); //Se obtiene el mensaje
	}
	
	
	public List<NCRoomDescription> getList() {
		return Collections.unmodifiableList(this.list);
	}

}
