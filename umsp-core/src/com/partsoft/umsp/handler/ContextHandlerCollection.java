package com.partsoft.umsp.handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.partsoft.umsp.Handler;
import com.partsoft.umsp.HandlerContainer;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.packet.PacketConnection;
import com.partsoft.utils.ListUtils;

@SuppressWarnings("unchecked")
public class ContextHandlerCollection extends HandlerCollection {

	private Map<String, Object> _contextMap;

	private Class<?> _contextClass = PacketContextHandler.class;

	public void mapContexts() {
		Map<String, Object> contextMap = new HashMap<String, Object>();
		Handler[] branches = getHandlers();

		for (int b = 0; branches != null && b < branches.length; b++) {
			Handler[] handlers = null;

			if (branches[b] instanceof PacketContextHandler) {
				handlers = new Handler[] { branches[b] };
			} else if (branches[b] instanceof HandlerContainer) {
				handlers = ((HandlerContainer) branches[b]).getChildHandlersByClass(PacketContextHandler.class);
			} else
				continue;

			for (int i = 0; i < handlers.length; i++) {
				PacketContextHandler handler = (PacketContextHandler) handlers[i];
				String context_protocol = handler.getContextProtocol();

				Object contexts = contextMap.get(context_protocol);

				String[] vhosts = handler.getVirtualHosts();

				if (vhosts != null && vhosts.length > 0) {
					Map<String, Object> hosts;
					if (contexts instanceof Map)
						hosts = (Map<String, Object>) contexts;
					else {
						hosts = new HashMap<String, Object>();
						hosts.put("*", contexts);
						contextMap.put(context_protocol, hosts);
					}

					for (int j = 0; j < vhosts.length; j++) {
						String vhost = vhosts[j];
						contexts = hosts.get(vhost);
						contexts = ListUtils.add(contexts, branches[b]);
						hosts.put(vhost, contexts);
					}
				} else if (contexts instanceof Map) {
					Map<String, Object> hosts = (Map<String, Object>) contexts;
					contexts = hosts.get("*");
					contexts = ListUtils.add(contexts, branches[b]);
					hosts.put("*", contexts);
				} else {
					contexts = ListUtils.add(contexts, branches[b]);
					contextMap.put(context_protocol, contexts);
				}
			}
		}
		_contextMap = contextMap;
	}

	public void setHandlers(Handler[] handlers) {
		_contextMap = null;
		super.setHandlers(handlers);
		if (isStarted())
			mapContexts();
	}

	protected void doStart() throws Exception {
		mapContexts();
		super.doStart();
	}

	
	public void handle(String protocol, Request request, Response response, int dispatch) throws IOException {
		Handler[] handlers = getHandlers();
		if (handlers == null || handlers.length == 0)
			return;

		Request base_request = PacketConnection.getCurrentConnection().getRequest();

		// data structure which maps a request to a context
		// each match is called in turn until the request is handled
		// { context path =>
		// { virtual host => context }
		// }
		Map<String, Object> map = _contextMap;
		if (map != null && protocol != null) {
			// first, get all contexts matched by context path
			Object contexts = map.get(protocol);

			for (int i = 0; i < ListUtils.size(contexts); i++) {
				// then, match against the virtualhost of each context
				Object list = ListUtils.get(contexts, i);

				if (list instanceof Map) {
					Map<String, Object> hosts = (Map<String, Object>) list;
					String host = normalizeHostname(request.getOriginName());

					// explicitly-defined virtual hosts, most specific
					list = hosts.get(host);
					for (int j = 0; j < ListUtils.size(list); j++) {
						Handler handler = (Handler) ListUtils.get(list, j);
						handler.handle(protocol, request, response, dispatch);
						if (base_request.isHandled())
							return;
					}

					// wildcard for one level of names
					list = hosts.get("*." + host.substring(host.indexOf(".") + 1));
					for (int j = 0; j < ListUtils.size(list); j++) {
						Handler handler = (Handler) ListUtils.get(list, j);
						handler.handle(protocol, request, response, dispatch);
						if (base_request.isHandled())
							return;
					}

					// no virtualhosts defined for the context, least specific
					// will handle any request that does not match to a specific
					// virtual host above
					list = hosts.get("*");
					for (int j = 0; j < ListUtils.size(list); j++) {
						Handler handler = (Handler) ListUtils.get(list, j);
						handler.handle(protocol, request, response, dispatch);
						if (base_request.isHandled())
							return;
					}
				} else {
					for (int j = 0; j < ListUtils.size(list); j++) {
						Handler handler = (Handler) ListUtils.get(list, j);
						handler.handle(protocol, request, response, dispatch);
						if (base_request.isHandled())
							return;
					}
				}
			}
		} else {
			// This may not work in all circumstances... but then I think it
			// should never be called
			for (int i = 0; i < handlers.length; i++) {
				handlers[i].handle(protocol, request, response, dispatch);
				if (base_request.isHandled())
					return;
			}
		}
	}

//	public ContextHandler addContext(String protocol, String resourceBase) {
//		try {
//			ContextHandler context = (ContextHandler) _contextClass.newInstance();
//			addHandler(context);
//			return context;
//		} catch (Exception e) {
//			Log.debug(e);
//			throw new Error(e);
//		}
//	}

	public Class<?> getContextClass() {
		return _contextClass;
	}

	public void setContextClass(Class<?> contextClass) {
		if (contextClass == null || !(PacketContextHandler.class.isAssignableFrom(contextClass)))
			throw new IllegalArgumentException();
		_contextClass = contextClass;
	}

	private String normalizeHostname(String host) {
		if (host == null)
			return null;
		if (host.endsWith("."))
			return host.substring(0, host.length() - 1);
		return host;
	}

}
