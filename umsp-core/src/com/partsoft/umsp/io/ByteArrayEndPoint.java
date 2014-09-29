package com.partsoft.umsp.io;

import java.io.IOException;

import com.partsoft.umsp.EndPoint;

public class ByteArrayEndPoint implements EndPoint {
	byte[] _inBytes;
	ByteArrayBuffer _in;
	ByteArrayBuffer _out;
	boolean _closed;
	boolean _nonBlocking;
	boolean _growOutput;

	public ByteArrayEndPoint() {
	}

	public boolean isNonBlocking() {
		return _nonBlocking;
	}

	public void setNonBlocking(boolean nonBlocking) {
		_nonBlocking = nonBlocking;
	}

	public ByteArrayEndPoint(byte[] input, int outputSize) {
		_inBytes = input;
		_in = new ByteArrayBuffer(input);
		_out = new ByteArrayBuffer(outputSize);
	}

	public ByteArrayBuffer getIn() {
		return _in;
	}

	public void setIn(ByteArrayBuffer in) {
		_in = in;
	}

	public ByteArrayBuffer getOut() {
		return _out;
	}

	public void setOut(ByteArrayBuffer out) {
		_out = out;
	}

	public boolean isOpen() {
		return !_closed;
	}

	public boolean isBlocking() {
		return !_nonBlocking;
	}

	public boolean blockReadable(long millisecs) {
		return true;
	}

	public boolean blockWritable(long millisecs) {
		return true;
	}
	
	public void shutdownOutput() throws IOException {
	}

	public void close() throws IOException {
		_closed = true;
	}

	public int fill(Buffer buffer) throws IOException {
		if (_closed)
			throw new IOException("CLOSED");
		if (_in == null)
			return -1;
		if (_in.length() <= 0)
			return _nonBlocking ? 0 : -1;
		int len = buffer.put(_in);
		_in.skip(len);
		return len;
	}

	public int fill(Buffer buffer, int size) throws IOException {
		if (_closed)
			throw new IOException("CLOSED");
		if (_in == null || size <= 0)
			return -1;
		if (_in.length() <= 0 || _in.length() < size)
			return _nonBlocking ? 0 : -1;
		int len = buffer.put(_in.array(), _in.getIndex(), size);
		_in.skip(len);
		return len;
	}

	public int flush(Buffer buffer) throws IOException {
		if (_closed)
			throw new IOException("CLOSED");
		if (_growOutput && buffer.length() > _out.space()) {
			_out.compact();

			if (buffer.length() > _out.space()) {
				ByteArrayBuffer n = new ByteArrayBuffer(_out.putIndex() + buffer.length());

				n.put(_out.peek(0, _out.putIndex()));
				if (_out.getIndex() > 0) {
					n.mark();
					n.setGetIndex(_out.getIndex());
				}
				_out = n;
			}
		}
		int len = _out.put(buffer);
		buffer.skip(len);
		return len;
	}

	public int flush(Buffer header, Buffer buffer, Buffer trailer) throws IOException {
		if (_closed)
			throw new IOException("CLOSED");

		int flushed = 0;

		if (header != null && header.length() > 0)
			flushed = flush(header);

		if (header == null || header.length() == 0) {
			if (buffer != null && buffer.length() > 0)
				flushed += flush(buffer);

			if (buffer == null || buffer.length() == 0) {
				if (trailer != null && trailer.length() > 0) {
					flushed += flush(trailer);
				}
			}
		}

		return flushed;
	}

	public void reset() {
		_closed = false;
		_in.clear();
		_out.clear();
		if (_inBytes != null)
			_in.setPutIndex(_inBytes.length);
	}

	public String getLocalAddr() {
		return null;
	}

	public String getLocalHost() {
		return null;
	}

	public int getLocalPort() {
		return 0;
	}

	public String getRemoteAddr() {
		return null;
	}

	public String getRemoteHost() {
		return null;
	}

	public int getRemotePort() {
		return 0;
	}

	public Object getTransport() {
		return _inBytes;
	}

	public void flush() throws IOException {
	}

	public boolean isBufferingInput() {
		return false;
	}

	public boolean isBufferingOutput() {
		return false;
	}

	public boolean isBufferred() {
		return false;
	}

	public boolean isGrowOutput() {
		return _growOutput;
	}

	public void setGrowOutput(boolean growOutput) {
		_growOutput = growOutput;
	}

}
