package com.partsoft.umsp;

import java.util.EventListener;

public interface RequestAttributeListener extends EventListener {
	
	public abstract void attributeAdded(RequestAttributeEvent requestAttributeEvent);

	public abstract void attributeRemoved(RequestAttributeEvent requestAttributeEvent);

	public abstract void attributeReplaced(RequestAttributeEvent requestAttributeEvent);
	
}
