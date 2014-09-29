package com.partsoft.umsp.packet;

import java.io.IOException;

import com.partsoft.umsp.EndPoint;
import com.partsoft.umsp.Parser;
import com.partsoft.umsp.io.Buffer;
import com.partsoft.umsp.io.BufferPools;
import com.partsoft.umsp.io.ByteBufferInputStream;
import com.partsoft.umsp.io.EofException;
import com.partsoft.umsp.io.View;

public class PacketParser implements Parser {

	public static final int STATE_START = -13;
	public static final int STATE_HEADER = -5;
	public static final int STATE_END = 0;
	public static final int STATE_CONTENT = 2;

	protected int _state = STATE_START;

	private EndPoint _endPoint;

	private BufferPools _buffers;

	protected int _contentBufferSize;

	private View _contentView = new View();

	private EventHandler _handler;

	private Buffer _packet;

	private Buffer _buffer;

	protected int _header_wellbe_filled = Buffer.INT_SIZE;

	protected int _content_wellbe_filled = 0;
	
	protected int _content_len = -1;
	
	@SuppressWarnings("unused")
	private Input _input;

	public PacketParser(Buffer buffer, EventHandler handler) {
		_packet = buffer;
		_buffer = buffer;
		_handler = handler;
	}

	public PacketParser(BufferPools buffers, EndPoint _endPoint, EventHandler _handler, int contentBufferSize) {
		_header_wellbe_filled = Buffer.INT_SIZE;
		this._buffers = buffers;
		this._endPoint = _endPoint;
		this._handler = _handler;
		this._contentBufferSize = contentBufferSize;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}

	public boolean isComplete() {
		return isState(STATE_END);
	}

	public boolean isMoreInBuffer() {
		if (_packet != null && _packet.hasContent())
			return true;
		return false;
	}

	public boolean isState(int state) {
		return _state == state;
	}

	public boolean isIdle() {
		return isState(STATE_START);
	}

	@Deprecated
	public Buffer getPacketBuffer() {
		if (_packet == null) {
			_packet = _buffers.getBuffer(_contentBufferSize);
		}
		return _packet;
	}

	public long parseAvailable() throws IOException {
		long len = parseNext();
		long total = len > 0 ? len : 0;

		// continue parsing
		while (!isComplete() && _buffer != null && _buffer.length() > 0) {
			len = parseNext();
			if (len > 0)
				total += len;
			else if (len < 0) {
				throw new EofException();
			}
		}
		return total;
	}

	public long parseNext() throws IOException {
		long total_filled = -1;

		if (_state == STATE_END)
			return -1;

		if (_buffer == null) {
			if (_packet == null) {
				_packet = _buffers.getBuffer(_contentBufferSize);
			}
			_buffer = _packet;
		}
		
		while (_header_wellbe_filled > 0) {
			int filled = -1;
			switch (_state) {
			case STATE_START:
				_state = STATE_HEADER;
				//记录当前位置
				_buffer.mark(0);
				if (_buffer.space() < Buffer.INT_SIZE) {
					_buffer.compact();
				}
				break;
			case STATE_HEADER:
				filled = _endPoint.fill(_buffer, _header_wellbe_filled);
				if (filled > 0) {
					if (total_filled < 0) {
						total_filled = 0;
					}
					total_filled += filled;
					_header_wellbe_filled -= filled;
				} else if (total_filled < 0) {
					throw new EofException();
				} else {
					// 如果读取超时获取已到结束直接返回
					return total_filled;
				}

				if (_header_wellbe_filled <= 0) {
					_header_wellbe_filled = 0;
					_content_wellbe_filled = _buffer.getInt() - Buffer.INT_SIZE;
					if (_content_wellbe_filled < 0 || _content_wellbe_filled > Short.MAX_VALUE) {
						throw new PacketException(String.format("数据包长度(%d)不正确", _content_wellbe_filled));
					}
					
					if (_buffer.capacity() < (Buffer.INT_SIZE + _content_wellbe_filled)) {
						//扩展空间
						_buffer.increaseCapacity(Buffer.INT_SIZE + _content_wellbe_filled);
					} 
					if (_buffer.space() < _content_wellbe_filled) {
						//紧凑数据
						_buffer.compact();
					}
					_state = STATE_CONTENT;
				}
				break;
			default:
				_state = STATE_CONTENT;
				break;
			}
			if (_state == STATE_CONTENT) {
				break;
			}
		} // 停止获取数据包长度

		while (_content_wellbe_filled > 0) {
			int filled = _endPoint.fill(_buffer, _content_wellbe_filled);
			if (filled > 0) {
				if (total_filled < 0) {
					total_filled = 0;
				}
				total_filled += filled;
				_content_wellbe_filled -= filled;
			} else if (total_filled < 0) {
				throw new EofException();
			} else {
				break;
			}
			if (_content_wellbe_filled == 0) {
				_state = STATE_END;
				_content_len = _buffer.length();
				Buffer chunk = _buffer.get(_content_len);
				_contentView.update(chunk);
				_handler.packetComplete(_contentView);
			}
		} // 停止获取数据包内容循环
		return total_filled;
	}

	@Deprecated
	public int getContentReaded() {
		return _content_len <= 0 ? 0 : _contentView.putIndex();
	}

	@Deprecated
	public int getContentLength() {
		return _content_len;
	}

	public void reset(boolean returnBuffers) {
		synchronized (this) {
			_state = STATE_START;
			_header_wellbe_filled = Buffer.INT_SIZE;
			_content_wellbe_filled = 0;
			if (_packet != null) {
				if (_packet.length() == 0) {
					if (_packet != null && returnBuffers)
						_buffers.returnBuffer(this._contentBufferSize, _packet);
					_packet = null;
				} else {
					_packet.setMarkIndex(-1);
					_packet.compact();
				}
			}
			if (_content_len >= 0) {
				_contentView.clear();
			}
			_content_len = -1;
			_buffer = _packet;
		}
	}

	public abstract static class EventHandler {

		public abstract void packetComplete(Buffer packetBuffer) throws IOException;

	}

	public static class Input extends ByteBufferInputStream {

		public Input(PacketParser parser, long maxIdleTime) {
			super(parser._contentView);
		}

	}
}
