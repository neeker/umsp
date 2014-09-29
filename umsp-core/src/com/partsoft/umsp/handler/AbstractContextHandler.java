package com.partsoft.umsp.handler;

import java.io.IOException;

import com.partsoft.umsp.Container;
import com.partsoft.umsp.Container.Relationship;
import com.partsoft.umsp.Context;
import com.partsoft.umsp.ContextEvent;
import com.partsoft.umsp.ContextListener;
import com.partsoft.umsp.OriginHandler;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.packet.PacketException;

public abstract class AbstractContextHandler extends AbstractHandler {

	private Context context;

	protected PacketContextHandler parent;

	private ContextListener contextListener = new ContextListener() {

		public void contextInitialized(ContextEvent contextEvent) {
			AbstractContextHandler.this.context = contextEvent.getContext();
			try {
				handleContextInitialized(contextEvent.getContext());
			} catch (Throwable e) {
				Log.warn(e);
			}
		}

		public void contextDestroyed(ContextEvent contextEvent) {
			try {
				handleContextDestroyed(AbstractContextHandler.this.context);
			} catch (Throwable e) {
				Log.warn(e);
			}
			AbstractContextHandler.this.context = null;
		}
	};

	private Container.Listener containerListener = new Container.Listener() {

		public void addBean(Object bean) {

		}

		public void removeBean(Object bean) {
			if (bean == AbstractContextHandler.this) {
				getOrigin().getContainer().removeEventListener(containerListener);
				if (AbstractContextHandler.this.parent != null) {
					AbstractContextHandler.this.parent.removeEventListener(contextListener);
					AbstractContextHandler.this.parent = null;
				}
			}
		}

		public void add(Relationship relationship) {
			if (relationship.getChild() == AbstractContextHandler.this) {
				if (relationship.getParent() instanceof PacketContextHandler) {
					AbstractContextHandler.this.parent = ((PacketContextHandler) relationship.getParent());
					AbstractContextHandler.this.parent.addEventListener(contextListener);
				} else {
					throw new IllegalStateException(String.format("%s must be handle in ContenxtHandler",
							AbstractContextHandler.this.getClass().getSimpleName()));
				}
			}
		}

		public void remove(Relationship relationship) {
			if (relationship.getChild() == AbstractContextHandler.this) {
				if (relationship.getParent() instanceof PacketContextHandler) {
					((PacketContextHandler) relationship.getParent()).removeEventListener(contextListener);
					AbstractContextHandler.this.parent = null;
				}
			}
		}

	};

	public Context getContext() {
		return context;
	}

	@Override
	public void setOrigin(OriginHandler server) {
		OriginHandler old_server = getOrigin();
		if (old_server != server && old_server != null) {
			server.getContainer().removeEventListener(containerListener);
		}
		super.setOrigin(server);
		if (server != null) {
			server.getContainer().addEventListener(containerListener);
		}
	}

	protected void handleContextInitialized(Context ctx) {

	}

	protected void handleContextDestroyed(Context ctx) {

	}

	protected abstract void handleRequest(Request request, Response response) throws IOException;

	protected void handleTimeout(Request request, Response response) throws IOException {
		throw new PacketException("请求超时");
	}
	
	/**
	 * 当连接上了之后的处理
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	protected void handleConnect(Request request, Response response) throws IOException {
		
	}
	
	/**
	 * 当连接断开之后的处理
	 * @param request
	 * @param response
	 */
	protected void handleDisConnect(Request request, Response response) {
		
	}

	public void handle(String protocol, Request request, Response response, int dispatch) throws IOException {
		switch (dispatch) {
		case CONNECT:
			handleConnect(request, response);
			break;
		case REQUEST:
			handleRequest(request, response);
			break;
		case TIMEOUT:
			handleTimeout(request, response);
			break;
		case DISCONNECT:
			handleDisConnect(request, response);
			break;
		default:
			throw new PacketException("error dispatch");
		}
	}

}
