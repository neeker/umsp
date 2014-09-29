package com.partsoft.umsp;

import java.io.IOException;

public interface Handler extends LifeCycle {

	//已连接
	public static final int CONNECT = 0;
	
	//请求
	public static final int REQUEST = 1;
	
	//等待超时
	public static final int TIMEOUT = 2;
	
	//连接终止
	public static final int DISCONNECT = 3;
	
	public void handle(String protocol, Request request, Response response, int dispatch) throws IOException;

	public void setOrigin(OriginHandler server);

	public OriginHandler getOrigin();

	public void destroy();

}
