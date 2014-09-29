package com.partsoft.umsp.handler;

import java.util.EventObject;

public class RecipientEvent extends EventObject {

	private static final long serialVersionUID = -2861579140090498789L;
	
	private Object target;
	
	public RecipientEvent(Object source) {
		super(source);
	}
	
	public RecipientEvent(Object source, Object target) {
		super(source);
		this.target = target;
	}
	
	public Object getTarget() {
		return target;
	}

}
