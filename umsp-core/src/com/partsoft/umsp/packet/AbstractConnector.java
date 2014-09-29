package com.partsoft.umsp.packet;

import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.partsoft.umsp.Connector;
import com.partsoft.umsp.EndPoint;
import com.partsoft.umsp.LifeCycle;
import com.partsoft.umsp.OriginHandler;
import com.partsoft.umsp.handler.PacketContextHandler;
import com.partsoft.umsp.io.AbstractBuffers;
import com.partsoft.umsp.io.Buffer;
import com.partsoft.umsp.io.EofException;
import com.partsoft.umsp.io.SocketEndPoint;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.thread.ThreadPool;

public abstract class AbstractConnector extends AbstractBuffers implements Connector {

	private String _name;

	private OriginHandler _originHandler;

	private String _protocol = PacketContextHandler.UNKNOWN_CONTEXT_PROTOCOL;

	private ThreadPool _threadPool;

	protected Set<Connection> _connectionSet;

	private String _host;
	private int _port = 0;

	// accept线程优先级偏移量
	private int _acceptorPriorityOffset = 0;

	private boolean _useDNS;

	private boolean _reuseAddress = true;

	// 最大socket数据读取等待时间(毫秒)，默认3秒
	protected int _maxIdleTime = 1500;

	// 处理资源低下时的数据读取等待时间(毫秒)，默认1.5秒
	protected int _lowResourceMaxIdleTime = 800;

	// 关闭时等待多长时间发送数据完成返回(毫秒)，默认3秒
	protected int _soLingerTime = 0;

	protected transient Thread[] _dispatchedThreads;

	Object _statsLock = new Object();
	transient long _statsStartedAt = -1;
	transient int _requests;
	transient int _connections; // total number of connections made to server

	transient int _connectionsOpen; // number of connections currently open
	transient int _connectionsOpenMin; // min number of connections open
										// simultaneously
	transient int _connectionsOpenMax; // max number of connections open
										// simultaneously

	transient long _connectionsDurationMin; // min duration of a connection
	transient long _connectionsDurationMax; // max duration of a connection
	transient long _connectionsDurationTotal; // total duration of all
												// coneection

	transient int _connectionsRequestsMin; // min requests per connection
	transient int _connectionsRequestsMax; // max requests per connection

	private boolean _ignoredEofException = true;

	private boolean _ignoredConnectionException = false;

	public void setIgnoredEofException(boolean ignoredEofException) {
		this._ignoredEofException = ignoredEofException;
	}

	public boolean isIgnoredEofException() {
		return _ignoredEofException;
	}

	public boolean isIgnoredConnectionException() {
		return _ignoredConnectionException;
	}

	public void setIgnoredConnectionException(boolean value) {
		this._ignoredConnectionException = value;
	}

	public AbstractConnector() {
		this._protocol = "UNKNOWN";
	}

	public AbstractConnector(String protocol) {
		this._protocol = protocol;
	}

	public OriginHandler getOrigin() {
		return _originHandler;
	}

	public void setOrigin(OriginHandler server) {
		_originHandler = server;
	}

	public ThreadPool getThreadPool() {
		return _threadPool;
	}

	public void setThreadPool(ThreadPool pool) {
		_threadPool = pool;
	}

	public void setHost(String host) {
		_host = host;
	}

	public String getHost() {
		return _host;
	}

	public void setPort(int port) {
		_port = port;
	}

	public int getPort() {
		return _port;
	}

	/**
	 * @return Returns the maxIdleTime.
	 */
	public int getMaxIdleTime() {
		return _maxIdleTime;
	}

	/**
	 * Set the maximum Idle time for a connection, which roughly translates to
	 * the {@link Socket#setSoTimeout(int)} call, although with NIO
	 * implementations other mechanisms may be used to implement the timeout.
	 * The max idle time is applied:
	 * <ul>
	 * <li>When waiting for a new request to be received on a connection</li>
	 * <li>When reading the headers and content of a request</li>
	 * <li>When writing the headers and content of a response</li>
	 * </ul>
	 * Jetty interprets this value as the maximum time between some progress
	 * being made on the connection. So if a single byte is read or written,
	 * then the timeout (if implemented by jetty) is reset. However, in many
	 * instances, the reading/writing is delegated to the JVM, and the semantic
	 * is more strictly enforced as the maximum time a single read/write
	 * operation can take. Note, that as Jetty supports writes of memory mapped
	 * file buffers, then a write may take many 10s of seconds for large content
	 * written to a slow device.
	 * <p>
	 * Previously, Jetty supported separate idle timeouts and IO operation
	 * timeouts, however the expense of changing the value of soTimeout was
	 * significant, so these timeouts were merged. With the advent of NIO, it
	 * may be possible to again differentiate these values (if there is demand).
	 * 
	 * @param maxIdleTime
	 *            The maxIdleTime to set.
	 */
	public void setMaxIdleTime(int maxIdleTime) {
		_maxIdleTime = maxIdleTime;
	}

	/**
	 * @return Returns the maxIdleTime.
	 */
	public int getLowResourceMaxIdleTime() {
		return _lowResourceMaxIdleTime;
	}

	/**
	 * @param maxIdleTime
	 *            The maxIdleTime to set.
	 */
	public void setLowResourceMaxIdleTime(int maxIdleTime) {
		_lowResourceMaxIdleTime = maxIdleTime;
	}

	/**
	 * @return Returns the soLingerTime.
	 */
	public int getSoLingerTime() {
		return _soLingerTime;
	}

	/**
	 * @param soLingerTime
	 *            The soLingerTime to set or -1 to disable.
	 */
	public void setSoLingerTime(int soLingerTime) {
		_soLingerTime = soLingerTime;
	}

	protected void doStart() throws Exception {
		_connectionSet = new HashSet<Connection>();
		if (_originHandler == null)
			throw new IllegalStateException("No server");

		// open listener port
		open();

		if (_threadPool == null)
			_threadPool = _originHandler.getThreadPool();
		if (_threadPool != _originHandler.getThreadPool() && (_threadPool instanceof LifeCycle))
			((LifeCycle) _threadPool).start();
	}

	protected void doStop() throws Exception {
		Log.info(String.format("Stopped %s", getClass().getName()));
		try {
			close();
		} catch (IOException e) {
			Log.warn(e);
		}

		if (_threadPool == _originHandler.getThreadPool())
			_threadPool = null;
		else if (_threadPool instanceof LifeCycle)
			((LifeCycle) _threadPool).stop();

		Thread[] dispatchedThreads = null;
		synchronized (this) {
			dispatchedThreads = _dispatchedThreads;
			_dispatchedThreads = null;
		}
		if (dispatchedThreads != null) {
			for (int i = 0; i < dispatchedThreads.length; i++) {
				Thread thread = dispatchedThreads[i];
				if (thread != null)
					thread.interrupt();
			}
		}

		Set<Connection> set = null;

		synchronized (_connectionSet) {
			set = new HashSet<Connection>(_connectionSet);
		}

		Iterator<Connection> iter = set.iterator();
		while (iter.hasNext()) {
			Connection connection = iter.next();
			connection.close();
		}
	}

	public void join() throws InterruptedException {
		Thread[] threads = _dispatchedThreads;
		if (threads != null)
			for (int i = 0; i < threads.length; i++)
				if (threads[i] != null)
					threads[i].join();
	}

	protected void configure(Socket socket) throws IOException {
		try {
			socket.setReuseAddress(true);
		} catch (Exception e) {
			Log.ignore(e);
		}

		try {
			socket.setTcpNoDelay(true);
		} catch (Exception e) {
			Log.ignore(e);
		}
		if (_maxIdleTime >= 0) {
			try {
				socket.setSoTimeout(_maxIdleTime);
			} catch (Exception e) {
				Log.ignore(e);
			}
		}
		
		try {
			if (_soLingerTime >= 1000) {
				socket.setSoLinger(true, _soLingerTime / 1000);
			} else {
				socket.setSoLinger(false, 0);
			}
		} catch (Exception e) {
			Log.ignore(e);
		}
	}

	public void customize(EndPoint endpoint, PacketRequest request) throws IOException {
		Connection connection = (Connection) endpoint;
		if (connection._sotimeout != _maxIdleTime) {
			connection._sotimeout = _maxIdleTime;
			((Socket) endpoint.getTransport()).setSoTimeout(_maxIdleTime);
		}
	}

	public void persist(EndPoint endpoint) throws IOException {
	}

	public void stopAccept(int acceptorID) throws Exception {
	}

	public boolean getResolveNames() {
		return _useDNS;
	}

	public void setResolveNames(boolean resolve) {
		_useDNS = resolve;
	}

	public String toString() {
		String name = this.getClass().getName();
		int dot = name.lastIndexOf('.');
		if (dot > 0)
			name = name.substring(dot + 1);

		return name + "@" + (getHost() == null ? "0.0.0.0" : getHost()) + ":"
				+ (getLocalPort() <= 0 ? getPort() : getLocalPort());
	}

	public String getName() {
		if (_name == null)
			_name = (getHost() == null ? "0.0.0.0" : getHost()) + ":"
					+ (getLocalPort() <= 0 ? getPort() : getLocalPort());
		return _name;
	}

	public void setName(String name) {
		_name = name;
	}

	public String getProtocol() {
		return _protocol;
	}

	public void setProtocol(String p) {
		this._protocol = p;
	}

	/**
	 * @return Get the number of requests handled by this context since last
	 *         call of statsReset(). If setStatsOn(false) then this is
	 *         undefined.
	 */
	public int getRequests() {
		return _requests;
	}

	/**
	 * @return Returns the connectionsDurationMin.
	 */
	public long getConnectionsDurationMin() {
		return _connectionsDurationMin;
	}

	/**
	 * @return Returns the connectionsDurationTotal.
	 */
	public long getConnectionsDurationTotal() {
		return _connectionsDurationTotal;
	}

	/**
	 * @return Returns the connectionsOpenMin.
	 */
	public int getConnectionsOpenMin() {
		return _connectionsOpenMin;
	}

	/**
	 * @return Returns the connectionsRequestsMin.
	 */
	public int getConnectionsRequestsMin() {
		return _connectionsRequestsMin;
	}

	/**
	 * @return Number of connections accepted by the server since statsReset()
	 *         called. Undefined if setStatsOn(false).
	 */
	public int getConnections() {
		return _connections;
	}

	/**
	 * @return Number of connections currently open that were opened since
	 *         statsReset() called. Undefined if setStatsOn(false).
	 */
	public int getConnectionsOpen() {
		return _connectionsOpen;
	}

	/**
	 * @return Maximum number of connections opened simultaneously since
	 *         statsReset() called. Undefined if setStatsOn(false).
	 */
	public int getConnectionsOpenMax() {
		return _connectionsOpenMax;
	}

	/**
	 * @return Average duration in milliseconds of open connections since
	 *         statsReset() called. Undefined if setStatsOn(false).
	 */
	public long getConnectionsDurationAve() {
		return _connections == 0 ? 0 : (_connectionsDurationTotal / _connections);
	}

	/**
	 * @return Maximum duration in milliseconds of an open connection since
	 *         statsReset() called. Undefined if setStatsOn(false).
	 */
	public long getConnectionsDurationMax() {
		return _connectionsDurationMax;
	}

	/**
	 * @return Average number of requests per connection since statsReset()
	 *         called. Undefined if setStatsOn(false).
	 */
	public int getConnectionsRequestsAve() {
		return _connections == 0 ? 0 : (_requests / _connections);
	}

	/**
	 * @return Maximum number of requests per connection since statsReset()
	 *         called. Undefined if setStatsOn(false).
	 */
	public int getConnectionsRequestsMax() {
		return _connectionsRequestsMax;
	}

	/**
	 * Reset statistics.
	 */
	public void statsReset() {
		_statsStartedAt = _statsStartedAt == -1 ? -1 : System.currentTimeMillis();

		_connections = 0;

		_connectionsOpenMin = _connectionsOpen;
		_connectionsOpenMax = _connectionsOpen;
		_connectionsOpen = 0;

		_connectionsDurationMin = 0;
		_connectionsDurationMax = 0;
		_connectionsDurationTotal = 0;

		_requests = 0;

		_connectionsRequestsMin = 0;
		_connectionsRequestsMax = 0;
	}

	public void setStatsOn(boolean on) {
		if (on && _statsStartedAt != -1)
			return;
		if (Log.isDebugEnabled())
			Log.debug("Statistics on = " + on + " for " + this);
		statsReset();
		_statsStartedAt = on ? System.currentTimeMillis() : -1;
	}

	/**
	 * @return True if statistics collection is turned on.
	 */
	public boolean getStatsOn() {
		return _statsStartedAt != -1;
	}

	/**
	 * @return Timestamp stats were started at.
	 */
	public long getStatsOnMs() {
		return (_statsStartedAt != -1) ? (System.currentTimeMillis() - _statsStartedAt) : 0;
	}

	protected void connectionOpened(PacketConnection connection) {
		if (!isIgnoredConnectionException()) {
			Log.info(String.format("%s已连接", connection.toString()));
		}

		if (_statsStartedAt == -1)
			return;
		synchronized (_statsLock) {
			_connectionsOpen++;
			if (_connectionsOpen > _connectionsOpenMax)
				_connectionsOpenMax = _connectionsOpen;
		}
	}

	protected void connectionException(PacketConnection connection, Throwable e) {
		boolean log_exception = true;
		if (e instanceof EofException && isIgnoredEofException()) {
			log_exception = false;
		} else if (isIgnoredConnectionException()) {
			log_exception = false;
		}
		if (log_exception) {
			Log.error(String.format("连接上下文(%s)处理出错，错误内容：%s", connection.toString(), e.getMessage()), e);
		}
	}

	protected void connectionClosed(PacketConnection connection) {
		if (!isIgnoredConnectionException()) {
			Log.info(String.format("%s已断开", connection.toString()));
		}

		if (_statsStartedAt >= 0) {
			long duration = System.currentTimeMillis() - connection.getTimeStamp();
			int requests = connection.getRequests();
			synchronized (_statsLock) {
				_requests += requests;
				_connections++;
				_connectionsOpen--;
				_connectionsDurationTotal += duration;
				if (_connectionsOpen < 0)
					_connectionsOpen = 0;
				if (_connectionsOpen < _connectionsOpenMin)
					_connectionsOpenMin = _connectionsOpen;
				if (_connectionsDurationMin == 0 || duration < _connectionsDurationMin)
					_connectionsDurationMin = duration;
				if (duration > _connectionsDurationMax)
					_connectionsDurationMax = duration;
				if (_connectionsRequestsMin == 0 || requests < _connectionsRequestsMin)
					_connectionsRequestsMin = requests;
				if (requests > _connectionsRequestsMax)
					_connectionsRequestsMax = requests;
			}
		}

		connection.destroy();
	}

	/**
	 * @return the acceptorPriority
	 */
	public int getAcceptorPriorityOffset() {
		return _acceptorPriorityOffset;
	}

	/**
	 * Set the priority offset of the acceptor threads. The priority is adjusted
	 * by this amount (default 0) to either favour the acceptance of new threads
	 * and newly active connections or to favour the handling of already
	 * dispatched connections.
	 * 
	 * @param offset
	 *            the amount to alter the priority of the acceptor threads.
	 */
	public void setAcceptorPriorityOffset(int offset) {
		_acceptorPriorityOffset = offset;
	}

	public boolean isConfidential(PacketRequest request) {
		return false;
	}

	/**
	 * @return True if the the server socket will be opened in SO_REUSEADDR
	 *         mode.
	 */
	public boolean getReuseAddress() {
		return _reuseAddress;
	}

	/**
	 * @param reuseAddress
	 *            True if the the server socket will be opened in SO_REUSEADDR
	 *            mode.
	 */
	public void setReuseAddress(boolean reuseAddress) {
		_reuseAddress = reuseAddress;
	}

	protected PacketConnection newPacketConnection(EndPoint endpoint) {
		return new PacketConnection(this.getOrigin(), this, this, endpoint, new PacketGenerator(this, endpoint,
				getResponseBufferSize()));
	}

	protected class Connection extends SocketEndPoint implements Runnable {
		boolean _dispatched = false;
		PacketConnection _connection;
		int _sotimeout;
		protected Socket _socket;

		public Connection(Socket socket) throws IOException {
			super(socket);
			_connection = newPacketConnection(this);
			_sotimeout = socket.getSoTimeout();
			_socket = socket;
		}

		public int getSoTimeout() {
			return this._sotimeout;
		}

		public void setSoTimeout(int timeout) {
			this._sotimeout = timeout;
		}

		public PacketConnection getConnection() {
			return this._connection;
		}

		public Socket getSocket() {
			return this._socket;
		}

		public boolean isDispatched() {
			return this._dispatched;
		}

		public void dispatch() throws InterruptedException, IOException {
			if (getThreadPool() == null || !getThreadPool().dispatch(this)) {
				close();
			}
		}

		public int fill(Buffer buffer) throws IOException {
			int l = super.fill(buffer);
			if (l < 0)
				close();
			return l;
		}

		public void run() {
			String thread_name = null;
			try {
				if (Log.isDebugEnabled()) {
					thread_name = Thread.currentThread().getName();
					Thread.currentThread().setName(
							thread_name + "@" + _connection._endp.getRemoteHost() + ":"
									+ _connection._endp.getRemotePort());
				}
				connectionOpened(_connection);
				synchronized (_connectionSet) {
					_connectionSet.add(this);
				}
				while (isStarted() && !isClosed()) {
					if (_connection.isIdle()) {
						if (getOrigin().getThreadPool().isLowOnThreads()) {
							int lrmit = getLowResourceMaxIdleTime();
							if (lrmit >= 0 && _sotimeout != lrmit) {
								_sotimeout = lrmit;
								_socket.setSoTimeout(_sotimeout);
							}
						}
					}
					_connection.handle();
					Thread.yield();
				}
			} catch (EofException e) {
				try {
					connectionException(_connection, e);
				} catch (Throwable e1) {
					Log.ignore(e1);
				}
				try {
					close();
				} catch (IOException e2) {
					Log.ignore(e2);
				}
			} catch (IOException e) {
				try {
					connectionException(_connection, e);
				} catch (Throwable e1) {
					Log.ignore(e1);
				}
				try {
					close();
				} catch (IOException e2) {
					Log.warn(e2);
				}
			} catch (Throwable e) {
				try {
					connectionException(_connection, e);
				} catch (Throwable e1) {
					Log.ignore(e1);
				}
				try {
					close();
				} catch (IOException e2) {
					Log.ignore(e2);
				}
			} finally {
				connectionClosed(_connection);
				synchronized (_connectionSet) {
					_connectionSet.remove(this);
				}
				if (thread_name != null) {
					Thread.currentThread().setName(thread_name);
				}
			}
		}
	}

}
