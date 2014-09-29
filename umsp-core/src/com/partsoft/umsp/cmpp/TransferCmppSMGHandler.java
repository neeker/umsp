package com.partsoft.umsp.cmpp;

import java.util.List;

import com.partsoft.umsp.FilterableBatchPool;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.SpClientInfoGetter;
import com.partsoft.umsp.handler.RecipientEvent;
import com.partsoft.umsp.handler.RecipientListener;
import com.partsoft.umsp.log.Log;
import com.partsoft.utils.ListUtils;
import com.partsoft.utils.StringUtils;

public class TransferCmppSMGHandler extends AbstractCmppSMGContextHandler {
	
	protected FilterableBatchPool<Deliver> batchPool;

	protected Object recipientListener;

	protected SpClientInfoGetter clientInfoGetter;

	protected boolean limitTrustRemoteAddr = true;

	public void setClientInfoGetter(SpClientInfoGetter informationResolver) {
		this.clientInfoGetter = informationResolver;
	}

	public void setLimitTrustRemoteAddr(boolean limitTrustRemoteAddr) {
		this.limitTrustRemoteAddr = limitTrustRemoteAddr;
	}

	public void setBatchPool(FilterableBatchPool<Deliver> batchPool) {
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
	protected ConnectResponse buildConnectResponse(final ConnectResponse resp, String remoteAddr, String enterpriseId,
			String authenticationToken, int timestamp) {
		resp.status = 2;
		boolean truct_ip =false;
		if (this.limitTrustRemoteAddr) {
			if (clientInfoGetter.isMustCheckRemoteAddr(enterpriseId)) {
				truct_ip = clientInfoGetter.isRemoteAddrTrust(remoteAddr);
			} else {
				truct_ip = true;
			}
		} else {
			truct_ip = true;
		}
		if (truct_ip) {
			String service_number = clientInfoGetter.getServiceNumber(enterpriseId);
			if (Log.isDebugEnabled()) {
				Log.debug(String.format("sp cid=%s, sp-num=%s", enterpriseId, service_number));
			}
			resp.status = 3;
			if (StringUtils.hasText(service_number)) {
				String negotiated_secret = clientInfoGetter.getNegotiatedSecret(enterpriseId);
				String tmpClientToken = CmppUtils.generateClientToken(enterpriseId, negotiated_secret, timestamp);
				if (tmpClientToken.equals(authenticationToken)) {
					resp.status = 0;
					resp.authenticationToken = CmppUtils.generateServerToken(0, authenticationToken, negotiated_secret);
				}
			}
		}
		return resp;
	}

	@Override
	protected void afterSuccessClientConnected(Request request, Response response) {

	}
	
	@Override
	protected int resolveRequestMaxSubmitsPerSecond(String enterpriseId) {
		return clientInfoGetter.getMaxSubmitPerSecond(enterpriseId);
	}

	@Override
	protected int resolveRequestMaxDeliversPerSecond(String enterpriseId) {
		return clientInfoGetter.getMaxDeliverPerSecond(enterpriseId);
	}

	@Override
	protected String resolveRequestServiceNumber(String enterpriseId) {
		return clientInfoGetter.getServiceNumber(enterpriseId);
	}
	
	@Override
	protected int resolveRequestMaxConnections(String enterpriseId) {
		return clientInfoGetter.getMaxConnections(enterpriseId);
	}

	@Override
	protected void returnQueuedDelivers(String serviceNumber, List<Deliver> submits) {
		batchPool.returnObjects(submits, serviceNumber);
	}

	@Override
	protected List<Deliver> takeQueuedDelivers(String serviceNumber, int count) {
		return batchPool.takeObjects(count > getMaxOnceDelivers() ? getMaxOnceDelivers() : count, serviceNumber);
	}

	@Override
	protected int testQueuedDelivers(String serviceNumber) {
		return batchPool.countPooling(getMaxOnceDelivers(), serviceNumber);
	}

	@Override
	protected void dispatchSubmit(Submit submit, SubmitResponse submit_response) {
		RecipientEvent event = new RecipientEvent(submit, submit_response);
		for (int i = 0; i < ListUtils.size(this.recipientListener); i++) {
			RecipientListener listener = (RecipientListener) ListUtils.get(recipientListener, i);
			listener.dataArrive(event);
		}
	}

	@Override
	protected String resolveRequestSignature(String enterpriseId) {
		return clientInfoGetter.getSignature(enterpriseId);
	}

}
