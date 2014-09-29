package com.partsoft.umsp.smgp;

public abstract class AbstractSmgpContextSPHandler extends AbstractSmgpContextHandler {

	protected String account;

	protected String password;

	protected int spNumber;

	protected int enterpriseId;


	public void setAccount(String account) {
		this.account = account;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setEnterpriseId(int enterpriseId) {
		this.enterpriseId = enterpriseId;
	}

	public int getSpNumber() {
		return spNumber;
	}

	public void setSpNumber(int spNumber) {
		this.spNumber = spNumber;
	}
	

}
