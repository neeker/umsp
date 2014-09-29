package com.partsoft.umsp;

import java.io.IOException;

public interface Parser {
	
	void reset(boolean returnBuffers);
	
	boolean isComplete();
	
	long parseAvailable() throws IOException;
	
	boolean isMoreInBuffer();
	
	boolean isIdle();

}
