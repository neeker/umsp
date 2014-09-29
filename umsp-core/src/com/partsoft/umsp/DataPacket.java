package com.partsoft.umsp;

import java.io.Serializable;
import java.util.Date;

@SuppressWarnings("rawtypes")
public interface DataPacket extends Serializable, Comparable  {
	
	int getDataSize();
	
	int getBufferSize();
	
	long getCreateTimeMillis();
	
	Date getCreateTime();
	
	int getCommandID();
	
}
