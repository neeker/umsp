package com.partsoft.umsp;

import com.partsoft.umsp.handler.AbstractOriginHandler;
import com.partsoft.umsp.packet.PacketClientConnector;
import com.partsoft.umsp.thread.QueuedThreadPool;
import com.partsoft.utils.Assert;
import com.partsoft.utils.StringUtils;

public final class Client extends AbstractOriginHandler implements Attributes, OriginHandler {

	/**
	 * @brief 是否自动重连
	 */
	protected boolean _autoReConnect = false;

	/**
	 * @brief 重连间隔时间，单位：毫秒，默认值(30秒)
	 */
	protected long _reConnectIntervalTime = 30000;

	/**
	 * 连接超时时间，单位：毫秒，默认值1秒
	 */
	protected int connectTimeout = 1000;
	
	/**
	 * 数据读取超时
	 */
	protected int maxIdleTime = 500;

	/**
	 * @brief 最大连接发起数
	 */
	protected int _maxConnection = 1;
	
	private boolean _ignoredEofException = true;

	private boolean _ignoredConnectionException = false;

	protected String _host;

	protected int _port;

	protected String _protocol;

	public Client() {
		setOrigin(this);
	}
	
	public void setIgnoredConnectionException(boolean _ignoredConnectionException) {
		this._ignoredConnectionException = _ignoredConnectionException;
	}
	
	public void setIgnoredEofException(boolean _ignoredEofException) {
		this._ignoredEofException = _ignoredEofException;
	}

	public Client(String host, int port) {
		this();
		setHost(host);
		setPort(port);
	}

	public Client(String protocol, String host, int port) {
		this(host, port);
		setProtocol(protocol);
	}

	public String getProtocol() {
		return _protocol;
	}

	public void setProtocol(String protocol) {
		this._protocol = protocol;
	}

	public String getHost() {
		return _host;
	}

	public void setHost(String host) {
		this._host = host;
	}

	public int getPort() {
		return _port;
	}

	public void setPort(int port) {
		this._port = port;
	}

	public int getMaxConnection() {
		return _maxConnection;
	}
	
	public void setMaxIdleTime(int maxIdleTime) {
		this.maxIdleTime = maxIdleTime;
	}
	
	public int getMaxIdleTime() {
		return maxIdleTime;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * @brief 重连间隔时间，单位：毫秒，默认值(3秒)
	 * @return
	 */
	public long getReConnectIntervalTime() {
		return _reConnectIntervalTime;
	}

	public void setReConnectIntervalTime(long reConnectIntervalTime) {
		this._reConnectIntervalTime = reConnectIntervalTime;
	}

	public void setMaxConnection(int maxConnection) {
		Assert.isTrue(maxConnection > 0);
		this._maxConnection = maxConnection;
	}

	/**
	 * 断开自动重连
	 * 
	 * @return
	 */
	public boolean isAutoReConnect() {
		return _autoReConnect;
	}

	public void setAutoReConnect(boolean autoReConnect) {
		this._autoReConnect = autoReConnect;
	}
	
	public int getConnectedCount() {
		int connectedCount = 0;
		for (Connector connector : getConnectors()) {
			if (connector instanceof PacketClientConnector) {
				connectedCount += ((PacketClientConnector) connector).getConnectedCount();
			}
		}
		return connectedCount;
	}
	
	public boolean isConnected() {
		int connectedCount = 0;
		for (Connector connector : getConnectors()) {
			if (connector instanceof PacketClientConnector) {
				connectedCount += ((PacketClientConnector) connector).getConnectedCount();
			}
		}
		return connectedCount > 0;
	}

	protected void updateConnectorParamers() {
		if (getConnectors() != null) {
			for (Connector connector : getConnectors()) {
				if (connector instanceof PacketClientConnector) {
					PacketClientConnector packet_connector = (PacketClientConnector) connector;
					if (isAutoReConnect() != packet_connector.isAutoReConnect()) {
						packet_connector.setAutoReConnect(_autoReConnect);
					}
					if (getMaxConnection() != packet_connector.getMaxConnection()) {
						packet_connector.setMaxConnection(getMaxConnection());
					}
					if (getReConnectIntervalTime() != packet_connector.getReConnectIntervalTime()) {
						packet_connector.setReConnectIntervalTime(getReConnectIntervalTime());
					}
					if (getProtocol() != null && packet_connector.getProtocol() == null
							&& getProtocol().equals(packet_connector.getProtocol())) {
						packet_connector.setProtocol(getProtocol());
					}
					if (getConnectTimeout() != packet_connector.getConnectTimeout()) {
						packet_connector.setConnectTimeout(getConnectTimeout());
					}
					if (getMaxIdleTime() != packet_connector.getMaxIdleTime()) {
						packet_connector.setMaxIdleTime(getMaxIdleTime());
					}
					if (getMaxIdleTime() != packet_connector.getLowResourceMaxIdleTime()) {
						packet_connector.setLowResourceMaxIdleTime(getMaxIdleTime());
					}
					
					packet_connector.setIgnoredConnectionException(this._ignoredConnectionException);
					packet_connector.setIgnoredEofException(this._ignoredEofException);
					
				}
			}
		}
	}
	
	@Override
	protected void doStart() throws Exception {
		if (getConnectors() == null) {
			setConnectors(new Connector[] { new PacketClientConnector(getProtocol(), getHost(), getPort()) });
		}
		updateConnectorParamers();
		super.doStart();
		if (this.getThreadPool() instanceof QueuedThreadPool) {
			((QueuedThreadPool) this.getThreadPool()).setMaxThreads(getMaxConnection() + 5);
			if (StringUtils.hasText(getProtocol())) {
				((QueuedThreadPool) this.getThreadPool()).setName(getProtocol());
			}
		}
	}

}
