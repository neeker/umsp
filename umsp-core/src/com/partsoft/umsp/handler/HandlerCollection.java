package com.partsoft.umsp.handler;

import java.io.IOException;

import com.partsoft.umsp.Handler;
import com.partsoft.umsp.MultiException;
import com.partsoft.umsp.OriginHandler;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.packet.PacketException;
import com.partsoft.utils.ListUtils;

public class HandlerCollection extends AbstractHandlerContainer {

	private Handler[] _handlers;

	public HandlerCollection() {
		super();
	}

	/**
	 * @return Returns the handlers.
	 */
	public Handler[] getHandlers() {
		return _handlers;
	}

	/**
	 * 
	 * @param handlers
	 *            The handlers to set.
	 */
	public void setHandlers(Handler[] handlers) {
		Handler[] old_handlers = _handlers == null ? null : (Handler[]) _handlers.clone();

		if (getOrigin() != null)
			getOrigin().getContainer().update(this, old_handlers, handlers, "handler");

		OriginHandler server = getOrigin();
		MultiException mex = new MultiException();
		for (int i = 0; handlers != null && i < handlers.length; i++) {
			if (handlers[i].getOrigin() != server)
				handlers[i].setOrigin(server);
		}

		// quasi atomic.... so don't go doing this under load on a SMP system.
		_handlers = handlers;

		for (int i = 0; old_handlers != null && i < old_handlers.length; i++) {
			if (old_handlers[i] != null) {
				try {
					if (old_handlers[i].isStarted())
						old_handlers[i].stop();
				} catch (Throwable e) {
					mex.add(e);
				}
			}
		}

		mex.ifExceptionThrowRuntime();
	}

	public void handle(String protocol, Request request, Response response, int disptach) throws IOException {
		if (_handlers != null && isStarted()) {
			MultiException mex = null;

			for (int i = 0; i < _handlers.length; i++) {
				try {
					_handlers[i].handle(protocol, request, response, disptach);
				} catch (IOException e) {
					throw e;
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					if (mex == null)
						mex = new MultiException();
					mex.add(e);
				}
			}
			if (mex != null) {
				if (mex.size() == 1)
					throw new PacketException(mex.getThrowable(0));
				else
					throw new PacketException(mex);
			}

		}
	}

	protected void doStart() throws Exception {
		MultiException mex = new MultiException();
		if (_handlers != null) {
			for (int i = 0; i < _handlers.length; i++)
				try {
					_handlers[i].start();
				} catch (Throwable e) {
					mex.add(e);
				}
		}
		super.doStart();
		mex.ifExceptionThrow();
	}

	protected void doStop() throws Exception {
		MultiException mex = new MultiException();
		try {
			super.doStop();
		} catch (Throwable e) {
			mex.add(e);
		}
		if (_handlers != null) {
			for (int i = _handlers.length; i-- > 0;)
				try {
					_handlers[i].stop();
				} catch (Throwable e) {
					mex.add(e);
				}
		}
		mex.ifExceptionThrow();
	}

	public void setOrigin(OriginHandler server) {
		OriginHandler old_server = getOrigin();

		super.setOrigin(server);

		Handler[] h = getHandlers();
		for (int i = 0; h != null && i < h.length; i++)
			h[i].setOrigin(server);

		if (server != null && server != old_server)
			server.getContainer().update(this, null, _handlers, "handler");

	}

	public void addHandler(Handler handler) {
		setHandlers((Handler[]) ListUtils.addToArray(getHandlers(), handler, Handler.class));
	}

	public void removeHandler(Handler handler) {
		Handler[] handlers = getHandlers();

		if (handlers != null && handlers.length > 0)
			setHandlers((Handler[]) ListUtils.removeFromArray(handlers, handler));
	}

	protected Object expandChildren(Object list, Class<?> byClass) {
		Handler[] handlers = getHandlers();
		for (int i = 0; handlers != null && i < handlers.length; i++)
			list = expandHandler(handlers[i], list, byClass);
		return list;
	}

}
