package com.partsoft.umsp;

public class RequestAttributeEvent extends RequestEvent {

	private static final long serialVersionUID = -4732756089424676471L;

	private String name;

	private Object value;

	public RequestAttributeEvent(Context source, Request request, String name, Object value) {
		super(source, request);
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public Object getValue() {
		return value;
	}

}
