package com.partsoft.umsp;

public interface HandlerContainer extends LifeCycle {
	
	public void addHandler(Handler handler);

	public void removeHandler(Handler handler);

	public Handler[] getChildHandlers();

	public Handler[] getChildHandlersByClass(Class<?> byclass);

	public Handler getChildHandlerByClass(Class<?> byclass);
	
}
