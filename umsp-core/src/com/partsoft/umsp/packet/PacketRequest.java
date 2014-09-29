package com.partsoft.umsp.packet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;

import com.partsoft.umsp.Attributes;
import com.partsoft.umsp.AttributesMap;
import com.partsoft.umsp.EndPoint;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.RequestAttributeEvent;
import com.partsoft.umsp.RequestAttributeListener;
import com.partsoft.umsp.handler.PacketContextHandler;
import com.partsoft.utils.ListUtils;

public class PacketRequest implements Request {

	private boolean _handled = false;

	private Attributes _attributes;

	private Object _requestAttributeListeners;

	private Object _requestListeners;

	private EndPoint _endp;

	private boolean _dns = false;

	protected PacketConnection _connection;

	private PacketContextHandler.SContext _context;

	private String _originName;

	private long _requestTimestamp;

	public PacketRequest(PacketConnection connection) {
		this._connection = connection;
		this._endp = connection._endp;
		this._dns = _connection.getResolveNames();
		this._requestTimestamp = System.currentTimeMillis();
	}

	protected void recycle() {
		_handled = false;
	}

	protected void updateRequestTime(long timestamp) {
		_requestTimestamp = timestamp;
	}

	public InputStream getInputStream() throws IOException {
		return _connection.getInputStream();
	}

	public boolean isHandled() {
		return _handled;
	}

	public void setHandled(boolean h) {
		_handled = h;
	}

	public long getStartTimestamp() {
		return _connection.getConnectTimestamp();
	}

	public long getRequestTimestamp() {
		return _requestTimestamp;
	}

	public Object getAttribute(String paramString) {
		if (_attributes == null)
			return null;
		return _attributes.getAttribute(paramString);
	}

	@SuppressWarnings("unchecked")
	public Enumeration<String> getAttributeNames() {
		if (_attributes == null)
			return Collections.enumeration(Collections.EMPTY_LIST);
		return AttributesMap.getAttributeNamesCopy(_attributes);
	}

	public void setAttribute(String paramString, Object paramObject) {
		Object old_value = _attributes == null ? null : _attributes.getAttribute(paramString);
		if (_attributes == null)
			_attributes = new AttributesMap();
		_attributes.setAttribute(paramString, paramObject);

		if (_requestAttributeListeners != null) {
			final RequestAttributeEvent event = new RequestAttributeEvent(_context, this, paramString,
					old_value == null ? paramObject : old_value);
			final int size = ListUtils.size(_requestAttributeListeners);
			for (int i = 0; i < size; i++) {
				final EventListener listener = (RequestAttributeListener) ListUtils.get(_requestAttributeListeners, i);
				if (listener instanceof RequestAttributeListener) {
					final RequestAttributeListener l = (RequestAttributeListener) listener;

					if (old_value == null)
						l.attributeAdded(event);
					else if (paramObject == null)
						l.attributeRemoved(event);
					else
						l.attributeReplaced(event);
				}
			}
		}
	}

	public void removeAttribute(String name) {
		Object old_value = _attributes == null ? null : _attributes.getAttribute(name);
		if (_attributes != null)
			_attributes.removeAttribute(name);
		if (old_value != null) {
			if (_requestAttributeListeners != null) {
				final RequestAttributeEvent event = new RequestAttributeEvent(_context, this, name, old_value);
				final int size = ListUtils.size(_requestAttributeListeners);
				for (int i = 0; i < size; i++) {
					final EventListener listener = (RequestAttributeListener) ListUtils.get(_requestAttributeListeners,
							i);
					if (listener instanceof RequestAttributeListener) {
						final RequestAttributeListener l = (RequestAttributeListener) listener;
						((RequestAttributeListener) l).attributeRemoved(event);
					}
				}
			}
		}
	}

	@Deprecated
	public int getContentLength() {
		if (_connection == null || _connection.getParser() == null)
			return -1;
		return _connection.getParser().getContentLength();
	}

	@Deprecated
	public int getContentReaded() {
		if (_connection == null || _connection.getParser() == null)
			return -1;
		return _connection.getParser().getContentReaded();
	}

	public String getProtocol() {
		return _context != null ? _context.getProtocol() : _connection != null ? _connection._connector.getProtocol()
				: "UNKNOWN";
	}

	public String getOriginName() {
		if (_originName != null)
			return _originName;
		if (_connection == null) {
			_originName = _endp != null ? _endp.getLocalHost() + ":" + _endp.getLocalPort() : "0.0.0.0:0";
		} else {
			_originName = _connection.getConnector().getName();
		}
		return _originName;
	}

	public int getServerPort() {
		return _endp != null ? _endp.getLocalPort() : -1;
	}

	public String getRemoteAddr() {
		return _endp != null ? _endp.getRemoteAddr() : null;
	}

	public String getRemoteHost() {
		return _endp != null ? _endp.getRemoteHost() : "UNKNOWN";
	}

	public int getRemotePort() {
		return _endp != null ? _endp.getRemotePort() : -1;
	}

	public String getLocalName() {
		if (_dns)
			return _endp == null ? null : _endp.getLocalHost();
		return _endp == null ? null : _endp.getLocalAddr();

	}

	public String getLocalAddr() {
		return _endp == null ? null : _endp.getLocalAddr();
	}

	public int getLocalPort() {
		return _endp != null ? _endp.getLocalPort() : -1;
	}

	public PacketContextHandler.SContext getContext() {
		return _context;
	}

	public void setContext(PacketContextHandler.SContext _context) {
		this._context = _context;
	}

	public boolean isSecure() {
		return _connection.isConfidential(this);
	}

	public void setRequestListeners(Object requestListeners) {
		_requestListeners = requestListeners;
	}

	public Object takeRequestListeners() {
		final Object listeners = _requestListeners;
		_requestListeners = null;
		return listeners;
	}

	public PacketResponse getResponse() {
		return _connection.getResponse();
	}

	public void addEventListener(final EventListener listener) {
		if (listener instanceof RequestAttributeListener)
			_requestAttributeListeners = ListUtils.add(_requestAttributeListeners, listener);
	}

	public void removeEventListener(final EventListener listener) {
		_requestAttributeListeners = ListUtils.remove(_requestAttributeListeners, listener);
	}
	
	public int getRequests() {
		return _connection.getRequests();
	}
	
}
