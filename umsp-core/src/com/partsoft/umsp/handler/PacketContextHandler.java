package com.partsoft.umsp.handler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.partsoft.umsp.Attributes;
import com.partsoft.umsp.AttributesMap;
import com.partsoft.umsp.Context;
import com.partsoft.umsp.ContextAttributeEvent;
import com.partsoft.umsp.ContextAttributeListener;
import com.partsoft.umsp.ContextEvent;
import com.partsoft.umsp.ContextListener;
import com.partsoft.umsp.Handler;
import com.partsoft.umsp.OriginHandler;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.RequestAttributeListener;
import com.partsoft.umsp.RequestListener;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.log.Logger;
import com.partsoft.umsp.packet.PacketConnection;
import com.partsoft.umsp.packet.PacketException;
import com.partsoft.umsp.packet.PacketRequest;
import com.partsoft.umsp.packet.PacketResponse;
import com.partsoft.utils.ListUtils;
import com.partsoft.utils.StringUtils;

public class PacketContextHandler extends HandlerWrapper implements Attributes, OriginHandler.ShutdownGraceful {

	public static final String MANAGED_ATTRIBUTES = "com.partsoft.umsp.ManagedAttributes";

	public static final String UNKNOWN_CONTEXT_PROTOCOL = "UNKNOWN";

	private static ThreadLocal<Context> __context = new ThreadLocal<Context>();

	private boolean _shutdown;

	private AttributesMap _attributes;

	private AttributesMap _contextAttributes;

	private Map<String, Object> _initParams;

	private Set<String> _managedAttributes;

	private ClassLoader _classLoader;

	protected SContext _context;

	private ErrorHandler _errorHandler;

	private EventListener[] _eventListeners;

	private Object _contextListeners;
	private Object _contextAttributeListeners;
	private Object _requestListeners;
	private Object _requestAttributeListeners;

	private Logger _logger;

	private String _displayName;

	private String _contextProtocol = UNKNOWN_CONTEXT_PROTOCOL;

	private String[] _vhosts;

	private Set<String> _connectors;
	
	public PacketContextHandler() {
		_context = new SContext();
		_attributes = new AttributesMap();
		_initParams = new HashMap<String, Object>();
	}
	
	public PacketContextHandler(Handler handler) {
		this();
		setHandler(handler);
	}
	
	public PacketContextHandler(String protocol, Handler handler) {
		this();
		setContextProtocol(protocol);
		setHandler(handler);
	}

	public static Context getCurrentContext() {
		Context context = __context.get();
		return context;
	}

	public String[] getVirtualHosts() {
		return _vhosts;
	}

	public void setVirtualHosts(String[] vhosts) {
		if (vhosts == null) {
			_vhosts = vhosts;
		} else {
			_vhosts = new String[vhosts.length];
			for (int i = 0; i < vhosts.length; i++)
				_vhosts[i] = normalizeHostname(vhosts[i]);
		}
	}

	private String normalizeHostname(String host) {
		if (host == null)
			return null;

		if (host.endsWith("."))
			return host.substring(0, host.length() - 1);

		return host;
	}

	public void setShutdown(boolean b) {
		_shutdown = b;
	}

	public void removeAttribute(String name) {
		setManagedAttribute(name, null);
		_attributes.removeAttribute(name);
	}

	public void setAttribute(String name, Object attribute) {
		setManagedAttribute(name, attribute);
		_attributes.setAttribute(name, attribute);
	}

	public void setAttributes(Attributes attributes) {
		if (attributes instanceof AttributesMap) {
			_attributes = (AttributesMap) attributes;
			Enumeration<String> e = _attributes.getAttributeNames();
			while (e.hasMoreElements()) {
				String name = (String) e.nextElement();
				setManagedAttribute(name, attributes.getAttribute(name));
			}
		} else {
			_attributes = new AttributesMap();
			Enumeration<String> e = attributes.getAttributeNames();
			while (e.hasMoreElements()) {
				String name = (String) e.nextElement();
				Object value = attributes.getAttribute(name);
				setManagedAttribute(name, value);
				_attributes.setAttribute(name, value);
			}
		}
	}

	public Object getAttribute(String name) {
		return _attributes.getAttribute(name);
	}

	public Enumeration<String> getAttributeNames() {
		return AttributesMap.getAttributeNamesCopy(_attributes);
	}

	public void clearAttributes() {
		Enumeration<String> e = _attributes.getAttributeNames();
		while (e.hasMoreElements()) {
			String name = (String) e.nextElement();
			setManagedAttribute(name, null);
		}
		_attributes.clearAttributes();
	}

	public ClassLoader getClassLoader() {
		return _classLoader;
	}

	public void setClassLoader(ClassLoader classLoader) {
		_classLoader = classLoader;
	}

	private void setManagedAttribute(String name, Object value) {
		if (_managedAttributes != null && _managedAttributes.contains(name)) {
			Object o = _context.getAttribute(name);
			if (o != null)
				getOrigin().getContainer().removeBean(o);
			if (value != null)
				getOrigin().getContainer().addBean(value);
		}
	}

	public EventListener[] getEventListeners() {
		return _eventListeners;
	}

	public void setEventListeners(EventListener[] eventListeners) {
		_contextListeners = null;
		_contextAttributeListeners = null;
		_requestListeners = null;
		_requestAttributeListeners = null;

		_eventListeners = eventListeners;

		for (int i = 0; eventListeners != null && i < eventListeners.length; i++) {
			EventListener listener = _eventListeners[i];

			if (listener instanceof ContextListener)
				_contextListeners = ListUtils.add(_contextListeners, listener);

			if (listener instanceof ContextAttributeListener)
				_contextAttributeListeners = ListUtils.add(_contextAttributeListeners, listener);

			if (listener instanceof RequestListener)
				_requestListeners = ListUtils.add(_requestListeners, listener);

			if (listener instanceof RequestAttributeListener)
				_requestAttributeListeners = ListUtils.add(_requestAttributeListeners, listener);
		}
	}

	public void addEventListener(EventListener listener) {
		setEventListeners((EventListener[]) ListUtils.addToArray(getEventListeners(), listener, EventListener.class));
	}
	
	public void removeEventListener(EventListener listener) {
		setEventListeners((EventListener[]) ListUtils.removeFromArray(getEventListeners(), listener));
	}

	public boolean isShutdown() {
		return !_shutdown;
	}

	public String getContextProtocol() {
		return _contextProtocol;
	}

	public void setContextProtocol(String contextProtocol) {
		if (contextProtocol != null && contextProtocol.length() > 1
				&& contextProtocol.equalsIgnoreCase(UNKNOWN_CONTEXT_PROTOCOL))
			throw new IllegalArgumentException("ends with /");
		_contextProtocol = contextProtocol;

		if (getOrigin() != null && (getOrigin().isStarting() || getOrigin().isStarted())) {
			Handler[] contextCollections = getOrigin().getChildHandlersByClass(ContextHandlerCollection.class);
			for (int h = 0; contextCollections != null && h < contextCollections.length; h++)
				((ContextHandlerCollection) contextCollections[h]).mapContexts();
		}
	}

	public String getDisplayName() {
		return _displayName;
	}

	public String getInitParameter(String name) {
		return _initParams.get(name).toString();
	}

	public Enumeration<String> getInitParameterNames() {
		return Collections.enumeration(_initParams.keySet());
	}

	public Map<String, Object> getInitParams() {
		return _initParams;
	}

	public String[] getConnectorNames() {
		if (_connectors == null || _connectors.size() == 0)
			return null;
		return (String[]) _connectors.toArray(new String[_connectors.size()]);
	}

	public void setConnectorNames(String[] connectors) {
		if (connectors == null || connectors.length == 0)
			_connectors = null;
		else
			_connectors = new HashSet<String>(Arrays.asList(connectors));
	}

	@Override
	public void setOrigin(OriginHandler server) {
		if (_errorHandler != null) {
			OriginHandler old_server = getOrigin();
			if (old_server != null && old_server != server)
				old_server.getContainer().update(this, _errorHandler, null, "error", true);
			super.setOrigin(server);
			if (server != null && server != old_server)
				server.getContainer().update(this, null, _errorHandler, "error", true);
			_errorHandler.setOrigin(server);
		} else
			super.setOrigin(server);
	}

	@Override
	protected void doStart() throws Exception {
		_logger = Log.getLogger(getDisplayName() == null ? getContextProtocol() : getDisplayName());
		ClassLoader old_classloader = null;
		Thread current_thread = null;
		SContext old_context = null;

		_contextAttributes = new AttributesMap();

		try {

			// Set the classloader
			if (_classLoader != null) {
				current_thread = Thread.currentThread();
				old_classloader = current_thread.getContextClassLoader();
				current_thread.setContextClassLoader(_classLoader);
			}

			old_context = (SContext) __context.get();
			__context.set(_context);

			if (_errorHandler == null)
				setErrorHandler(new ErrorHandler());

			startContext();

		} finally {
			__context.set(old_context);

			// reset the classloader
			if (_classLoader != null) {
				current_thread.setContextClassLoader(old_classloader);
			}
		}
	}

	protected void startContext() throws Exception {
		super.doStart();

		if (_errorHandler != null)
			_errorHandler.start();

		// Context listeners
		if (_contextListeners != null) {
			ContextEvent event = new ContextEvent(_context);
			for (int i = 0; i < ListUtils.size(_contextListeners); i++) {
				((ContextListener) ListUtils.get(_contextListeners, i)).contextInitialized(event);
			}
		}

		String managedAttributes = (String) _initParams.get(MANAGED_ATTRIBUTES);
		if (managedAttributes != null) {
			_managedAttributes = new HashSet<String>();
			StringUtils.QuotedStringTokenizer tok = new StringUtils.QuotedStringTokenizer(managedAttributes, ",");
			while (tok.hasMoreTokens())
				_managedAttributes.add(tok.nextToken().trim());

			Enumeration<String> e = _context.getAttributeNames();
			while (e.hasMoreElements()) {
				String name = (String) e.nextElement();
				Object value = _context.getAttribute(name);
				setManagedAttribute(name, value);
			}
		}
	}

	protected void doStop() throws Exception {
		ClassLoader old_classloader = null;
		Thread current_thread = null;

		SContext old_context = (SContext) __context.get();
		__context.set(_context);
		try {
			// Set the classloader
			if (_classLoader != null) {
				current_thread = Thread.currentThread();
				old_classloader = current_thread.getContextClassLoader();
				current_thread.setContextClassLoader(_classLoader);
			}

			super.doStop();

			// Context listeners
			if (_contextListeners != null) {
				ContextEvent event = new ContextEvent(_context);
				for (int i = ListUtils.size(_contextListeners); i-- > 0;) {
					((ContextListener) ListUtils.get(_contextListeners, i)).contextDestroyed(event);
				}
			}

			if (_errorHandler != null)
				_errorHandler.stop();

			Enumeration<String> e = _context.getAttributeNames();
			while (e.hasMoreElements()) {
				String name = (String) e.nextElement();
				setManagedAttribute(name, null);
			}
		} finally {
			__context.set(old_context);
			// reset the classloader
			if (_classLoader != null)
				current_thread.setContextClassLoader(old_classloader);
		}

		if (_contextAttributes != null)
			_contextAttributes.clearAttributes();
		_contextAttributes = null;
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		if (errorHandler != null)
			errorHandler.setOrigin(getOrigin());
		if (getOrigin() != null)
			getOrigin().getContainer().update(this, _errorHandler, errorHandler, "errorHandler", true);
		_errorHandler = errorHandler;
	}

	@Override
	public void handle(String protocol, Request request, Response response, int dispatch) throws IOException {
		boolean new_context = false;
		SContext old_context = null;
		ClassLoader old_classloader = null;
		Thread current_thread = null;

		PacketRequest base_request = (request instanceof PacketRequest) ? (PacketRequest) request : PacketConnection
				.getCurrentConnection().getRequest();

		PacketResponse base_response = (request instanceof PacketResponse) ? (PacketResponse) response
				: PacketConnection.getCurrentConnection().getResponse();

		if (!isStarted() || _shutdown || (dispatch == REQUEST && base_request.isHandled()))
			return;

		old_context = (SContext) base_request.getContext();

		// Are we already in this context?
		if (old_context != _context) {
			new_context = true;

			// Check the vhosts
			if (_vhosts != null && _vhosts.length > 0) {
				String vhost = normalizeHostname(request.getOriginName());

				boolean match = false;

				// TODO non-linear lookup
				for (int i = 0; !match && i < _vhosts.length; i++) {
					String contextVhost = _vhosts[i];
					if (contextVhost == null)
						continue;
					if (contextVhost.startsWith("*.")) {
						// wildcard only at the beginning, and only for one
						// additional subdomain level
						match = contextVhost.regionMatches(true, 2, vhost, vhost.indexOf(".") + 1,
								contextVhost.length() - 2);
					} else
						match = contextVhost.equalsIgnoreCase(vhost);
				}
				if (!match)
					return;
			}

			// Check the connector
			if (_connectors != null && _connectors.size() > 0) {
				String connector = PacketConnection.getCurrentConnection().getProtocol();
				if (connector == null || !_connectors.contains(connector))
					return;
			}

			if (!protocol.equals(_contextProtocol)) {
				// Not for this context!
				return;
			}
		}

		try {
			// Update the paths
			base_request.setContext(_context);

			if (new_context) {
				// Set the classloader
				if (_classLoader != null) {
					current_thread = Thread.currentThread();
					old_classloader = current_thread.getContextClassLoader();
					current_thread.setContextClassLoader(_classLoader);
				}

				// Handle the REALLY SILLY request events!
				base_request.setRequestListeners(_requestListeners);
				if (_requestAttributeListeners != null) {
					final int s = ListUtils.size(_requestAttributeListeners);
					for (int i = 0; i < s; i++)
						base_request.addEventListener(((EventListener) ListUtils.get(_requestAttributeListeners, i)));
				}
			}

			// Handle the request
			try {
				Handler handler = getHandler();
				if (handler != null)
					handler.handle(protocol, request, response, dispatch);
			} catch (PacketException e) {
				base_response.errorTerminated();
				throw e;
			} finally {
				// Handle more REALLY SILLY request events!
				if (new_context) {
					base_request.takeRequestListeners();
					if (_requestAttributeListeners != null) {
						for (int i = ListUtils.size(_requestAttributeListeners); i-- > 0;)
							base_request.removeEventListener(((EventListener) ListUtils.get(_requestAttributeListeners,
									i)));
					}
				}
			}
		} finally {
			if (old_context != _context) {
				// reset the classloader
				if (_classLoader != null) {
					current_thread.setContextClassLoader(old_classloader);
				}
				// reset the context and servlet path.
				base_request.setContext(old_context);
			}
		}
	}

	public class SContext implements Context {

		public PacketContextHandler getContextHandler() {
			return PacketContextHandler.this;
		}

		public int getMajorVersion() {
			return 0;
		}

		public int getMinorVersion() {
			return 1;
		}

		public String getProtocol() {
			return getContextProtocol();
		}

		public void log(String paramString, Throwable paramThrowable) {
			_logger.warn(paramString, paramThrowable);
		}

		public void log(String paramString) {
			_logger.info(paramString);
		}

		public String getServerInfo() {
			return getOrigin().getVersion();
		}

		public String getInitParameter(String paramString) {
			return getInitParameter(paramString);
		}

		public Enumeration<String> getInitParameterNames() {
			return getInitParameterNames();
		}

		public Object getAttribute(String paramString) {
			Object o = getContextHandler().getAttribute(paramString);
			if (o == null && _contextAttributes != null)
				o = _contextAttributes.getAttribute(paramString);
			return o;
		}

		public Enumeration<String> getAttributeNames() {
			HashSet<String> set = new HashSet<String>();
			if (_contextAttributes != null) {
				Enumeration<String> e = _contextAttributes.getAttributeNames();
				while (e.hasMoreElements())
					set.add(e.nextElement());
			}
			Enumeration<String> e = getContextHandler().getAttributeNames();
			while (e.hasMoreElements())
				set.add(e.nextElement());
			return Collections.enumeration(set);
		}

		public void setAttribute(String name, Object value) {
			if (_contextAttributes == null) {
				getContextHandler().setAttribute(name, value);
				return;
			}

			setManagedAttribute(name, value);
			Object old_value = _contextAttributes.getAttribute(name);

			if (value == null)
				_contextAttributes.removeAttribute(name);
			else
				_contextAttributes.setAttribute(name, value);

			if (_contextAttributeListeners != null) {
				ContextAttributeEvent event = new ContextAttributeEvent(_context, name, old_value == null ? value
						: old_value);

				for (int i = 0; i < ListUtils.size(_contextAttributeListeners); i++) {
					ContextAttributeListener l = (ContextAttributeListener) ListUtils
							.get(_contextAttributeListeners, i);

					if (old_value == null)
						l.attributeAdded(event);
					else if (value == null)
						l.attributeRemoved(event);
					else
						l.attributeReplaced(event);
				}
			}
		}

		public void removeAttribute(String name) {

			setManagedAttribute(name, null);

			if (_contextAttributes == null) {
				// Set it on the handler
				_attributes.removeAttribute(name);
				return;
			}

			Object old_value = _contextAttributes.getAttribute(name);
			_contextAttributes.removeAttribute(name);
			if (old_value != null) {
				if (_contextAttributeListeners != null) {
					ContextAttributeEvent event = new ContextAttributeEvent(_context, name, old_value);

					for (int i = 0; i < ListUtils.size(_contextAttributeListeners); i++)
						((ContextAttributeListener) ListUtils.get(_contextAttributeListeners, i))
								.attributeRemoved(event);
				}
			}
		}
	}
}
