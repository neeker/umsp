package com.partsoft.umsp.packet;

import java.io.IOException;
import java.io.OutputStream;

import com.partsoft.umsp.EndPoint;
import com.partsoft.umsp.Generator;
import com.partsoft.umsp.io.Buffer;
import com.partsoft.umsp.io.BufferPools;
import com.partsoft.umsp.io.ByteArrayBuffer;
import com.partsoft.umsp.io.EofException;

public abstract class AbstractGenerator implements Generator {
	// states
	public final static int STATE_START = 1;
	public final static int STATE_WRITED = 2;
	public final static int STATE_FLUSHED = 3;
	public final static int STATE_END = 4;

	private static final byte[] NO_BYTES = {};

	protected int _state = STATE_START;

	protected long _contentWritten = 0;
	protected long _contentLength = -3; // 未知
	protected boolean _last = false;
	protected boolean _close = false;

	protected BufferPools _buffers;
	protected EndPoint _endp;

	protected int _contentBufferSize;

	protected Buffer _buffer; // Buffer for copy of passed _content
	protected Buffer _content; // Buffer passed to addContent

	public AbstractGenerator(BufferPools buffers, EndPoint io, int contentBufferSize) {
		this._buffers = buffers;
		this._endp = io;
		_contentBufferSize = contentBufferSize;
	}

	public void reset(boolean returnBuffers) {
		_last = false;
		_close = false;
		_contentWritten = 0;

		synchronized (this) {
			if (returnBuffers) {
				if (_buffer != null) {
					_buffers.returnBuffer(_contentBufferSize, _buffer);
					_buffer = null;
				}
			} else if (_buffer != null){
				_buffer.clear();
			}
		}
		_content = null;
		_state = STATE_START;
	}

	public void resetBuffer() {
		_last = false;
		_close = false;
		_contentWritten = 0;
		_content = null;
		if (_buffer != null)
			_buffer.clear();
	}

	/**
	 * @return Returns the contentBufferSize.
	 */
	public int getContentBufferSize() {
		return _contentBufferSize;
	}

	/**
	 * @param contentBufferSize
	 *            The contentBufferSize to set.
	 */
	public void increaseContentBufferSize(int contentBufferSize) {
		if (contentBufferSize > _contentBufferSize) {
			_contentBufferSize = contentBufferSize;
			if (_buffer != null) {
				Buffer nb = _buffers.getBuffer(_contentBufferSize);
				nb.put(_buffer);
				_buffers.returnBuffer(_contentBufferSize, _buffer);
				_buffer = nb;
			}
		}
	}

	public Buffer getUncheckedBuffer() {
		return _buffer;
	}

	public boolean isComplete() {
		return _state == STATE_END;
	}

	public boolean isIdle() {
		return _state != STATE_START;
	}

	public boolean isCommitted() {
		return _state != STATE_START;
	}

	/**
	 * @return <code>false</code> if the connection should be closed after a
	 *         request has been read, <code>true</code> if it should be used for
	 *         additional requests.
	 */
	public boolean isPersistent() {
		return !_close;
	}

	public void setPersistent(boolean persistent) {
		_close = !persistent;
	}

	public boolean isBufferFull() {
		if (_buffer != null && _buffer.space() == 0) {
			if (_buffer.length() == 0 && !_buffer.isImmutable())
				_buffer.compact();
			return _buffer.space() == 0;
		}
		return _content != null && _content.length() > 0;
	}

	public boolean isContentWritten() {
		return _contentLength >= 0 && _contentWritten >= _contentLength;
	}
	
	public void setContentLength(long value) {
		if (value < 0)
			_contentLength = -3;
		else
			_contentLength = value;
	}


	/**
	 * Complete the message.
	 * 
	 * @throws IOException
	 */
	public void complete() throws IOException {

	}

	public abstract long flush() throws IOException;

	public long getContentWritten() {
		return _contentWritten;
	}

	public static class Output extends OutputStream {
		protected AbstractGenerator _generator;
		protected long _maxIdleTime;
		protected ByteArrayBuffer _buf = new ByteArrayBuffer(NO_BYTES);
		protected boolean _closed;

		public Output(AbstractGenerator generator, long maxIdleTime) {
			_generator = generator;
			_maxIdleTime = maxIdleTime;
		}

		public void close() throws IOException {
			_closed = true;
		}

		void blockForOutput() throws IOException {
			if (_generator._endp.isBlocking()) {
				try {
					flush();
				} catch (IOException e) {
					_generator._endp.close();
					throw e;
				}
			} else {
				if (!_generator._endp.blockWritable(_maxIdleTime)) {
					_generator._endp.close();
					throw new EofException("timeout");
				}

				_generator.flush();
			}
		}

		void reopen() {
			_closed = false;
		}

		public void flush() throws IOException {
			// block until everything is flushed
			Buffer content = _generator._content;
			Buffer buffer = _generator._buffer;
			if (content != null && content.length() > 0 || buffer != null && buffer.length() > 0
					|| _generator.isBufferFull()) {
				_generator.flush();

				while ((content != null && content.length() > 0 || buffer != null && buffer.length() > 0)
						&& _generator._endp.isOpen())
					blockForOutput();
			}
		}

		public void write(byte[] b, int off, int len) throws IOException {
			_buf.wrap(b, off, len);
			write(_buf);
			_buf.wrap(NO_BYTES);
		}

		public void write(byte[] b) throws IOException {
			_buf.wrap(b);
			write(_buf);
			_buf.wrap(NO_BYTES);
		}

		/*
		 * @see java.io.OutputStream#write(int)
		 */
		public void write(int b) throws IOException {
			if (_closed)
				throw new IOException("Closed");
			if (!_generator._endp.isOpen())
				throw new EofException();

			// Block until we can add _content.
			while (_generator.isBufferFull()) {
				blockForOutput();
				if (_closed)
					throw new IOException("Closed");
				if (!_generator._endp.isOpen())
					throw new EofException();
			}

			// Add the _content
			if (_generator.addContent((byte) b))
				// BufferPools are full so flush.
				flush();

			if (_generator.isContentWritten()) {
				flush();
				close();
			}
		}

		private void write(Buffer buffer) throws IOException {
			if (_closed)
				throw new IOException("Closed");
			if (!_generator._endp.isOpen())
				throw new EofException();

			// Block until we can add _content.
			while (_generator.isBufferFull()) {
				blockForOutput();
				if (_closed)
					throw new IOException("Closed");
				if (!_generator._endp.isOpen())
					throw new EofException();
			}

			// Add the _content
			_generator.addContent(buffer, Generator.MORE);

			// Have to flush and complete headers?
			if (_generator.isBufferFull())
				flush();

			if (_generator.isContentWritten()) {
				flush();
				close();
			}

			// Block until our buffer is free
			while (buffer.length() > 0 && _generator._endp.isOpen())
				blockForOutput();
		}
	}
}
