package com.partsoft.umsp.io;

public interface BufferPools {
	
	public Buffer getBuffer(int size);

	public void returnBuffer(int size, Buffer buffer);
	
}