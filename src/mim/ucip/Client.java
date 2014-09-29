package mim.ucip;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import mim.renewal.model.RenewalEntry;
import mim.renewal.util.Util;
import mim.ucip.util.RequestBuilder;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;


public class Client {
	private static String ucipIP = "127.0.0.1";
	private static int ucipPort = 8081;
	
	private Logger log = Logger.getLogger(getClass().getName());
	
	int index = 0;
	
	static {
		ResourceBundle myResources = ResourceBundle.getBundle("renewal");
		try {
			ucipPort = Integer.parseInt(myResources.getString("ucip.port"));
			ucipIP = myResources.getString("ucip.ip");
			
		} catch (Exception e) { }
	}
	
	public void sendRequest(List<RenewalEntry> entries) {
		for(RenewalEntry entry : entries) {
			if(entry.getStat() != 2) {
				//sendRequest(entry);
				
				entry.setReferenceId(Util.generateReferenceId());
				
				Calendar expiryCal = Calendar.getInstance();
				expiryCal.add(Calendar.DAY_OF_MONTH, 7);
				
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
				
				String chargingRequest = RequestBuilder.build(RequestBuilder.REQUEST_FUNCTION_CHARGE,  
					new String[] { "msisdn", entry.getaParty(), "expirydate", 
						sdf.format(expiryCal.getTime()), "refid", entry.getReferenceId(), 
						"amount", Integer.toString((int)entry.getAmount())
					});
				
				
				
				RequestResult rs = sendRequest(ucipIP, ucipPort, 30, 30, chargingRequest, entry.getReferenceId());
				
				int errorCode = processResponse(rs.responseString);
				
				entry.setErrorCode(Integer.toString(errorCode));
				
			    switch(errorCode) {
			    	case 0:
			    		entry.setStat(2);
			    		break;
			    		
			    	case 124:
			    		entry.setStat(3);
			    		break;
			    		
			    	default:
			    		entry.setStat(1);
			    }
				
			    entry.setRetryCount(entry.getRetryCount() + 1);
				
				index++;
			}
		}
	}
	
	public void sendRequest(RenewalEntry entry) {
		Socket clientSocket = null;
		PrintWriter out = null;
		BufferedReader in = null;
		
		try {
			
			entry.setReferenceId(Util.generateReferenceId());
			
			Calendar expiryCal = Calendar.getInstance();
			expiryCal.add(Calendar.DAY_OF_MONTH, 7);
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
			
			String chargingRequest = RequestBuilder.build(RequestBuilder.REQUEST_FUNCTION_CHARGE,  
				new String[] { "msisdn", entry.getaParty(), "expirydate", 
					sdf.format(expiryCal.getTime()), "refid", entry.getReferenceId(), 
					"amount", Double.toString(entry.getAmount())
				});
			
			log.info("Sending charging request: " + chargingRequest);
			
			clientSocket = new Socket(ucipIP, ucipPort);
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			
			StringBuilder sb = new StringBuilder();
		    String line;
		    while ((line = in.readLine()) != null)
		    	sb.append(line).append("\n");
			
			//if(index % 5 == 0)
			//	sb.append("<Response><result>2</result><resultCode>000</resultCode><description>20140724110615222</description></Response>");
			//else
			//	sb.append("<Response><result>2</result><resultCode>124</resultCode><description>20140724110615222</description></Response>");
		    
			int errorCode = processResponse(sb.toString());
			
			entry.setErrorCode(Integer.toString(errorCode));
			
		    switch(errorCode) {
		    	case 0:
		    		entry.setStat(2);
		    		break;
		    		
		    	case 124:
		    		entry.setStat(3);
		    		break;
		    		
		    	default:
		    		entry.setStat(1);
		    }
			
		    entry.setRetryCount(entry.getRetryCount() + 1);
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {						
			try {
				if(out != null)
					out.close();
				
				if(in != null)
					in.close();
				
				if(clientSocket != null)
					clientSocket.close();
			} catch (IOException e) {
				log.error("sendRequest(): failed to clear resources", e);
			}			
		}
	}
	
	private int processResponse(String xml) {
		
		log.info("Processing response: " + xml);
		
		XPathFactory xpathFactory = XPathFactory.newInstance();
	    XPath xpath = xpathFactory.newXPath();
	    String resultCode = null;
	    
		InputSource source = new InputSource(new StringReader(xml));		
		try {
			resultCode = xpath.evaluate("/Response/resultCode", source);
		} catch (XPathExpressionException e1) {
			e1.printStackTrace();
		}
		
		if(resultCode == null)
			return -1;
		
		try {
			return Integer.parseInt(resultCode.trim());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		return -1;
	}
	
	public RequestResult sendRequest(String IP, int port, int connectTimeout,
			int waitTimeout, String requestMsg, String refID) {
		RequestResult result = new RequestResult();
		DataInputStream incomingStream = null;
		DataOutputStream outgoingStream = null;
		String replyMsg = "";

		int tokencount = 0;
		String input = "";

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
				String s1 = Character.toString(ch);
				replyMsg = replyMsg + (char) token;
				tokencount++;

				if (ch == '\n') {
					break;
				}
				timeRunning = new Date().getTime() - startTime;
			}
			log.debug("[" + refID + "][send to server] time running:" + timeRunning + " MSec");
			log.debug("[" + refID + "][send to server] completed");

			clientsock.close();
			outgoingStream.close();
			incomingStream.close();
			result.requestResultCode = 0;
			result.responseString = replyMsg;
			log.debug("[" + refID + "][Response Msg] <- "
					+ replyMsg);
		} catch (IOException ioExp) {
			log
					.error("[" + refID + "][send to server] Exception error:"
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
				log.error("[" + refID
						+ "][send to server] Error: [" + ex.getMessage() + "]");
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