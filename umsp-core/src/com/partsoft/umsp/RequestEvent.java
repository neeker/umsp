package com.partsoft.umsp;

import java.util.EventObject;

public class RequestEvent extends EventObject {
	
	private static final long serialVersionUID = -2032966296066597085L;
	
	private Request request;

	public RequestEvent(Context source, Request request) {
		super(source);
		this.request = request;
	}
	
	public Request getRequest() {
		return request;
	}
	
	public Context getContext() {
		return (Context) super.getSource();
	}
	
}
