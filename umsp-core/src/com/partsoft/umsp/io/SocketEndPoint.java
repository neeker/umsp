package com.partsoft.umsp.io;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;


public class SocketEndPoint extends StreamEndPoint {

	public static final String ALL_INTERFACES = "0.0.0.0";

	final Socket _socket;
	final InetSocketAddress _local;
	final InetSocketAddress _remote;

	public SocketEndPoint(Socket socket) throws IOException {
		super(socket.getInputStream(), socket.getOutputStream());
		_socket = socket;
		_local = (InetSocketAddress) _socket.getLocalSocketAddress();
		_remote = (InetSocketAddress) _socket.getRemoteSocketAddress();
	}

	public boolean isOpen() {
		return super.isOpen() && _socket != null && !_socket.isClosed() && !_socket.isInputShutdown()
				&& !_socket.isOutputShutdown();
	}

	public void shutdownOutput() throws IOException {
		if (!_socket.isClosed() && !_socket.isOutputShutdown())
			_socket.shutdownOutput();
	}

	public void close() throws IOException {
		_socket.close();
		_in = null;
		_out = null;
	}

	public String getLocalAddr() {
		if (_local == null || _local.getAddress() == null || _local.getAddress().isAnyLocalAddress())
			return ALL_INTERFACES;

		return _local.getAddress().getHostAddress();
	}

	public String getLocalHost() {
		if (_local == null || _local.getAddress() == null || _local.getAddress().isAnyLocalAddress())
			return ALL_INTERFACES;

		return _local.getAddress().getCanonicalHostName();
	}

	public int getLocalPort() {
		if (_local == null)
			return -1;
		return _local.getPort();
	}

	public String getRemoteAddr() {
		if (_remote == null)
			return null;
		InetAddress addr = _remote.getAddress();
		return (addr == null ? null : addr.getHostAddress());
	}

	public String getRemoteHost() {
		if (_remote == null)
			return null;
		return _remote.getAddress().getCanonicalHostName();
	}

	public int getRemotePort() {
		if (_remote == null)
			return -1;
		return _remote.getPort();
	}

	public Object getTransport() {
		return _socket;
	}

	@Override
	public String toString() {
		return "套接字 [本地=" + _local + ", 远程=" + _remote + "]";
	}

}
