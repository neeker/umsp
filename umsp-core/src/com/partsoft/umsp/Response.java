package com.partsoft.umsp;

import java.io.IOException;
import java.io.OutputStream;

public interface Response {

	/**
	 * 设置内容长度
	 * 
	 * @param paramInt
	 */
	void setContentLength(int paramInt);

	/**
	 * 设置缓冲区大小
	 * 
	 * @param paramInt
	 */
	void setBufferSize(int paramInt);

	/**
	 * 获取缓冲区大小
	 * 
	 * @return
	 */
	int getBufferSize();

	/**
	 * 提交缓冲区
	 * 
	 * @throws IOException
	 */
	void flushBuffer() throws IOException;

	/**
	 * 通知处理器是最后一个缓冲区
	 * 
	 * @throws IOException
	 */
	void finalBuffer() throws IOException;

	/**
	 * 重置缓冲区
	 */
	void resetBuffer();

	/**
	 * 是否已提交数据
	 * 
	 * @return
	 */
	boolean isCommitted();

	/**
	 * 重置应答所有状态
	 */
	void reset();

	/**
	 * 获得输出流
	 * 
	 * @return
	 * @throws IOException
	 */
	OutputStream getOutputStream() throws IOException;

}
