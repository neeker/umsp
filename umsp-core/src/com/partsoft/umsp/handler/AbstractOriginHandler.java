package com.partsoft.umsp.handler;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.partsoft.umsp.Attributes;
import com.partsoft.umsp.AttributesMap;
import com.partsoft.umsp.Connector;
import com.partsoft.umsp.Container;
import com.partsoft.umsp.Handler;
import com.partsoft.umsp.LifeCycle;
import com.partsoft.umsp.MultiException;
import com.partsoft.umsp.OriginHandler;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.packet.PacketConnection;
import com.partsoft.umsp.thread.QueuedThreadPool;
import com.partsoft.umsp.thread.ThreadPool;
import com.partsoft.utils.ListUtils;

public abstract class AbstractOriginHandler extends HandlerWrapper implements OriginHandler, Attributes {

	public final static String UNKNOWN_TITLE = "umsp";

	public final static String SNAPSHOT_VERSION = "0.1-SNAPSHOT";

	public final static String UNKNOWN_VERSION = "0.1.1";

	protected static String _version = (((AbstractOriginHandler.class.getPackage() != null && AbstractOriginHandler.class
			.getPackage().getImplementationVersion() != null) ? AbstractOriginHandler.class.getPackage()
			.getImplementationVersion() : UNKNOWN_VERSION).trim()).toLowerCase();

	protected static String _title = (((AbstractOriginHandler.class.getPackage() != null && AbstractOriginHandler.class
			.getPackage().getImplementationTitle() != null) ? AbstractOriginHandler.class.getPackage()
			.getImplementationTitle() : UNKNOWN_TITLE).trim()).toLowerCase();

	protected Object _delay_lock = new Object();
	
	protected MultiException _delayMex;
	
	private static ShutdownHookThread hookThread = new ShutdownHookThread();

	protected Container _container = new Container();

	protected ThreadPool _threadPool;

	private Connector[] _connectors;

	protected int _graceful = 0;

	protected List<LifeCycle> _dependentLifeCycles = new ArrayList<LifeCycle>();

	private AttributesMap _attributes = new AttributesMap();

	public ThreadPool getThreadPool() {
		return _threadPool;
	}

	public void setThreadPool(ThreadPool threadPool) {
		_container.update(this, _threadPool, threadPool, "threadpool", true);
		_threadPool = threadPool;
	}

	public Container getContainer() {
		return _container;
	}

	public Connector[] getConnectors() {
		return _connectors;
	}

	public void addConnector(Connector connector) {
		setConnectors((Connector[]) ListUtils.addToArray(getConnectors(), connector, Connector.class));
	}

	public void removeConnector(Connector connector) {
		setConnectors((Connector[]) ListUtils.removeFromArray(getConnectors(), connector));
	}

	public void setConnectors(Connector[] connectors) {
		if (connectors != null) {
			for (int i = 0; i < connectors.length; i++)
				connectors[i].setOrigin(this);
		}
		_container.update(this, _connectors, connectors, "connector");
		_connectors = connectors;
	}

	public String getVersion() {
		return String.format("%s-%s-%s", _title, getClass().getSimpleName().toLowerCase(), _version);
	}

	public void removeAttribute(String name) {
		_attributes.removeAttribute(name);
	}

	public void setAttribute(String name, Object attribute) {
		_attributes.setAttribute(name, attribute);
	}

	public Object getAttribute(String name) {
		return _attributes.getAttribute(name);
	}

	public Enumeration<String> getAttributeNames() {
		return AttributesMap.getAttributeNamesCopy(_attributes);
	}

	public void clearAttributes() {
		_attributes.clearAttributes();
	}

	public void setGraceful(int graceful) {
		this._graceful = graceful;
	}

	public boolean isStopAtShutdown() {
		return hookThread.contains(this);
	}

	public void setStopAtShutdown(boolean stop) {
		if (stop)
			hookThread.add(this);
		else
			hookThread.remove(this);
	}

	protected void doStart() throws Exception {
		Log.info(String.format("%s-%s-%s", _title, getClass().getSimpleName().toLowerCase(), _version));
		_delayMex = null;
		MultiException mex = new MultiException();

		Iterator<LifeCycle> itor = _dependentLifeCycles.iterator();
		while (itor.hasNext()) {
			try {
				((LifeCycle) itor.next()).start();
			} catch (Throwable e) {
				mex.add(e);
			}
		}

		if (_threadPool == null) {
			QueuedThreadPool tp = new QueuedThreadPool();
			setThreadPool(tp);
		}

		try {
			if (_threadPool instanceof LifeCycle)
				((LifeCycle) _threadPool).start();
		} catch (Throwable e) {
			mex.add(e);
		}

		try {
			super.doStart();
		} catch (Throwable e) {
			Log.warn("Error starting handlers", e);
		}

		if (_connectors != null) {
			for (int i = 0; i < _connectors.length; i++) {
				try {
					_connectors[i].start();
				} catch (Throwable e) {
					mex.add(e);
				}
			}
		}

		mex.ifExceptionThrow();
	}

	protected void doStop() throws Exception {
		MultiException mex = new MultiException();
		if (_graceful > 0) {

			if (_connectors != null) {
				for (int i = _connectors.length; i-- > 0;) {
					Log.info(String.format("Graceful shutdown %s", _connectors[i].toString()));
					try {
						_connectors[i].close();
					} catch (Throwable e) {
						mex.add(e);
					}
				}
			}

			Handler[] contexts = getChildHandlersByClass(ShutdownGraceful.class);
			for (int c = 0; c < contexts.length; c++) {
				ShutdownGraceful context = (ShutdownGraceful) contexts[c];
				Log.info(String.format("Graceful shutdown %s", context.toString()));
				context.setShutdown(true);
			}
			Thread.sleep(_graceful);
		}

		if (_connectors != null) {
			for (int i = _connectors.length; i-- > 0;)
				try {
					_connectors[i].stop();
				} catch (Throwable e) {
					mex.add(e);
				}
		}

		try {
			super.doStop();
		} catch (Throwable e) {
			mex.add(e);
		}

		try {
			if (_threadPool instanceof LifeCycle)
				((LifeCycle) _threadPool).stop();
		} catch (Throwable e) {
			mex.add(e);
		}

		if (!_dependentLifeCycles.isEmpty()) {
			ListIterator<LifeCycle> itor = _dependentLifeCycles.listIterator(_dependentLifeCycles.size());
			while (itor.hasPrevious()) {
				try {
					((LifeCycle) itor.previous()).stop();
				} catch (Throwable e) {
					mex.add(e);
				}
			}
		}

		mex.ifExceptionThrow();
	}

	public void handle(PacketConnection connection, int dispatch) throws IOException {
		handle(connection.getProtocol(), connection.getRequest(), connection.getResponse(), dispatch);
	}

	public void join() throws InterruptedException {
		synchronized (_delay_lock) {
			if (isRunning()) {
				_delay_lock.wait();
			}
		}
		try {
			stop();
		} catch (Exception e) {
			pushDelayException(e);
		}
		getThreadPool().join();
		if (_delayMex != null) {
			_delayMex.ifExceptionThrowRuntime();
		}
	}

	public void delayStop() {
		synchronized (_delay_lock) {
			_delay_lock.notifyAll();
			Thread.yield();
		}
	}

	public void pushDelayException(Throwable e) {
		if (_delayMex == null) {
			_delayMex = new MultiException();
		}
		_delayMex.add(e);
	}

	private static class ShutdownHookThread extends Thread {

		private boolean hooked = false;

		private ArrayList<AbstractOriginHandler> servers = new ArrayList<AbstractOriginHandler>();

		private void createShutdownHook() {
			if (!Boolean.getBoolean("UMSP_NO_SHUTDOWN_HOOK") && !hooked) {
				try {
					Method shutdownHook = java.lang.Runtime.class.getMethod("addShutdownHook",
							new Class[] { java.lang.Thread.class });
					shutdownHook.invoke(Runtime.getRuntime(), new Object[] { this });
					this.hooked = true;
				} catch (Exception e) {
					if (Log.isDebugEnabled())
						Log.debug("No shutdown hook in JVM ", e);
				}
			}
		}

		/**
		 * Add Server to servers list.
		 */
		public boolean add(AbstractOriginHandler server) {
			createShutdownHook();
			return this.servers.add(server);
		}

		/**
		 * Contains Server in servers list?
		 */
		public boolean contains(AbstractOriginHandler server) {
			return this.servers.contains(server);
		}

		/**
		 * Remove Server from list.
		 */
		public boolean remove(AbstractOriginHandler server) {
			createShutdownHook();
			return this.servers.remove(server);
		}

		/**
		 * Stop all Servers in list.
		 */
		public void run() {
			setName("Shutdown");
			Log.info("Shutdown hook executing");
			Iterator<AbstractOriginHandler> it = servers.iterator();
			while (it.hasNext()) {
				AbstractOriginHandler svr = it.next();
				if (svr == null)
					continue;
				try {
					svr.stop();
				} catch (Exception e) {
					Log.warn(e);
				}
				Log.info("Shutdown hook complete");

				// Try to avoid JVM crash
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					Log.warn(e);
				}
			}
		}
	}

}
