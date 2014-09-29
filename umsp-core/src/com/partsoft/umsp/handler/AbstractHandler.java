package com.partsoft.umsp.handler;

import com.partsoft.umsp.AbstractLifeCycle;
import com.partsoft.umsp.Handler;
import com.partsoft.umsp.OriginHandler;
import com.partsoft.umsp.log.Log;

public abstract class AbstractHandler extends AbstractLifeCycle implements Handler {
	protected String _string;
	private OriginHandler _origin;
	

	public AbstractHandler() {
	}

	protected void doStart() throws Exception {
		if (Log.isDebugEnabled()) Log.debug(String.format("starting %s", getClass().getName()));
	}

	protected void doStop() throws Exception {
		if (Log.isDebugEnabled()) Log.debug(String.format("stopping %s", getClass().getName()));
	}

	public String toString() {
		if (_string == null) {
			_string = super.toString();
			_string = _string.substring(_string.lastIndexOf('.') + 1);
		}
		return _string;
	}

	public void setOrigin(OriginHandler server) {
		OriginHandler old_server = _origin;
		if (old_server != null && old_server != server)
			old_server.getContainer().removeBean(this);
		_origin = server;
		if (_origin != null && _origin != old_server)
			_origin.getContainer().addBean(this);
	}

	public OriginHandler getOrigin() {
		return _origin;
	}

	public void destroy() {
		if (!isStopped())
			throw new IllegalStateException("!STOPPED");
		if (_origin != null)
			_origin.getContainer().removeBean(this);
	}

}
