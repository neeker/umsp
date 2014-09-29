package com.partsoft.umsp.sgip;

import com.partsoft.umsp.handler.RecipientEvent;
import com.partsoft.umsp.handler.RecipientListener;
import com.partsoft.utils.Assert;
import com.partsoft.utils.ListUtils;

/**
 * 使用传送管道接收短信的通道处理器
 * @author neeker
 */
public class RecipientListenerSgipSPHandler extends AbstractSgipSPReceiveHandler {
	
	protected Object recipientListener;
	
	public RecipientListenerSgipSPHandler() {
	}
	
	public RecipientListenerSgipSPHandler(RecipientListener recipientListener) {
		setRecipientListener(recipientListener);
	}
	
	public void setRecipientListener(RecipientListener recipientListener) {
		this.recipientListener = recipientListener;
	}
	
	@Override
	protected void doReceivedMessage(Deliver deliver) {
		Assert.notNull(recipientListener);
		for (int i = 0; i < ListUtils.size(this.recipientListener); i++) {
			RecipientListener listener = (RecipientListener) ListUtils.get(recipientListener, i);
			listener.dataArrive(new RecipientEvent(deliver));
		}
	}

	@Override
	protected void doReceivedReport(Report report) {
		Assert.notNull(recipientListener);
		for (int i = 0; i < ListUtils.size(this.recipientListener); i++) {
			RecipientListener listener = (RecipientListener) ListUtils.get(recipientListener, i);
			listener.dataArrive(new RecipientEvent(report));
		}
	}
	
}
