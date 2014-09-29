package com.partsoft.umsp.cmpp;

public abstract class AbstractCmppContextSPHandler extends AbstractCmppContextHandler {

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

	public void setSpNumber(int spNumber) {
		this.spNumber = spNumber;
	}

}
