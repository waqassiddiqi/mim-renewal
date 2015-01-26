package mim.renewal.model;

import java.util.Date;

public class RenewalEntry {
	private int id;
	private String aParty;
	private double amount;
	private int stat;
	private int chargeType;
	private Date created;
	private Date updated;
	private int retryCount;
	private int subscriberType;
	private String referenceId;
	private String errorCode;
	public int cos;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public String getaParty() {
		return aParty;
	}
	public void setaParty(String aParty) {
		this.aParty = aParty;
	}
	
	public double getAmount() {
		return amount;
	}
	public void setAmount(double amount) {
		this.amount = amount;
	}
	
	public int getStat() {
		return stat;
	}
	public void setStat(int stat) {
		this.stat = stat;
	}
	
	public int getChargeType() {
		return chargeType;
	}
	public void setChargeType(int chargeType) {
		this.chargeType = chargeType;
	}
	
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}
	
	public Date getUpdated() {
		return updated;
	}
	public void setUpdated(Date updated) {
		this.updated = updated;
	}
	
	public int getRetryCount() {
		return retryCount;
	}
	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}
	
	public int getSubscriberType() {
		return subscriberType;
	}
	public void setSubscriberType(int subscriberType) {
		this.subscriberType = subscriberType;
	}
	
	public String getReferenceId() {
		return referenceId;
	}
	public void setReferenceId(String referenceId) {
		this.referenceId = referenceId;
	}
	
	public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	
	public int getCos() {
		return cos;
	}
	public void setCos(int cos) {
		this.cos = cos;
	}
	
	@Override
	public String toString() {
		return "RenewalEntry [id=" + id + ", aParty=" + aParty + ", amount="
				+ amount + ", stat=" + stat + ", chargeType=" + chargeType
				+ ", created=" + created + ", updated=" + updated
				+ ", retryCount=" + retryCount + ", subscriberType="
				+ subscriberType + ", referenceId=" + referenceId
				+ ", errorCode=" + errorCode + "]";
	}
	
	
}
