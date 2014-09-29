package com.partsoft.umsp.handler;

import java.util.EventListener;

/**
 * 数据传送接收器
 * @author neeker
 */
public interface TransmitListener extends EventListener {
	
	/**
	 * 开始传送，用于开始传递时的回调。
	 */
	void beginTransmit(TransmitEvent event);
	
	/**
	 * 已传送，用于已传送时的回调
	 */
	void transmitted(TransmitEvent event);
	
	/**
	 * 传送完成，传送完成时的回调(例如，服务端已经应答的调用)
	 */
	void endTransmit(TransmitEvent event);
	
	
	/**
	 * 发送错误时的回调。
	 * @param event
	 */
	void transmitTimeout(TransmitEvent event);
	
}
