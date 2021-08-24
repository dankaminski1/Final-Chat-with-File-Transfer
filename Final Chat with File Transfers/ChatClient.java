//Daniel Kaminski
//Final Exercise in Lieu of Exam 
//9/30/2020


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;


public class ChatClient {
	private static String fileName;
	
	public static void main(String args[]) {
		if (args.length != 4) {
			System.err.println("Wrong number of arguments");
			return;
		}
		
		int port = -1;
		int filePort = -1;
		String serverAddress = "localhost";
		
		filePort = Integer.valueOf(args[1]);
		port = Integer.valueOf(args[3]);
		if (args.length == 6) serverAddress = args[5];
		setupClientClient(port, filePort, serverAddress);
		
		System.exit(0);
	}
	
	private static void setupClientClient(int port, int filePort, String serverAddress) {
		try {
			Socket clientSocket = new Socket(serverAddress, port);
			DataInputStream input = new DataInputStream(clientSocket.getInputStream());
			DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String userName = "";
			
			System.out.println("What is your name?");
			userName = reader.readLine();
			
			//give file transfer port to server
			output.writeUTF(filePort + "@" + userName);
			
			//setup threads for listening
			Listener listener = new Listener(input);
			Thread listenerThread = new Thread(listener);
			
			FTPReceive fTPListener = new FTPReceive(filePort);
			Thread fTPListenerThread = new Thread(fTPListener);
			
			listenerThread.start();
			fTPListenerThread.start();
			
			menuLoop(output, port);
			
			clientSocket.shutdownOutput();
			clientSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static void menuLoop(DataOutputStream output, int filePort) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String message;
			
			System.out.println("Enter an option ('m', 'f', 'x'):\n (M)essage (send)\n (F)ile (request)\ne(X)it");
			
			while((message = reader.readLine()) != null) {
				if (message.toLowerCase().equals("m")) {
					System.out.println("Enter your message:");
					message = reader.readLine();
					output.writeUTF(message);
				} else if (message.toLowerCase().equals("f")) {
					//Request a file
					System.out.println("Who owns the file?");
					String owner = reader.readLine();
					System.out.println("Which file do you want?");
					fileName = reader.readLine();
					output.writeUTF("#!" + owner);
					
				} else if (message.toLowerCase().equals("x")) {
					output.writeUTF(null);
					break;
				}
				System.out.println("Enter an option ('m', 'f', 'x'):\n (M)essage (send)\n (F)ile (request)\ne(X)it");
			}
			//System.out.println("closing your sockets...goodbye");
		} catch (Exception e) {
			//System.err.println(e.getMessage());
		}
		//System.out.println("closing your sockets...goodbye");
	}
	
	
	//---------------------End of methods; Start of helper classes------------------------
	
	
	private static class Listener implements Runnable {

		DataInputStream input;
		Listener(DataInputStream input) {
			this.input = input;
		}
		
		@Override
		public void run() {
			String message = "";
			try {
				while ((message = input.readUTF()) != null) {
					if (message.startsWith("#!") && message != "") {
						int targetFilePort = Integer.valueOf(message.substring(2));
						FTPGetFile fileGetter = new FTPGetFile(fileName, targetFilePort);
						Thread fileGetterThread = new Thread(fileGetter);
						fileGetterThread.start();
					} else {
						System.out.println(message);
					}
				}
			} catch (IOException e) {
				System.out.println("closing your sockets...goodbye");
				System.exit(0);
			}
		}
	}
	
	
	private static class FTPReceive implements Runnable {
		private ServerSocket serverSocket;
		private int filePort;
		
		FTPReceive(int filePort) {
			this.filePort = filePort;
		}
		
		@Override
		public void run() {
			try {
				serverSocket = new ServerSocket(filePort);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			while (true) {
				try {
					Socket clientSocket = serverSocket.accept();
					DataInputStream input = new DataInputStream(clientSocket.getInputStream());
					DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());		
					
					String fileName= input.readUTF();
					
					File file= new File(fileName);
					FileInputStream fileInput= new FileInputStream(file);
					byte[] fileBuffer= new byte[1500];
					int numberRead;
					while((numberRead= fileInput.read(fileBuffer)) != -1)
						output.write(fileBuffer, 0, numberRead);
					
					fileInput.close();	
					clientSocket.close();
				} catch (IOException e) {
					System.exit(0);
				}
			}
			
		}
	}
	
	
	private static class FTPGetFile implements Runnable {
		
		private String fileName;
		private int filePort;
		
		FTPGetFile(String fileName, int filePort) {
			this.fileName = fileName;
			this.filePort = filePort;
		}
		
		@Override
		public void run() {
			try {
				Socket clientSocket = new Socket("localhost", filePort);
			
				DataInputStream input = new DataInputStream(clientSocket.getInputStream());
				DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
				
				output.writeUTF( fileName );
				
				FileOutputStream fileOut= new FileOutputStream(fileName);	
				int numberRead;
				byte[] buffer= new byte[1500];
				while((numberRead= input.read(buffer)) != -1)
					fileOut.write(buffer, 0, numberRead);
				
				fileOut.close();
				clientSocket.close();
			} catch (Exception e) {
				System.exit(0);
			}
		}
		
	}
}