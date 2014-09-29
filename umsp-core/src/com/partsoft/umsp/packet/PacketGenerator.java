package com.partsoft.umsp.packet;

import java.io.IOException;

import com.partsoft.umsp.EndPoint;
import com.partsoft.umsp.io.Buffer;
import com.partsoft.umsp.io.BufferPools;
import com.partsoft.umsp.io.EofException;
import com.partsoft.umsp.log.Log;

public class PacketGenerator extends AbstractGenerator {

	private boolean _bypass = false;

	public PacketGenerator(BufferPools buffers, EndPoint io, int contentBufferSize) {
		super(buffers, io, contentBufferSize);
	}

	public void reset(boolean returnBuffers) {
		super.reset(returnBuffers);
		_bypass = false;
	}

	public void addContent(Buffer content, boolean last) throws IOException {
		if (_last || _state == STATE_END) {
			if (Log.isDebugEnabled()) Log.debug(String.format("忽略额外的内容: %s", content.toString()));
			content.clear();
			return;
		}
		_last = last;

		// Handle any unfinished business?
		if (_content != null && _content.length() > 0) {
			if (!_endp.isOpen())
				throw new EofException();
			flush();
			if (_content != null && _content.length() > 0)
				throw new IllegalStateException("FULL");
		}

		_content = content;
		_contentWritten += content.length();

		// Handle the _content
		if (_endp != null && _buffer == null && content.length() > 0 && _last) {
			// TODO - use bypass in more cases.
			// Make _content a direct buffer
			_bypass = true;
		} else {
			// Yes - so we better check we have a buffer
			if (_buffer == null)
				_buffer = _buffers.getBuffer(_contentBufferSize);

			// Copy _content to buffer;
			int len = _buffer.put(_content);
			_content.skip(len);
			if (_content.length() == 0)
				_content = null;
		}
	}

	/**
	 * send complete response.
	 * 
	 * @param response
	 */
	public void sendResponse(Buffer response) throws IOException {
		if (_state != STATE_START || _content != null && _content.length() > 0)
			throw new IllegalStateException();

		_last = true;

		_content = response;
		_bypass = true;

		_state = STATE_END;

		// TODO this is not exactly right, but should do.
		_contentLength = _contentWritten = response.length();
	}

	/**
	 * Add content.
	 * 
	 * @param b
	 *            byte
	 * @return true if the buffers are full
	 * @throws IOException
	 */
	public boolean addContent(byte b) throws IOException {
		if (_last || _state == STATE_END) {
			if (Log.isDebugEnabled()) {
				Log.debug(String.format("忽略额外数据: %d", new Byte(b)));
			}
			return false;
		}

		_state = STATE_WRITED;
		// Handle any unfinished business?
		if (_content != null && _content.length() > 0) {
			flush();
			if (_content != null && _content.length() > 0)
				throw new IllegalStateException("FULL");
		}

		_contentWritten++;

		// we better check we have a buffer
		if (_buffer == null)
			_buffer = _buffers.getBuffer(_contentBufferSize);

		// Copy _content to buffer;
		_buffer.put(b);

		return _buffer.space() <= 0;
	}

	public boolean isBufferFull() {
		// Should we flush the buffers?
		boolean full = super.isBufferFull() || _bypass;
		return full;
	}

	/**
	 * Complete the message.
	 * 
	 * @throws IOException
	 */
	public void complete() throws IOException {
		if (_state == STATE_END)
			return;
		super.complete();

		if (_state < STATE_FLUSHED) {
			_state = STATE_FLUSHED;
		}

		flush();
	}

	@SuppressWarnings("unused")
	public long flush() throws IOException {
		try {
			prepareBuffers();

			if (_endp == null) {
				return 0;
			}

			// Keep flushing while there is something to flush (except break
			// below)
			int total = 0;
			long last_len = -1;
			Flushing: while (true) {
				int len = -1;
				int to_flush = ((_buffer != null && _buffer.length() > 0) ? 2 : 0)
						| ((_bypass && _content != null && _content.length() > 0) ? 1 : 0);
				switch (to_flush) {
				case 3:
					throw new IllegalStateException(); // should never happen!
				case 2:
					len = _endp.flush(_buffer);
					break;
				case 1:
					len = _endp.flush(_content);
					break;
				case 0:
					_bypass = false;
					if (_buffer != null) {
						_buffer.clear();
					}

					// Are we completely finished for now?
					if (_content == null || _content.length() == 0) {
						if (_state == STATE_FLUSHED)
							_state = STATE_END;
						if (_state == STATE_END && _close)
							_endp.shutdownOutput();

						break Flushing;
					}

					// Try to prepare more to write.
					prepareBuffers();
				}

				// break If we failed to flush
				if (len > 0)
					total += len;
				else
					break Flushing;

				last_len = len;
			}

			return total;
		} catch (IOException e) {
			Log.ignore(e);
			throw (e instanceof EofException) ? e : new EofException(e);
		}
	}

	private void prepareBuffers() {
		// if we are not flushing an existing chunk
		// Refill buffer if possible
		if (_content != null && _content.length() > 0 && _buffer != null && _buffer.space() > 0) {
			int len = _buffer.put(_content);
			_content.skip(len);
			if (_content.length() == 0)
				_content = null;
		}
		if (_content != null && _content.length() == 0)
			_content = null;

	}

	public int getBytesBuffered() {
		return (_buffer == null ? 0 : _buffer.length()) + (_content == null ? 0 : _content.length());
	}

	public boolean isEmpty() {
		return (_buffer == null || _buffer.length() == 0) && (_content == null || _content.length() == 0);
	}

	public String toString() {
		return "PacketGenerator s=" + _state + " h=" + " b=" + (_buffer == null ? "null" : ("" + _buffer.length()))
				+ " c=" + (_content == null ? "null" : ("" + _content.length()));
	}
}
