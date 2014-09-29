package com.partsoft.umsp.smgp;

import java.util.List;

import com.partsoft.umsp.handler.RecipientEvent;
import com.partsoft.umsp.handler.RecipientListener;
import com.partsoft.utils.Assert;
import com.partsoft.utils.ListUtils;

/**
 * 用于转送至特定通道的长连接接收通道
 * @author neeker
 */
public class RecipientListenerSmgpSPHandler extends AbstractSmgpSPTransmitHandler {
	
	protected Object recipientListener;
	
	public RecipientListenerSmgpSPHandler() {
		super();
		super.setLoginMode(Constants.LoginModes.RECEIVE);
	}
	
	public void setRecipientListener(RecipientListener recipientListener) {
		this.recipientListener = recipientListener;
	}	
	
	@Override
	public void setLoginMode(int loginMode) {
		throw new IllegalStateException("not support");
	}

	@Override
	protected void returnQueuedSubmits(List<Submit> submits) {}

	@Override
	protected List<Submit> takeQueuedSubmits(int count) {
		return null;
	}

	@Override
	protected int testQueuedSubmits() {
		return 0;
	}

	@Override
	protected void doReceivedMessage(Deliver deliver) {
		Assert.notNull(recipientListener);
		RecipientEvent event = new RecipientEvent(deliver);
		for (int i = 0; i < ListUtils.size(this.recipientListener); i++) {
			RecipientListener listener = (RecipientListener) ListUtils.get(recipientListener, i);
			listener.dataArrive(event);
		}
	}

	@Override
	protected void doReceivedReport(Deliver report) {
		RecipientEvent event = new RecipientEvent(report);
		for (int i = 0; i < ListUtils.size(this.recipientListener); i++) {
			RecipientListener listener = (RecipientListener) ListUtils.get(recipientListener, i);
			listener.dataArrive(event);
		}
	}

}
