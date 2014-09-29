package com.partsoft.umsp;

import java.util.EventListener;

public interface ContextAttributeListener extends EventListener {

	void attributeAdded(ContextAttributeEvent contextAttributeEvent);

	void attributeRemoved(ContextAttributeEvent contextAttributeEvent);

	void attributeReplaced(ContextAttributeEvent contextAttributeEvent);

}
