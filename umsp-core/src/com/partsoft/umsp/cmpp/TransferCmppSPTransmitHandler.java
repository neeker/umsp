package com.partsoft.umsp.cmpp;

import java.util.List;

import com.partsoft.umsp.BatchPool;
import com.partsoft.umsp.handler.RecipientEvent;
import com.partsoft.umsp.handler.RecipientListener;
import com.partsoft.utils.Assert;
import com.partsoft.utils.ListUtils;

/**
 * 电信协议接收与发送通道双工实现类
 * @author neeker
 */
public class TransferCmppSPTransmitHandler extends AbstractCmppSPTransmitHandler {
	
	protected BatchPool<Submit> batchPool;
	
	protected Object recipientListener;
	
	public TransferCmppSPTransmitHandler() {
		super();
	}
	
	public void setBatchPool(BatchPool<Submit> batchPool) {
		this.batchPool = batchPool;
	}
	
	public void setRecipientListener(RecipientListener recipientListener) {
		this.recipientListener = recipientListener;
	}
	
	public void addRecipientListener(RecipientListener recipientListener) {
		this.recipientListener = ListUtils.add(this.recipientListener, recipientListener);
	}
	
	public void removeRecipientListener(RecipientListener recipientListener) {
		this.recipientListener = ListUtils.remove(this.recipientListener, recipientListener);
	}
	
	@Override
	protected void returnQueuedSubmits(List<Submit> submits) {
		Assert.notNull(batchPool);
		batchPool.returnObjects(submits);
	}

	@Override
	protected List<Submit> takeQueuedSubmits(int count) {
		return batchPool.takeObjects(count > getMaxOnceSubmits() ? getMaxOnceSubmits() : count);
	}

	@Override
	protected int testQueuedSubmits() {
		return batchPool.countPooling(getMaxOnceSubmits());
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
		Assert.notNull(recipientListener);
		RecipientEvent event = new RecipientEvent(report);
		for (int i = 0; i < ListUtils.size(this.recipientListener); i++) {
			RecipientListener listener = (RecipientListener) ListUtils.get(recipientListener, i);
			listener.dataArrive(event);
		}
	}

}
