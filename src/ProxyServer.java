
//package Proxy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyServer {

	// cache is a Map: the key is the URL and the value is the file name of the file
	// that stores the cached content
	Map<String, File> cache;

	ServerSocket proxySocket;
	Socket welcomSocket;
	RequestHandler requestHandler;

	File logDir = null;
	String logFileName = "/log.txt";
	File logFile = null;
	PrintWriter logWriter;

	public static void main(String[] args) {
		 new ProxyServer().startServer(Integer.parseInt(args[0]));
		// Using Hardcoded server port for testing
		//new ProxyServer().startServer(7777);
	}

	void startServer(int proxyPort) {
		System.out.println("Starting Server on port: " + proxyPort);
		System.out.println("Creating Cache: ");

		// create a cache object
		cache = new ConcurrentHashMap<>();
		try {
			// create the directory to store cached files.
			File cacheDir = new File("cached");
			if (!cacheDir.exists() || (cacheDir.exists() && !cacheDir.isDirectory())) {
				cacheDir.mkdirs();
				System.out.println("Creating Cached directory: ");
			}

			// create the directory to store a log file.
			logDir = new File("log");
			if (!logDir.exists() || (logDir.exists() && !logDir.isDirectory())) {
				logDir.mkdirs();
				System.out.println("Creating Log directory: ");
			}
			
			logFile = new File(logDir + logFileName);
			if (!logFile.exists() || (logFile.exists() && !logFile.isFile())) {
				logFile.createNewFile();
				System.out.println("Creating logWriter to file: " + logFile.getPath());
			}
			
			logWriter = new PrintWriter(new FileWriter(logFile, true));
			
			System.out.println("Creating ServerSocket on proxy port: " + proxyPort);
			proxySocket = new ServerSocket(proxyPort);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// While the proxy server is running
		// Need to change the flag from hardcoded true to a flaggable value
		// Leave as true for now to have to manually close proxy exe
		while (!proxySocket.isClosed()) {
			try {
				welcomSocket = proxySocket.accept();
				System.out.println("Create welcome socket on Local Port: " + welcomSocket.getLocalPort());

				requestHandler = new RequestHandler(welcomSocket, this);
				Thread requestThread = new Thread(requestHandler);
				requestThread.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public File getCache(String hashcode) {
		return cache.get(hashcode);
	}

	public void putCache(String hashcode, File fileName) { 
		System.out.println("PUT Cache Item hashcode: " + hashcode);
		cache.put(hashcode, fileName);
	}

	public synchronized void writeLog(String clientIpAddress, String url, String serverIpAddress) {

		String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
		String log = timeStamp + " : " + clientIpAddress + " : " + url + " : " + serverIpAddress + "\n";

		logWriter.append(log); 
		logWriter.flush(); 
	}

}
