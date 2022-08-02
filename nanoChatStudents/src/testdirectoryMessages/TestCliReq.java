package testdirectoryMessages;

import java.io.IOException;
import java.net.InetSocketAddress;

import es.um.redes.nanoChat.directory.connector.DirectoryConnector;

public class TestCliReq {


		// IDentificador de protocolo por defecto
		private static final int DEFAULT_PROTOCOL = 0;

		// IP del servidor de directorio
		private static final String DIRECTORY_SERVER_IP = new String("127.0.01");

		public static void main(String[] args) throws IOException {
			int protocol = DEFAULT_PROTOCOL;

			System.out.println("Asking for the server for protocol " + protocol + "...");
			
			DirectoryConnector dc = new DirectoryConnector(DIRECTORY_SERVER_IP);
			InetSocketAddress server_for_protocol = dc.getServerForProtocol(protocol);
			if (server_for_protocol == null) {
				System.out.println("There is no server for the protocol wanted");
			} else {
				System.out.println("The server address for protocol " + protocol + " is " + server_for_protocol);
			}
			System.exit(0);
		}
}
