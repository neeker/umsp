package com.partsoft.umsp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

public interface Request {

	/**
	 * 获取请求开始时间戳(连接开始)
	 * 
	 * @return
	 */
	long getStartTimestamp();

	/**
	 * 获取最近请求时间戳
	 * 
	 * @return
	 */
	long getRequestTimestamp();
	
	
	/**
	 * 获取属性
	 * 
	 * @param paramString
	 * @return
	 */
	Object getAttribute(String paramString);

	/**
	 * 设置属性
	 * 
	 * @param paramString
	 * @param paramObject
	 */
	void setAttribute(String paramString, Object paramObject);

	/**
	 * 移除属性
	 * 
	 * @param paramString
	 */
	void removeAttribute(String paramString);

	/**
	 * 获取属性名称
	 * 
	 * @return
	 */
	Enumeration<String> getAttributeNames();

	/**
	 * 获得数据请求包的大小
	 * 
	 * @return
	 */
	int getContentLength();

	/**
	 * 获取数据输入流
	 * 
	 * @return
	 * @throws IOException
	 */
	InputStream getInputStream() throws IOException;

	/**
	 * 获取协议名称
	 * 
	 * @return
	 */
	String getProtocol();

	/**
	 * 获取服务名称
	 * 
	 * @return
	 */
	String getOriginName();

	/**
	 * 获取服务端口
	 * 
	 * @return
	 */
	int getServerPort();

	/**
	 * 获取客户地址(IP)
	 * 
	 * @return
	 */
	String getRemoteAddr();

	/**
	 * 获取客户主机名(可用的话)
	 * 
	 * @return
	 */
	String getRemoteHost();

	/**
	 * 获取客户端口
	 * 
	 * @return
	 */
	int getRemotePort();

	/**
	 * 获取本地主机名(如果可用的话)
	 * 
	 * @return
	 */
	String getLocalName();

	/**
	 * 获取本地IP地址
	 * 
	 * @return
	 */
	String getLocalAddr();

	/**
	 * 获取本地端口
	 * 
	 * @return
	 */
	int getLocalPort();

	/**
	 * 获取请求上下文
	 * 
	 * @return
	 */
	Context getContext();

	/**
	 * 是否安全连接
	 * 
	 * @return
	 */
	boolean isSecure();

	/**
	 * 请求是否已经被处理了
	 * 
	 * @return
	 */
	boolean isHandled();

	/**
	 * 当前连接的请求次数
	 * 
	 * @return 返回1，表示第一次请求
	 */
	int getRequests();
	
}
