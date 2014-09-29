package com.partsoft.umsp.io;

import java.io.IOException;
import java.io.InputStream;


public class ByteBufferInputStream extends InputStream {

	protected final Buffer buffer;

	public ByteBufferInputStream(Buffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public int read() {
		try {
			return buffer != null && buffer.length() > 0 ? buffer.get() & 0xFF : -1;
		} catch (RuntimeIOException e) {
			return -1;
		}
	}

	@Override
	public int available() throws IOException {
		if (buffer == null)
			throw new IOException();
		return buffer.length();
	}

	@Override
	public synchronized void mark(int readlimit) {
		if (buffer != null)
			buffer.mark(0);
	}

	@Override
	public boolean markSupported() {
		return buffer != null;
	}

	@Override
	public synchronized void reset() throws IOException {
		if (buffer == null)
			throw new IOException();
		buffer.reset();
	}

}
