package mim.ucip;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
				sendRequest(entry);
				
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
	
	private int processResponse(String xml) throws XPathExpressionException {
		
		log.info("Processing response: " + xml);
		
		XPathFactory xpathFactory = XPathFactory.newInstance();
	    XPath xpath = xpathFactory.newXPath();
	    String resultCode = "";
	    
		InputSource source = new InputSource(new StringReader(xml));		
		resultCode = xpath.evaluate("/Response/resultCode", source);
		
		if(resultCode == null)
			return -1;
		
		try {
			return Integer.parseInt(resultCode.trim());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		return -1;
	}
}