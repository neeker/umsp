package com.partsoft.umsp.io;

import java.io.IOException;
import java.io.OutputStream;

public class ByteBufferOutputStream extends OutputStream {

	protected final Buffer buffer;

	public ByteBufferOutputStream(Buffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public void write(int b) throws IOException {
		if (buffer == null)
			throw new IOException();
		buffer.put((byte) b);
	}
}
