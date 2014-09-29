package com.partsoft.umsp.io;

import com.partsoft.umsp.AbstractLifeCycle;

public abstract class AbstractBuffers extends AbstractLifeCycle implements BufferPools {

	private int _requestBufferSize = 2 * 1024;

	private int _responseBufferSize = 1024;

	final static private int __REQUEST = 0;
	final static private int __RESPONSE = 1;
	final static private int __OTHER = 2;
	final private int[] _pool = { 1, 1, 2 };

	private final ThreadLocal<ThreadBuffers> _buffers = new ThreadLocal<ThreadBuffers>() {
		protected ThreadBuffers initialValue() {
			return new ThreadBuffers(_pool[__REQUEST], _pool[__RESPONSE], _pool[__OTHER]);
		}
	};

	public AbstractBuffers() {
		super();
	}

	public Buffer getBuffer(final int size) {
		final int set = (size == _responseBufferSize) ? __RESPONSE : (size == _requestBufferSize) ? __REQUEST : __OTHER;

		final ThreadBuffers thread_buffers = (ThreadBuffers) _buffers.get();

		final Buffer[] buffers = thread_buffers._buffers[set];
		for (int i = 0; i < buffers.length; i++) {
			final Buffer b = buffers[i];
			if (b != null && b.capacity() == size) {
				buffers[i] = null;
				return b;
			}
		}

		return newBuffer(size);
	}

	public void returnBuffer(int size, Buffer buffer) {
		buffer.clear();
		if (buffer.isVolatile() || buffer.isImmutable())
			return;

		final int set = (size == _responseBufferSize) ? __RESPONSE : (size == _requestBufferSize) ? __REQUEST : __OTHER;

		final ThreadBuffers thread_buffers = (ThreadBuffers) _buffers.get();
		final Buffer[] buffers = thread_buffers._buffers[set];
		for (int i = 0; i < buffers.length; i++) {
			if (buffers[i] == null) {
				buffers[i] = buffer;
				return;
			}
		}

	}

	protected void doStart() throws Exception {
		super.doStart();
		if (_requestBufferSize == _responseBufferSize) {
			_pool[__REQUEST] += _pool[__RESPONSE];
			_pool[__RESPONSE] = 0;
		}

	}

	/**
	 * @return Returns the requestBufferSize.
	 */
	public int getRequestBufferSize() {
		return _requestBufferSize;
	}

	/**
	 * @param requestBufferSize
	 *            The requestBufferSize to set.
	 */
	public void setRequestBufferSize(int requestBufferSize) {
		if (isStarted())
			throw new IllegalStateException();
		_requestBufferSize = requestBufferSize;
	}

	/**
	 * @return Returns the responseBufferSize.
	 */
	public int getResponseBufferSize() {
		return _responseBufferSize;
	}

	/**
	 * @param responseBufferSize
	 *            The responseBufferSize to set.
	 */
	public void setResponseBufferSize(int responseBufferSize) {
		if (isStarted())
			throw new IllegalStateException();
		_responseBufferSize = responseBufferSize;
	}

	protected abstract Buffer newBuffer(int size);

	protected static class ThreadBuffers {
		final Buffer[][] _buffers;

		ThreadBuffers(int requests, int responses, int others) {
			_buffers = new Buffer[3][];
			_buffers[__REQUEST] = new Buffer[requests];
			_buffers[__RESPONSE] = new Buffer[responses];
			_buffers[__OTHER] = new Buffer[others];

		}
	}

	public String toString() {
		return "{{" + _requestBufferSize + "," + _responseBufferSize + "}}";
	}
}
