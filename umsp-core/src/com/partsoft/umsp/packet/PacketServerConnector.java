package com.partsoft.umsp.packet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.partsoft.umsp.io.Buffer;
import com.partsoft.umsp.io.ByteArrayBuffer;
import com.partsoft.umsp.io.EofException;
import com.partsoft.umsp.log.Log;

public class PacketServerConnector extends AbstractConnector {

	protected ServerSocket _serverSocket;

	// accept线程数量
	private int _acceptors = 1;

	// 设置socket连接排队数(0表示为由操作系统控制)
	private int _acceptQueueSize = 0;

	public PacketServerConnector() {
		super();
	}

	public PacketServerConnector(String protocol) {
		super(protocol);
	}
	
	public PacketServerConnector(int port) {
		super();
		setPort(port);
	}
	
	public Object getConnection() {
		return _serverSocket;
	}

	/**
	 * @return Returns the acceptQueueSize.
	 */
	public int getAcceptQueueSize() {
		return _acceptQueueSize;
	}

	/**
	 * @param acceptQueueSize
	 *            The acceptQueueSize to set.
	 */
	public void setAcceptQueueSize(int acceptQueueSize) {
		_acceptQueueSize = acceptQueueSize;
	}

	/**
	 * @return Returns the number of acceptor threads.
	 */
	public int getAcceptors() {
		return _acceptors;
	}

	/**
	 * @param acceptors
	 *            The number of acceptor threads to set.
	 */
	public void setAcceptors(int acceptors) {
		_acceptors = acceptors;
	}

	public void open() throws IOException {
		// Create a new server socket and set to non blocking mode
		if (_serverSocket == null || _serverSocket.isClosed())
			_serverSocket = newServerSocket(getHost(), getPort(), getAcceptQueueSize());
		_serverSocket.setReuseAddress(getReuseAddress());
	}

	protected ServerSocket newServerSocket(String host, int port, int backlog) throws IOException {
		ServerSocket ss = host == null ? new ServerSocket(port, backlog) : new ServerSocket(port, backlog,
				InetAddress.getByName(host));
		return ss;
	}

	public void close() throws IOException {
		if (_serverSocket != null)
			_serverSocket.close();
		_serverSocket = null;
	}

	public void accept(int acceptorID) throws IOException, InterruptedException {
		Socket socket = _serverSocket.accept();
		if (Log.isDebugEnabled()) {
			Log.debug(String.format("客户端(%s)已经连接, 设置套接字参数...", socket.getRemoteSocketAddress()
					.toString()));
		}
		configure(socket);
		Connection connection = new Connection(socket);
		connection.dispatch();
	}

	protected Buffer newBuffer(int size) {
		return new ByteArrayBuffer(size);
	}

	public int getLocalPort() {
		if (_serverSocket == null || _serverSocket.isClosed())
			return -1;
		return _serverSocket.getLocalPort();
	}

	protected void doStart() throws Exception {
		super.doStart();
		// Start selector thread
		synchronized (this) {
			_dispatchedThreads = new Thread[getAcceptors()];

			for (int i = 0; i < _dispatchedThreads.length; i++) {
				if (!getThreadPool().dispatch(new Acceptor(i))) {
					Log.warn(String.format("连接请求超出最大线程池配置(%s)", getClass().getName()));
					break;
				}
			}
		}
	}

	private class Acceptor implements Runnable {
		int _acceptor = 0;

		Acceptor(int id) {
			_acceptor = id;
		}

		public void run() {
			Thread current = Thread.currentThread();
			String name;
			synchronized (PacketServerConnector.this) {
				if (_dispatchedThreads == null)
					return;

				_dispatchedThreads[_acceptor] = current;
				name = _dispatchedThreads[_acceptor].getName();
				current.setName(name + "@" + PacketServerConnector.this + "[" + _acceptor + "]");
			}
			int old_priority = current.getPriority();

			try {
				current.setPriority(old_priority - getAcceptorPriorityOffset());
				while (isRunning() && getConnection() != null) {
					try {
						if (Log.isDebugEnabled()) {
							Log.debug("等待客户连接...");
						}
						accept(_acceptor);
					} catch (EofException e) {
						Log.ignore(e);
					} catch (IOException e) {
						Log.ignore(e);
					} catch (ThreadDeath e) {
						throw e;
					} catch (Throwable e) {
						Log.warn(e);
					}
				}
			} finally {
				current.setPriority(old_priority);
				current.setName(name);

				synchronized (PacketServerConnector.this) {
					if (_dispatchedThreads != null)
						_dispatchedThreads[_acceptor] = null;
				}
			}
		}
	}

}
