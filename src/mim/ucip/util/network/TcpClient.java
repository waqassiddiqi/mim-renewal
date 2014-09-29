package mim.ucip.util.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Date;

import org.apache.log4j.Logger;

import mim.ucip.RequestResult;

public class TcpClient {
	
	private Logger log = Logger.getLogger(getClass().getName());
	
	public RequestResult sendRequest(String IP, int port, int connectTimeout,
			int waitTimeout, String requestMsg, String refID) {
		
		RequestResult result = new RequestResult();
		DataInputStream incomingStream = null;
		DataOutputStream outgoingStream = null;
		String replyMsg = "";


		Socket clientsock = null;
		try {
			SocketAddress sockaddr = new InetSocketAddress(IP, port);
			clientsock = new Socket();
			log.debug("[" + refID + "][send to server] Socket ID created");
			int timeoutConnectMs = connectTimeout;
			int timeoutWaitMs = waitTimeout;
			clientsock.setSoTimeout(timeoutWaitMs);
			clientsock.connect(sockaddr, timeoutConnectMs);
			log.debug("[" + refID + "][send to server] Socket connected");

			outgoingStream = new DataOutputStream(clientsock.getOutputStream());
			requestMsg = requestMsg + "\r\r";
			byte[] buf = requestMsg.getBytes();
			outgoingStream.write(buf);
			Thread.sleep(10L);
			outgoingStream.flush();
			log.debug("[" + refID + "][Request Msg] -> " + requestMsg);
			log.debug("[" + refID + "] Data Flushed");

			log.debug("[" + refID + "][send to server] Buffer sent");

			incomingStream = new DataInputStream(clientsock.getInputStream());
			log.debug("[" + refID + "][send to server] Waiting reply");

			long startTime = new Date().getTime();
			long timeRunning = 0L;
			int token;
			while ((token = incomingStream.read()) != -1) {
				char ch = (char) token;
				replyMsg = replyMsg + (char) token;

				if (ch == '\n') {
					break;
				}
				
				timeRunning = new Date().getTime() - startTime;
			}
			log.debug("[" + refID + "][send to server] time running:"
					+ timeRunning + " MSec");
			log.debug("[" + refID + "][send to server] completed");

			clientsock.close();
			outgoingStream.close();
			incomingStream.close();
			result.requestResultCode = 0;
			result.responseString = replyMsg;
			log.debug("[" + refID + "][Response Msg] <- " + replyMsg);
		} catch (IOException ioExp) {
			log.error("[" + refID + "][send to server] Exception error:"
					+ ioExp.getMessage());
			result.requestResultCode = -2;
			result.responseString = ioExp.getMessage();
		} catch (Exception ex) {
			log.error("[" + refID + "][send to server] Error: ["
					+ ex.getMessage() + "]");
			result.requestResultCode = -1;
			result.responseString = ex.getMessage();
		} finally {
			try {
				closeSilently(clientsock);
				clientsock.close();
				outgoingStream.close();
				incomingStream.close();
			} catch (Exception ex) {
				log.error("[" + refID + "][send to server] Error: ["
						+ ex.getMessage() + "]");
			}
		}
		return result;
	}

	public void closeSilently(Socket s) {
		if (s != null) {
			try {
				s.close();
			} catch (IOException e2) {
				log.error("Exception while closing socket: " + e2.toString());
			}
		}
	}
}
