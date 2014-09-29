package com.partsoft.umsp;

import java.io.IOException;

import com.partsoft.umsp.packet.PacketConnection;
import com.partsoft.umsp.thread.ThreadPool;

/**
 * 起源....
 * 
 * @author neeker
 */
public interface OriginHandler extends HandlerContainer {
	
	Container getContainer();
	
	ThreadPool getThreadPool();
	
	String getVersion();
	
	void delayStop();
	
	void pushDelayException(Throwable e);
	
	void handle(PacketConnection connection, int dispatch) throws IOException;
	
	public interface ShutdownGraceful {

		void setShutdown(boolean b);

	}

}
