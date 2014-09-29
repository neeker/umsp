package com.partsoft.umsp.packet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

import com.partsoft.umsp.EndPoint;
import com.partsoft.umsp.MultiException;
import com.partsoft.umsp.io.Buffer;
import com.partsoft.umsp.io.ByteArrayBuffer;
import com.partsoft.umsp.io.EofException;
import com.partsoft.umsp.log.Log;
import com.partsoft.utils.Assert;

public class PacketClientConnector extends AbstractConnector {

	static final int SLEEP_TIME = 200;

	protected int _maxConnection = 1;

	protected int _connectTimeout = 1500;

	/**
	 * @brief 重连间隔时间，单位：毫秒，默认值(30秒)
	 */
	protected long _reConnectIntervalTime = 30000;

	protected boolean _autoReConnect = false;

	private Proxy proxy;

	private int _dispatchedConnections = 0;

	public PacketClientConnector() {
	}

	public PacketClientConnector(String host, int port) {
		super();
		setHost(host);
		setPort(port);
	}

	public boolean isAutoReConnect() {
		return this._autoReConnect;
	}

	public void setAutoReConnect(boolean c) {
		this._autoReConnect = c;
	}

	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	public Proxy getProxy() {
		return proxy;
	}

	public void setConnectTimeout(int connectTimeout) {
		this._connectTimeout = connectTimeout;
	}

	public int getConnectTimeout() {
		return _connectTimeout;
	}

	public PacketClientConnector(String protocol, String host, int port) {
		super(protocol);
		setHost(host);
		setPort(port);
	}

	public Object getConnection() {
		return null;
	}

	public void open() throws IOException {
	}

	protected Socket newClientSocket(String host, int port) throws IOException {
		Socket socket = proxy != null ? new Socket(proxy) : new Socket();
		configure(socket);
		socket.connect(new InetSocketAddress(InetAddress.getByName(host), port), getConnectTimeout());
		return socket;
	}

	public void close() throws IOException {
	}

	public long getReConnectIntervalTime() {
		return _reConnectIntervalTime;
	}

	public void setReConnectIntervalTime(long reConnectIntervalTime) {
		this._reConnectIntervalTime = reConnectIntervalTime;
	}

	public int getMaxConnection() {
		return _maxConnection;
	}

	public void setMaxConnection(int maxConnection) {
		this._maxConnection = maxConnection;
	}

	public void connect(int connectID) throws IOException, InterruptedException {
		Socket _clientSocket = newClientSocket(getHost(), getPort());
		if (Log.isDebugEnabled()) {
			Log.debug(String.format("已连接至服务器(%s), 设置套接字参数...", _clientSocket.getRemoteSocketAddress().toString()));
		}
		configure(_clientSocket);
		Connection connection = new Connection(_clientSocket);
		connection.dispatch();
		synchronized (this) {
			_dispatchedConnections++;
		}
	}

	public int getConnectedCount() {
		return _dispatchedConnections;
	}

	public boolean isConnected() {
		return _dispatchedConnections > 0;
	}

	protected Buffer newBuffer(int size) {
		return new ByteArrayBuffer(size);
	}

	public void customize(EndPoint endpoint, PacketRequest request) throws IOException {
		Connection connection = (Connection) endpoint;
		if (connection._sotimeout != _maxIdleTime) {
			connection._sotimeout = _maxIdleTime;
			((Socket) endpoint.getTransport()).setSoTimeout(_maxIdleTime);
		}

		super.customize(endpoint, request);
	}

	public int getLocalPort() {
		return -1;
	}

	protected void doStart() throws Exception {
		setStatsOn(true);
		if (_reConnectIntervalTime < SLEEP_TIME)
			_reConnectIntervalTime = SLEEP_TIME;
		super.doStart();
		// Start selector thread
		_dispatchedConnections = 0;
		Assert.isTrue(_maxConnection > 0);
		synchronized (this) {
			_dispatchedThreads = new Thread[1];
			Assert.isTrue(getThreadPool().dispatch(new Connector(0)));
			Thread.yield();
		}
	}

	protected void doStop() throws Exception {
		super.doStop();
	}

	@Override
	protected void connectionException(PacketConnection connection, Throwable e) {
		boolean log_exception = true;
		if (e instanceof EofException && isIgnoredEofException()) {
			log_exception = false;
		}
		if (log_exception) {
			super.connectionException(connection, e);
			if (!isAutoReConnect()) {
				getOrigin().pushDelayException(e);
			}
		}
	}

	@Override
	protected void connectionOpened(PacketConnection connection) {
		super.connectionOpened(connection);
	}

	@Override
	protected void connectionClosed(PacketConnection connection) {
		super.connectionClosed(connection);
		synchronized (this) {
			_dispatchedConnections--;
		}

		if (isAutoReConnect()) {
			return;
		}

		if (_dispatchedConnections <= 0) {
			try {
				getOrigin().delayStop();
				if (Log.isDebugEnabled())
					Log.debug("无连接退出");
			} catch (Exception e) {
				Log.error(e.getMessage(), e);
			}
		}
	}

	private class Connector implements Runnable {
		int _connector = 0;

		Connector(int id) {
			_connector = id;
		}

		private void doAutoReConnectHandle() {
			if (Log.isDebugEnabled()) {
				Log.debug("进入自动连接模式@" + PacketClientConnector.this + "[" + _connector + "]");
			}
			Thread current = Thread.currentThread();
			String name;
			synchronized (PacketClientConnector.this) {
				if (_dispatchedThreads == null)
					return;

				_dispatchedThreads[_connector] = current;
				name = _dispatchedThreads[_connector].getName();
				current.setName(name + "@" + PacketClientConnector.this + "[" + _connector + "]");
			}
			int old_priority = current.getPriority();
			try {
				current.setPriority(old_priority - getAcceptorPriorityOffset());
				for (int i = 0; i < getMaxConnection() && isStarted(); i++) {
					boolean wellBeBreak = false;
					if (Log.isDebugEnabled()) {
						Log.debug(String.format("正在连接至服务器(%s)...", getName()));
					}
					try {
						connect(i);
					} catch (Throwable e) {
						if (!isIgnoredConnectionException()) {
							Log.error(String.format("连接服务器(%s)失败，错误信息： %s", getName(), e.getMessage()));
						}
						if (Log.isDebugEnabled()) {
							Log.debug(e.getMessage(), e);
						}
						wellBeBreak = true;
					}
					try {
						Thread.sleep(SLEEP_TIME);
						Thread.yield();
					} catch (Throwable e) {
						Log.ignore(e);
					}
					if (wellBeBreak) {
						break;
					}
				}

				int sleepCount = 0;
				while (isStarted()) {
					try {
						Thread.sleep(SLEEP_TIME);
						Thread.yield();
					} catch (InterruptedException e) {
						break;
					}
					sleepCount = sleepCount + 1;
					if (!isStarted())
						break;

					if ((sleepCount * SLEEP_TIME) > getReConnectIntervalTime()) {
						synchronized (PacketClientConnector.this) {
							if (_dispatchedConnections < getMaxConnection()) {
								if (Log.isDebugEnabled()) {
									Log.debug(String.format("准备重新连接服务器(%s)", getName()));
								}
								try {
									connect(_dispatchedConnections);
								} catch (Throwable e) {
									if (!isIgnoredConnectionException()) {
										Log.error(String.format("连接服务器(%s)失败，错误信息： %s", getName(), e.getMessage()));
									}
									if (Log.isDebugEnabled()) {
										Log.debug(e.getMessage(), e);
									}
								}
							}
						}
						sleepCount = 0;
					}
				}
			} finally {
				current.setPriority(old_priority);
				current.setName(name);
				synchronized (PacketClientConnector.this) {
					if (_dispatchedThreads != null)
						_dispatchedThreads[_connector] = null;
				}
			}
		}

		private void doNormalConnectHandle() {
			Thread current = Thread.currentThread();
			String name;
			synchronized (PacketClientConnector.this) {
				if (_dispatchedThreads == null)
					return;

				_dispatchedThreads[_connector] = current;
				name = _dispatchedThreads[_connector].getName();
				current.setName(name + "@" + PacketClientConnector.this + "[" + _connector + "]");
			}
			int old_priority = current.getPriority();
			int connectioned = 0;
			MultiException mex = new MultiException();
			try {
				current.setPriority(old_priority - getAcceptorPriorityOffset());
				for (; connectioned < getMaxConnection() && isStarted(); connectioned++) {
					try {
						if (Log.isDebugEnabled()) {
							Log.debug(String.format("正在连接服务器(%s)...", getName()));
						}
						connect(connectioned);
					} catch (Throwable e) {
						mex.add(e);
					} finally {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							Log.ignore(e);
						}
					}
				}
			} finally {
				current.setPriority(old_priority);
				current.setName(name);
				synchronized (PacketClientConnector.this) {
					if (_dispatchedThreads != null)
						_dispatchedThreads[_connector] = null;
					if (connectioned <= 0) {
						try {
							mex.ifExceptionThrow();
						} catch (Exception e) {
							try {
								if (!isIgnoredConnectionException()) {
									Log.error(String.format("连接至服务器(%s)的当前配置要求至少成功连接", getName()));
								}
								getOrigin().pushDelayException(e);
								getOrigin().delayStop();
							} catch (Exception noe) {
								Log.error(noe.getMessage(), noe);
							}
						}
					} else {
						Log.ignore(mex);
					}
				}
			}
		}

		public void run() {
			if (isAutoReConnect()) {
				doAutoReConnectHandle();
			} else {
				doNormalConnectHandle();
			}
		}
	}

}
