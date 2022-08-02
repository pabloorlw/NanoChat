package testdirectoryMessages;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import es.um.redes.nanoChat.directory.connector.DirectoryConnector;

public class TestSrvReg {

	// Rango admisible para los identificadores de protocolo.
	private static final int MIN_PROTOCOL = 0;
	private static final int MAX_PROTOCOL = 255;

	// IDentificador de protocolo por defecto
	private static final int DEFAULT_PROTOCOL = 0;

	// Numero de puerto minimo admisible para el servidor de chat
	private static final int MIN_PORT = 1025;

	// Puerto por defecto en el que atiende este servidor de char
	private static final int DEFAULT_PORT = 6969;

	// IP del servidor de directorio
	private static final String DIRECTORY_SERVER_IP = new String("127.0.01");

	// Metodo "main". Posibles argumetnos: -protocol <protocol> -port <port>
	public static void main(String[] args) throws IOException{
		int protocol = DEFAULT_PROTOCOL;
		int port = DEFAULT_PORT;
		if (args.length > 0) {
			int valuesFromArgs[] = getArgs(args);
			if (valuesFromArgs[0] == -1 && valuesFromArgs[1] == -1) {
				System.err.println("Illegal Syntax. Valid args: -protocol <protocol> -port <port>");
			} else {
				if (valuesFromArgs[0] >= 0) {
					protocol = valuesFromArgs[0];
				}
				if (valuesFromArgs[1] >= 0) {
					port = valuesFromArgs[1];
				}
			}
		}
		if (!protocolOk(protocol)) {
			System.err.println("Illegal protocol value. Valid range: [" + Integer.toString(MIN_PROTOCOL) + ","
					+ Integer.toString(MAX_PROTOCOL) + "]");
			System.exit(1);
		}
		if (!portOk(port)) {
			System.err.println("Illegal port value. Valid value: greater or equal than " + Integer.toString(MIN_PORT));
			System.exit(1);
		}
		System.out.println("Directory Server, ip = " + DIRECTORY_SERVER_IP);
		System.out.println("Chat server, ip = " + getLocalIP());
		System.out.println("Chat Server, protocol = " + Integer.toString(protocol));
		System.out.println("Chat server, port = " + Integer.toString(port));
		System.out.println("Starting register of this chat server...");
		DirectoryConnector dc = new DirectoryConnector(DIRECTORY_SERVER_IP);
		boolean registerOk = dc.registerServerForProtocol(protocol, port);
		String infoFinal = new String("OK");
		if (!registerOk) {
			infoFinal = new String("ERROR");
		}
		System.out.println("Register operation completed. Result: " + infoFinal);
		System.exit(0);
	}

	// Metodo para obtener, a partir de los argumetnos pasados al programa, el
	// protocolo y el puerto de escucha del servidor de chat.
	private static int[] getArgs(String[] args) {
		int protocol = -1;
		int port = -1;
		if (args.length == 2 || args.length == 4) {
			String arg0 = args[0];
			String arg1 = args[1];
			String arg2 = new String("");
			String arg3 = new String("");
			if (args.length == 4) {
				arg2 = args[2];
				arg3 = args[3];
			}
			if (arg0.startsWith("-") && (arg2.equals("") || arg2.startsWith("-"))) {
				//Posible reordenación de argumentos.
				if (arg2.contentEquals("-protocol")) {
					String tmpArg0 = new String(arg0);
					String tmpArg1 = new String(arg1);
					arg0 = arg2;
					arg1 = arg3;
					arg2 = tmpArg0;
					arg3 = tmpArg1;
				}
				try {
					if (arg0.contentEquals("-protocol")) {
						protocol = Integer.parseInt(arg1);
					}
					else if (arg0.contentEquals("-port")) {
						port = Integer.parseInt(arg1);
					}
				}
				catch (NumberFormatException e) {
					System.err.println("Wrong value passed to option " + arg0);
					return new int[] {-1, -1};
				}
				//Si hay un segundo par de argumentos (-<clave>, <valor>), estos deberan estar referidos al puerto.
				if (!arg2.contentEquals("")) {
					try {
						if (arg2.contentEquals("-port")) {
							port = Integer.parseInt(arg3);
						}
					}
					catch (NumberFormatException e) {
						System.err.println("Wrong value passed to option " + arg2);
						return new int[] {-1, -1};
					}
				}
			}
		}
		return new int[] {protocol, port};
	}
	
	
	
	
	private static boolean protocolOk (int protocol) {
		boolean resp = true;
		if (protocol < MIN_PROTOCOL || protocol > MAX_PROTOCOL) {
			resp = false;
		}
		return resp;
	}
	
	
	private static boolean portOk(int port) {
		boolean resp = true;
		if (port < MIN_PORT) {
			resp = false;
		}
		return resp;
	}
	
	//Metodo para obtener la primera IP que no esté asociada al "loopback".
	private static String getLocalIP() throws SocketException{
		String resp = new String("");
		Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
		while (networkInterfaces.hasMoreElements()) {
			NetworkInterface networkInterface = networkInterfaces.nextElement();
			if (!networkInterface.isLoopback()) {
				Enumeration<InetAddress> localAddresses = networkInterface.getInetAddresses();
				//the IP can be version 4 or 6.
				while (localAddresses.hasMoreElements()) {
					InetAddress localAddress = localAddresses.nextElement();
					String ip = localAddress.toString().substring(1);
					int numPoints = ip.length() - ip.replace(".",  "").length();
					if (numPoints == 3) {
						resp = ip;
						break;
					}
				}
				break;
			}
		}
		return resp;
	}
	
}
