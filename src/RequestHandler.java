
//package Proxy;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;

// RequestHandler is thread that process requests of one client connection
public class RequestHandler extends Thread {

	ProxyServer proxyServer = null; // create a reference to the ProxyServer class
	Socket clientSocket = null; // create a socket from the client to the proxy

	InputStream inFromClient;
	OutputStream outToClient;
	BufferedReader clientToProxyBufferedReader;
	BufferedWriter clientToProxyBufferedWriter;

	Socket serverSocket = null; // create a socket from the proxy to the network server
	InputStream inFromServer;
	OutputStream outToServer;
	BufferedReader proxyToServerBufferedReader;
	BufferedWriter proxyToServerBufferedWriter;

	String fileName = "";

	InetAddress serverIpAddress = null;
	String clientIpAddress = null;
	String serverIp = null;

	String urlString = "";
	String requestType = "";
	int port = 80;

	// Pass in socket object and proxy server as a reference in order to get to the log and cache methods
	public RequestHandler(Socket clientSocket, ProxyServer proxyServer) {

		System.out.println("Creating a new RequestHandler object by accepting a connection to the proxy server");
		this.clientSocket = clientSocket;
		clientIpAddress = clientSocket.getInetAddress().toString();

		this.proxyServer = proxyServer;
		try {
			//clientSocket.setSoTimeout(5000);
			inFromClient = clientSocket.getInputStream();
			outToClient = clientSocket.getOutputStream();

			fileName = generateRandomFileName() + ".dat";

		} catch (Exception e) {
			System.out.println(
					"Error creating a new Request Handler and connection to the proxy server: " + e.toString());
			e.printStackTrace();
		}
	}

	@Override

	public void run() {

		try {
			// Data writing from the proxy to the client
			clientToProxyBufferedWriter = new BufferedWriter(new OutputStreamWriter(outToClient));

			// Data read from the client into the proxy
			clientToProxyBufferedReader = new BufferedReader(new InputStreamReader(inFromClient));

			// Reading the HTTP request from the client that is connected to the proxy
			// server
			String clientRequest = clientToProxyBufferedReader.readLine();
			System.out.println("Client Request String: " + clientRequest);

			if (clientRequest != null) {
				// Get the Request type "CONNECT", "GET", "POST" from the client request
				requestType = clientRequest.substring(0, clientRequest.indexOf(' '));

//				// Get the urlString from the client request for where the message is destine to go
				urlString = clientRequest.substring(clientRequest.indexOf(' ') + 1);
//
//				// Remove everything past next space
				urlString = urlString.substring(0, urlString.indexOf(' '));
//
//				// Prepend http:// if necessary to create correct URL
				if (!urlString.substring(0, 4).equals("http")) {
					String temp = "http://";
					urlString = temp + urlString;
				}

				// Split the current urlString into a basic url value, and host value
				String url = urlString.substring(7);
				String addressArray[] = url.split(":");
				String host = addressArray[0];

				// If the request is trying to establish an initial connection
				// create a tunnel between the client and the network server
				if (requestType.equals("CONNECT")) {

					port = Integer.valueOf(addressArray[1]);
					serverIpAddress = InetAddress.getByName(host);

					// Create a new connection to the network server
					ProxyToNetworkServerConnection(serverIpAddress, port, host);
				} else {
					System.out.println("Lookup in cache for host value: " + host);
					if (!sendCachedInfoToClient(host)) {
						// if there was not a cached file of the GET request, lookup the request using
						// HTTP connections
						sendUncachedRequestToServer(urlString, host);
					}
				}

				if (serverIpAddress == null) {
					serverIp = "";
				} else {
					serverIp = serverIpAddress.getHostAddress().toString();
				}

				/// Logs Ip of client + URL sent to proxy server
				proxyServer.writeLog(clientIpAddress, url, serverIp);

				/// Debug client request information
				System.out.println("Request Type: " + requestType);
				System.out.println("URL String: " + urlString);
				System.out.println("URL: " + url);
				System.out.println("Host: " + host);
				System.out.println("Port: " + port);
				System.out.println("Server Host IP Address " + serverIpAddress);
			}
		}

		catch (UnknownHostException e) {
			System.out.println(
					"Unknown Host Exception error connecting to host: " + urlString + " Exception: " + e.toString());
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("Error on HTTPS : " + urlString + " Exception: " + e.toString());
			e.printStackTrace();
		}
	}

	private void ProxyToNetworkServerConnection(InetAddress serverIpAddress, int port, String host) {
		try {
			serverSocket = new Socket(serverIpAddress, port);

			//serverSocket.setSoTimeout(5000);
			inFromServer = serverSocket.getInputStream();
			outToServer = serverSocket.getOutputStream();

			String line = "HTTP/1.0 200 Connection established\r\n" + "Proxy-Agent: ProxyServer/1.0\r\n" + "\r\n";
			clientToProxyBufferedWriter.write(line);
			clientToProxyBufferedWriter.flush();

			// Data writing to the network server
			proxyToServerBufferedWriter = new BufferedWriter(new OutputStreamWriter(outToServer));

			// Data read from the network server
			proxyToServerBufferedReader = new BufferedReader(new InputStreamReader(inFromServer));

			// Creating a separate runnable thread for the client to pass data to the
			// network server
			ClientConnectionToNetworkConnection clientToNetwork = new ClientConnectionToNetworkConnection(
					clientSocket.getInputStream(), serverSocket.getOutputStream());
			Thread clientToServerCommunication = new Thread(clientToNetwork);
			clientToServerCommunication.start();

			try {
				byte[] buffer = new byte[4096];
				int read = 0;
				do {
					read = serverSocket.getInputStream().read(buffer);
					if (read > 0) {
						clientSocket.getOutputStream().write(buffer, 0, read);
						if (serverSocket.getInputStream().available() < 1) {
							clientSocket.getOutputStream().flush();
						}
					}
				} while (read >= 0);

			} catch (SocketTimeoutException e) {

			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (Exception e) {

		}
	}

	// Sends the cached content stored in the cache file to the client
	private boolean sendCachedInfoToClient(String fileName) {

		try {

			File cachedFile = proxyServer.getCache(fileName);
			System.out.println("Cached Item from client: " + cachedFile); 
			if (cachedFile != null) {
				BufferedReader cachedFileBufferedReader = new BufferedReader(
						new InputStreamReader(new FileInputStream(cachedFile)));
				
				String cachedItem;
				while ((cachedItem = cachedFileBufferedReader.readLine()) != null) {
					System.out.println("Cached Item from client: " + cachedItem);
					clientToProxyBufferedWriter.write(cachedItem);
				}
				
				clientToProxyBufferedWriter.flush();
				cachedFileBufferedReader.close();
				clientToProxyBufferedWriter.close();

				return true;
			} else {
				return false;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	// Sends the cached content stored in the cache file to the client
	private void sendUncachedRequestToServer(String url, String host) {

		try {

			File cachedFile = null;
			BufferedWriter cacheFileWriter = null;

			cachedFile = new File("cached/" + fileName);

			if (!cachedFile.exists()) {
				cachedFile.createNewFile();
			}

			// Create Buffered output stream to write to cached copy of file
			cacheFileWriter = new BufferedWriter(new FileWriter(cachedFile));

			// Create a connection to remote server
			URL remoteURL = new URL(url);
			HttpURLConnection proxyToServerCon = (HttpURLConnection) remoteURL.openConnection();

			BufferedReader proxyToServerReader = new BufferedReader(
					new InputStreamReader(proxyToServerCon.getInputStream()));

			String line = "HTTPS/1.0 200 OK\n" + "Proxy-agent: ProxyServer/1.0\n" + "\r\n";
			clientToProxyBufferedWriter.write(line); 
			clientToProxyBufferedWriter.flush();
			
			if (proxyToServerReader != null) {
				// Read from input stream between proxy and remote server
				while ((line = proxyToServerReader.readLine()) != null) {
					// Send GET/POST data to client
					clientToProxyBufferedWriter.write(line);
					cacheFileWriter.write(line);
				}
				
				clientToProxyBufferedWriter.flush();
				cacheFileWriter.flush();


				// push data file to proxy server cache
				proxyServer.putCache(host, cachedFile);

				if (cacheFileWriter != null) {
					cacheFileWriter.close();
				}
				if (clientToProxyBufferedWriter != null) {
					clientToProxyBufferedWriter.close();
				}
			}
		} catch (IOException e) {
			System.out.println("Load uncached item exception: " + e.toString());
			e.printStackTrace();
		}
	}

	// Generates a random file name for a cahced file
	public String generateRandomFileName() {

		String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_";
		SecureRandom RANDOM = new SecureRandom();
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < 10; ++i) {
			sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return sb.toString();
	}

	public class ClientConnectionToNetworkConnection extends Thread {
		InputStream inFromClientToServer;
		OutputStream outFromServerToClient;

		public ClientConnectionToNetworkConnection(InputStream inFromClientToServer,
				OutputStream outFromServerToClient) {
			this.inFromClientToServer = inFromClientToServer;
			this.outFromServerToClient = outFromServerToClient;
		}

		@Override
		public void run() {
			try {
				// Read byte by byte from client and send directly to server
				byte[] buffer = new byte[4096];
				int read;
				do {
					read = inFromClientToServer.read(buffer);
					if (read > 0) {
						outFromServerToClient.write(buffer, 0, read);
						if (inFromClientToServer.available() < 1) {
							outFromServerToClient.flush();
						}
					}
				} while (read >= 0);

			} catch (IOException e) {
				System.out.println("Proxy to client HTTPS read Error");
				e.printStackTrace();

			}
		}
	}
}
