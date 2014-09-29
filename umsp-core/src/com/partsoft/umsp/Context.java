package com.partsoft.umsp;

import java.util.Enumeration;

public interface Context {

	int getMajorVersion();

	int getMinorVersion();

	String getProtocol();

	void log(String paramString, Throwable paramThrowable);
	
	void log(String paramString);

	String getServerInfo();

	String getInitParameter(String paramString);

	Enumeration<String> getInitParameterNames();
	
	Object getAttribute(String paramString);
	
	Enumeration<String> getAttributeNames();
	
	void setAttribute(String paramString, Object paramObject);
	
	void removeAttribute(String paramString);

}
