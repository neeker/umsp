package com.partsoft.umsp.handler;

/**
 * 消息处理器
 * @author neeker
 *
 * @param <E>
 */
public interface DataHandler<E> {
	
	void handle(E object) throws HandleException;

}
