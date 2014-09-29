package com.partsoft.umsp;

import java.util.EventListener;

public interface ContextListener extends EventListener {
	
	void contextInitialized(ContextEvent contextEvent);

	void contextDestroyed(ContextEvent contextEvent);
	
}
