package com.partsoft.umsp;

import java.util.EventObject;

public class ContextAttributeEvent extends EventObject {

	private static final long serialVersionUID = 3758956604133587446L;

	private String name;

	private Object value;

	public ContextAttributeEvent(Context source, String name, Object value) {
		super(source);
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return this.name;
	}

	public Object getValue() {
		return this.value;
	}

}
