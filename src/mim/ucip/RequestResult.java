package mim.ucip;

public class RequestResult {
	public int requestResultCode;
	public String responseString;
	
	@Override
	public String toString() {
		return "RequestResult [requestResultCode=" + requestResultCode
				+ ", responseString=" + responseString + "]";
	}
}
