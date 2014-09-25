package mim.ucip.util;

public class RequestBuilder {
	public static String REQUEST_FUNCTION_CHARGE = "chargeUser";
	
	public static String build(String function, String... nameValuePairs) {
		StringBuilder sb = new StringBuilder("<methodCall>");
		sb.append("<function>" + function + "</function>");
		
		if(nameValuePairs.length > 0 && nameValuePairs.length % 2 == 0) {
			for(int i=0; i<nameValuePairs.length/2; i++) {
				sb.append("<");
				sb.append(nameValuePairs[i*2]);
				sb.append(">");
				
				sb.append(nameValuePairs[i*2 + 1]);
				
				sb.append("</");
				sb.append(nameValuePairs[i*2]);
				sb.append(">");
			}
		}
		
		sb.append("</methodCall>");
		
		return sb.toString();
	}
}
