package com.partsoft.umsp.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;

import com.partsoft.umsp.Connection;
import com.partsoft.umsp.Connector;
import com.partsoft.umsp.EndPoint;
import com.partsoft.umsp.Generator;
import com.partsoft.umsp.Handler;
import com.partsoft.umsp.OriginHandler;
import com.partsoft.umsp.io.Buffer;
import com.partsoft.umsp.io.BufferPools;
import com.partsoft.umsp.io.EofException;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.packet.PacketParser.Input;

public class PacketConnection implements Connection {

	private static ThreadLocal<PacketConnection> __currentConnection = new ThreadLocal<PacketConnection>();

	private long _timeStamp = System.currentTimeMillis();

	private int _requests;

	private boolean _handling;

	private boolean _destroy;

	private String _protocol;

	protected final Connector _connector;

	protected final PacketGenerator _generator;

	protected final PacketRequest _request;

	protected final PacketResponse _response;

	protected final PacketParser _parser;

	protected final OriginHandler _handler;

	protected final EndPoint _endp;

	protected final BufferPools _bufferPools;

	protected Input _in;

	protected Output _out;

	private long _connectTimestamp;

	public static PacketConnection getCurrentConnection() {
		return __currentConnection.get();
	}

	protected static void setCurrentConnection(PacketConnection connection) {
		__currentConnection.set(connection);
	}

	public PacketConnection(OriginHandler origin, Connector connector, BufferPools bufferPools, EndPoint _endp,
			PacketGenerator _generator) {
		this._handler = origin;
		this._connector = connector;
		this._endp = _endp;
		this._bufferPools = bufferPools;
		this._parser = new PacketParser(connector, _endp, new RequestHandler(), connector.getRequestBufferSize());
		this._generator = _generator;
		this._request = new PacketRequest(this);
		this._response = new PacketResponse(this);
		this._connectTimestamp = System.currentTimeMillis();
	}

	public void reset(boolean returnBuffers) {
		_parser.reset(returnBuffers);
		_generator.reset(returnBuffers);
		_request.recycle();
		_response.recycle();
	}
	
	protected void resetOut() {
		_generator.reset(false);
		_request.recycle();
		_response.recycle();
	}
	
	public long getConnectTimestamp() {
		return _connectTimestamp;
	}

	public Connector getConnector() {
		return _connector;
	}

	public boolean getResolveNames() {
		return _connector.getResolveNames();
	}

	public String getProtocol() {
		if (_protocol == null) {
			_protocol = _connector != null ? _connector.getProtocol() : "UNKNOWN";
		}
		return _protocol;
	}

	public void handle() throws IOException {

		boolean more_in_buffer = true;
		boolean dis_connect = false;
		int no_progress = 0;

		while (more_in_buffer) {
			try {
				synchronized (this) {
					_handling = true;
				}
				setCurrentConnection(this);
				long io = 0;
				if (!_parser.isComplete()) {
					if (_requests <= 0) {
						// 第一次请求激发连接事件
						handleConnect();
						reset(false);
						_requests++;
						continue;
					} else {
						// 读取数据包
						io = _parser.parseAvailable();
					}
				}

				boolean g_commited = _generator.isCommitted();
				boolean g_complete = _generator.isComplete();

				while (g_commited && !g_complete) {
					long written = _generator.flush();
					io += written;
					if (written <= 0)
						break;
					if (_endp.isBufferingOutput()) {
						_endp.flush();
					}
				}

				if (_endp.isBufferingOutput()) {
					_endp.flush();
					if (!_endp.isBufferingOutput()) {
						no_progress = 0;
					}
				}
				if (io > 0) {
					no_progress = 0;
				} else if (no_progress++ >= 2) {
					return;
				}
			} catch (IOException e) {
				if (e instanceof SocketTimeoutException || e.getCause() instanceof SocketTimeoutException) {
					handleTimeout();
					resetOut();
				} else {
					_endp.close();
					throw e;
				}
			} catch (Throwable e) {
				_endp.close();
				throw new IOException(String.format("连接处理错误: %s", e.getMessage()), e);
			} finally {
				if (!_endp.isOpen()) {
					reset(true);
					more_in_buffer = false;
					if (dis_connect == false) {
						dis_connect = true;
						handleDisConnect();
					}
				}

				setCurrentConnection(null);

				// 检查连接是否还有数据
				more_in_buffer = more_in_buffer ? _parser.isMoreInBuffer() || _endp.isBufferingInput() : more_in_buffer;

				synchronized (this) {
					_handling = false;
					if (_destroy) {
						if (dis_connect == false) {
							reset(true);
							handleDisConnect();
						}
						destroy();
						return;
					}
				}

				boolean parser_complete = _parser.isComplete();
				boolean generator_complete = _generator.isComplete();

				if (parser_complete && generator_complete && !_endp.isBufferingOutput()) {
					more_in_buffer = false;
					if (!_generator.isPersistent()) {
						_parser.reset(true);
					} else {
						reset(false);
					}
				}
			}
		}

	}

	protected void handleTimeout() throws IOException {
		if (_handler.isRunning()) {
			boolean error = false;
			try {
				if (_out != null)
					_out.reopen();
				_connector.customize(_endp, _request);
				_handler.handle(this, Handler.TIMEOUT);
				_request.setHandled(true);
			} catch (IOException e) {
				_request.setHandled(true);
				error = true;
				throw e;
			} catch (Throwable e) {
				if (e instanceof ThreadDeath)
					throw (ThreadDeath) e;
				Log.error(e.getMessage(), e);
				error = true;
				throw new IOException(e);
			} finally {
				if (_endp.isOpen()) {
					if (_generator.isPersistent()) {
						_connector.persist(_endp);
					}
					if (error) {
						_endp.close();
					}
					_response.complete();
				} else {
					_response.complete();
				}
			}
		}
	}

	protected void handleRequest() throws IOException {
		if (_handler.isRunning()) {
			_request.updateRequestTime(System.currentTimeMillis());
			boolean error = false;
			try {
				if (_out != null)
					_out.reopen();
				_connector.customize(_endp, _request);
				_handler.handle(this, Handler.REQUEST);
			} catch (IOException e) {
				_request.setHandled(true);
				error = true;
				throw e;
			} catch (Throwable e) {
				if (e instanceof ThreadDeath)
					throw (ThreadDeath) e;
				error = true;
				throw new IOException(e);
			} finally {
				if (_endp.isOpen()) {
					if (_generator.isPersistent())
						_connector.persist(_endp);
					if (error)
						_endp.close();
					else if (!_response.isCommitted() && !_request.isHandled()) {
						_response.errorTerminated();
						throw new IllegalStateException(String.format(String.format("协议(%s)请求未能被处理", getProtocol())));
					}
					_response.complete();
				} else {
					_response.complete();
				}
			}
		}
	}

	protected void handleConnect() throws IOException {
		if (_handler.isRunning()) {
			boolean error = false;
			try {
				if (_out != null)
					_out.reopen();
				_connector.customize(_endp, _request);
				_handler.handle(this, Handler.CONNECT);
			} catch (IOException e) {
				_request.setHandled(true);
				error = true;
				throw new IOException(e);
			} catch (Throwable e) {
				if (e instanceof ThreadDeath)
					throw (ThreadDeath) e;
				error = true;
				throw new IOException(e);
			} finally {
				if (_endp.isOpen()) {
					if (_generator.isPersistent()) {
						_connector.persist(_endp);
					}
					if (error) {
						_endp.close();
					}
					_response.complete();
				} else {
					_response.complete(); // 关闭了。。。。做爱做的事
				}
			}
		}
	}

	protected void handleDisConnect() {
		try {
			_handler.handle(this, Handler.DISCONNECT);
			_request.setHandled(true);
		} catch (Throwable e) {
			Log.ignore(e);
		}
	}

	public boolean isIdle() {
		return _generator.isIdle() && _parser.isIdle();
	}

	public boolean isConfidential(PacketRequest request) {
		return _connector.isConfidential(request);
	}

	public long getTimeStamp() {
		return _timeStamp;
	}

	public int getRequests() {
		return _requests;
	}

	public PacketParser getParser() {
		return _parser;
	}

	public PacketGenerator getGenerator() {
		return _generator;
	}

	public boolean isResponseCommitted() {
		return _generator.isCommitted();
	}

	public InputStream getInputStream() {
		if (_in == null)
			_in = new PacketParser.Input(((PacketParser) _parser), _connector.getMaxIdleTime());
		return _in;
	}

	public OutputStream getOutputStream() {
		if (_out == null)
			_out = new Output();
		return _out;
	}

	public void destroy() {
		synchronized (this) {
			_destroy = true;
			if (!_handling) {
				if (_parser != null)
					_parser.reset(true);

				if (_generator != null)
					_generator.reset(true);
			}
		}
	}

	public void completeResponse() throws IOException {
		_generator.complete();
	}

	public void finalCompleteResponse() throws IOException {
		completeResponse();
		_endp.close();
	}

	public void terminateResponse() throws IOException {
		_endp.close();
	}

	public void commitResponse(boolean last) throws IOException {
		if (!_generator.isCommitted()) {
			_generator.complete();
		}
		if (last)
			_generator.complete();
	}

	public void flushResponse() throws IOException {
		try {
			commitResponse(Generator.MORE);
			_generator.flush();
		} catch (IOException e) {
			throw (e instanceof EofException) ? e : new EofException(e);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("连接上下文[");
		if (_endp != null) {
			sb.append(_endp.toString());
		} else {
			sb.append("类型=未连接");
		}
		sb.append(", 创建时间=");
		sb.append(_timeStamp);
		sb.append("]");
		return sb.toString();
	}

	public class RequestHandler extends PacketParser.EventHandler {

		@Override
		public void packetComplete(Buffer packetBuffer) throws IOException {
			_requests++;
			if (Log.isDebugEnabled()) {
				Log.debug(String.format("完整接收到一个分组数据包(%d字节)", packetBuffer.length()));
				Log.debug(String.format("数据内容:\n %s\n", packetBuffer.toAllDetailString()));
			}
			handleRequest();
		}

	}

	public class Output extends AbstractGenerator.Output {
		Output() {
			super((AbstractGenerator) PacketConnection.this._generator, _connector.getMaxIdleTime());
		}

		public void close() throws IOException {
			if (_closed)
				return;

			if (!_generator.isCommitted())
				commitResponse(Generator.LAST);
			else
				flushResponse();

			super.close();
		}

		public void flush() throws IOException {
			if (!_generator.isCommitted())
				commitResponse(Generator.MORE);
			super.flush();
		}

	}

	public PacketRequest getRequest() {
		return _request;
	}

	public PacketResponse getResponse() {
		return _response;
	}

}
