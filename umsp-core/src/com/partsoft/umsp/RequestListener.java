package com.partsoft.umsp;

import java.util.EventListener;

public interface RequestListener extends EventListener {
	
	void requestDestroyed(RequestEvent requestEvent);

	void requestInitialized(RequestEvent requestEvent);
	
}