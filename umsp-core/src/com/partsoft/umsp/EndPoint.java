package com.partsoft.umsp;

import java.io.IOException;

import com.partsoft.umsp.io.Buffer;

public interface EndPoint {

	/**
	 * 关闭输出
	 * @throws IOException
	 */
	void shutdownOutput() throws IOException;

	/**
	 * 关闭终端
	 * @throws IOException
	 */
	void close() throws IOException;
	
	/**
	 * 从对端获取数据至缓冲区
	 * @param buffer {@link Buffer}
	 * @return 返回获取的字节数
	 * @throws IOException
	 */
	int fill(Buffer buffer) throws IOException;

	/**
	 * 从兑现获取数据至缓冲区
	 * @param buffer {@link Buffer}
	 * @param size
	 * @return 返回获取的字节数
	 * @throws IOException
	 */
	int fill(Buffer buffer, int size) throws IOException;

	int flush(Buffer buffer) throws IOException;

	int flush(Buffer header, Buffer buffer, Buffer trailer) throws IOException;

	public String getLocalAddr();

	public String getLocalHost();

	public int getLocalPort();

	public String getRemoteAddr();

	public String getRemoteHost();

	public int getRemotePort();

	public boolean isBlocking();

	public boolean isBufferred();

	public boolean blockReadable(long millisecs) throws IOException;

	public boolean blockWritable(long millisecs) throws IOException;

	public boolean isOpen();

	public Object getTransport();

	public boolean isBufferingInput();

	public boolean isBufferingOutput();

	public void flush() throws IOException;

}
