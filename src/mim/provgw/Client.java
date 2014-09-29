package mim.provgw;

import java.util.ResourceBundle;

import mim.ucip.RequestResult;
import mim.ucip.util.network.TcpClient;

import org.apache.log4j.Logger;

public class Client {
	private static String provGwIP = "127.0.0.1";
	private static int provGwPort = 9091;
	private TcpClient client;
	
	private Logger log = Logger.getLogger(getClass().getName());
	
	static {
		ResourceBundle myResources = ResourceBundle.getBundle("renewal");
		try {
			provGwIP = myResources.getString("provgw.ip");
			provGwPort = Integer.parseInt(myResources.getString("provgw.port"));
			
		} catch (Exception e) { }
	}
	
	public Client() {
		client = new TcpClient();
	}
	
	public void sendRequest(String request, String referenceId) {
		
		RequestResult r = client.sendRequest(provGwIP, provGwPort, 30, 30, request, referenceId);
		
		log.debug("ProvGw response: " + r.responseString);
		
	}
}
