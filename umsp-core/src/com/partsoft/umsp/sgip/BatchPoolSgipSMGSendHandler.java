package com.partsoft.umsp.sgip;

import java.util.List;

import com.partsoft.umsp.DataPacket;
import com.partsoft.umsp.FilterableBatchPool;

public class BatchPoolSgipSMGSendHandler extends AbstractSgipSMGSendHandler {
	
	protected FilterableBatchPool<MoForwardPacket> batchPool;
	
	@SuppressWarnings("unchecked")
	public void setBatchPool(FilterableBatchPool<? extends DataPacket> batchPool) {
		this.batchPool = (FilterableBatchPool<MoForwardPacket>) batchPool;
	}

	@Override
	protected void returnQueuedSubmits(String serviceNumber, List<MoForwardPacket> submits) {
		this.batchPool.returnObjects(submits, serviceNumber);
	}

	@Override
	protected List<MoForwardPacket> takeQueuedSubmits(String serviceNumber, int count) {
		return (List<MoForwardPacket>) this.batchPool.takeObjects(count > getMaxOnceSubmits() ? getMaxOnceSubmits() : count, serviceNumber);
	}

	@Override
	protected int testQueuedSubmits(String serviceNumber) {
		return this.batchPool.countPooling(getMaxOnceSubmits(), serviceNumber);
	}

}
