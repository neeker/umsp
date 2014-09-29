package com.partsoft.umsp;

import java.util.EventObject;

public class ContextEvent extends EventObject {

	private static final long serialVersionUID = -8930628058247795048L;

	public ContextEvent(Context source) {
		super(source);
	}

	public Context getContext() {
		return ((Context) super.getSource());
	}
}
