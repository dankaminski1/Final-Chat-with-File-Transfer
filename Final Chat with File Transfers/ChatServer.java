//Daniel Kaminski
//Final Exercise in Lieu of Exam 
//9/30/2020


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ChatServer{
	
	public static void main(String args[]) {
		if (args.length != 1) {
			System.err.println("Wrong number of arguments");
			return;
		}
		
		int port = -1;
		
		port = Integer.valueOf(args[0]);
		setupServerClient(port);
		
		System.exit(0);
	}
	
	private static void setupServerClient(int port) {
		
			//setup port for all to connect to
			ServerSocket serverSocket = null;
			try {
				serverSocket = new ServerSocket(port);
			} catch (IOException e) {
				e.printStackTrace();
			}
			//list of clients connected plus their info
			ArrayList<ClientInfo> clientList = new ArrayList<ClientInfo>();
			
			//start
			while(true) {
				//clientSocket
				ClientInfo newClientInfo = new ClientInfo();
				try {
				newClientInfo.socket = serverSocket.accept();
				newClientInfo.initializeDataStreams();

				clientList.add(newClientInfo);
				
				//setup threads for listening
				Listener listener = new Listener(newClientInfo, clientList);
				Thread listenerThread = new Thread(listener);
				
				listenerThread.start();
				} catch (IOException e) {
					break;
				}
			}
			
			try {
				serverSocket.close();
			} catch (IOException e) {}
			
			for (ClientInfo client : clientList) {
				client.shutdown();
			}
		//exit back into main for sys exit
	}
	
	
	//---------------------End of methods; Start of helper classes------------------------
	
	
	private static class Listener implements Runnable {
		
		ClientInfo client;
		ArrayList<ClientInfo> clientList;
		Listener(ClientInfo client, ArrayList<ClientInfo> clientList) {
			this.client = client;
			this.clientList = clientList;
		}
		
		@Override
		public void run() {
			String message = "";
			try {
				while ((message = client.input.readUTF()) != null) {
					if (message.startsWith("#!") && message != "") {
						String targetClient = message.substring(2);
						for (ClientInfo ci : clientList) {
							if (ci.name.contentEquals(targetClient)) {
								client.ouput.writeUTF("#!" + ci.filePort);
								break;
							}
						}
					}else {
						outToRest(message);
					}
				}
			} catch (IOException e) {
				System.out.println("closing your sockets...goodbye");
			}
			client.shutdown();
			clientList.remove(client);
		}
		
		private void outToRest(String message) {
			for (ClientInfo ci: clientList) {
				if (ci != client) {
					try {
						ci.ouput.writeUTF(client.name + ": " + message);
					} catch (IOException e) {
						System.err.println(e.getMessage());
					}
				}
			}
		}
	}
	
	private static class ClientInfo {
		String name;
		Socket socket;
		DataInputStream input;
		DataOutputStream ouput;
		int filePort;
		
		ClientInfo(){}
		
		void initializeDataStreams() {
			try {
				//fromClientInputStream
				this.input = new DataInputStream(this.socket.getInputStream());
				//toClientOutputStream
				this.ouput = new DataOutputStream(this.socket.getOutputStream());
				
				//receive file transfer port and name from client
				String message = input.readUTF();
				int separatorIndex = message.indexOf('@');
				filePort = Integer.valueOf(message.substring(0, separatorIndex));
				name = message.substring(separatorIndex + 1);
				
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		}
		
		boolean shutdown() {
			try {
				socket.shutdownOutput();
				socket.close();
				return true;
			} catch (IOException e) {
				return false;
			}
		}
		
	}
}