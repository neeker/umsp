package com.partsoft.umsp.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.partsoft.umsp.EndPoint;

public class StreamEndPoint implements EndPoint {

	InputStream _in;

	OutputStream _out;

	public StreamEndPoint(InputStream in, OutputStream out) {
		_in = in;
		_out = out;
	}

	public boolean isBlocking() {
		return true;
	}

	public boolean blockReadable(long millisecs) throws IOException {
		return true;
	}

	public boolean blockWritable(long millisecs) throws IOException {
		return true;
	}

	public boolean isOpen() {
		return _in != null;
	}

	public final boolean isClosed() {
		return !isOpen();
	}

	public void shutdownOutput() throws IOException {
	}

	public void close() throws IOException {
		if (_in != null)
			_in.close();
		_in = null;
		if (_out != null)
			_out.close();
		_out = null;
	}

	public int fill(Buffer buffer, int size) throws IOException {
		if (_in == null || size <= 0)
			return 0;

		int space = buffer.space();
		if (space <= 0 || space < size) {
			if (buffer.hasContent())
				return 0;
			throw new IOException(String.format("预读数据(%d字节)大于缓冲区可用内存(%d字节)", size, space));
		} else if (space > size) {
			space = size;
		}
		int len = buffer.readFrom(_in, space);
		return len;
	}

	public int fill(Buffer buffer) throws IOException {
		if (_in == null)
			return 0;

		int space = buffer.space();
		if (space <= 0) {
			if (buffer.hasContent())
				return 0;
			throw new IOException("缓冲区无可用内存");
		}
		int len = buffer.readFrom(_in, space);
		return len;
	}

	public int flush(Buffer buffer) throws IOException {
		if (_out == null)
			return -1;
		int length = buffer.length();
		if (length > 0)
			buffer.writeTo(_out);
		buffer.clear();
		return length;
	}

	public int flush(Buffer header, Buffer buffer, Buffer trailer) throws IOException {
		int len = 0;
		if (header != null) {
			int tw = header.length();
			if (tw > 0) {
				int f = flush(header);
				len = f;
				if (f < tw)
					return len;
			}
		}

		if (buffer != null) {
			int tw = buffer.length();
			if (tw > 0) {
				int f = flush(buffer);
				if (f < 0)
					return len > 0 ? len : f;
				len += f;
				if (f < tw)
					return len;
			}
		}

		if (trailer != null) {
			int tw = trailer.length();
			if (tw > 0) {
				int f = flush(trailer);
				if (f < 0)
					return len > 0 ? len : f;
				len += f;
			}
		}
		return len;
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
		return null;
	}

	public InputStream getInputStream() {
		return _in;
	}

	public void setInputStream(InputStream in) {
		_in = in;
	}

	public OutputStream getOutputStream() {
		return _out;
	}

	public void setOutputStream(OutputStream out) {
		_out = out;
	}

	public void flush() throws IOException {
		_out.flush();
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

}
