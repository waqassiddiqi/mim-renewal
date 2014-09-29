package mim.renewal.model;

public class Subscriber {
	private String aParty;
	private int inActiveDays;
	private String expiry;
	
	public String getaParty() {
		return aParty;
	}
	public void setaParty(String aParty) {
		this.aParty = aParty;
	}
	
	public int getInActiveDays() {
		return inActiveDays;
	}
	public void setInActiveDays(int inActiveDays) {
		this.inActiveDays = inActiveDays;
	}
	
	public String getExpiry() {
		return expiry;
	}
	public void setExpiry(String expiry) {
		this.expiry = expiry;
	}
}
