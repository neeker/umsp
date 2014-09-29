package com.partsoft.umsp.sgip;

import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.SpClientInfoGetter;
import com.partsoft.umsp.handler.RecipientEvent;
import com.partsoft.umsp.handler.RecipientListener;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.sgip.Constants.BindResults;
import com.partsoft.utils.ListUtils;
import com.partsoft.utils.ObjectUtils;
import com.partsoft.utils.StringUtils;

public class SgipSMGRecipientListenerHandler extends AbstractSgipSMGReceiveHandler {
	
	protected Object recipientListener;
	
	protected SpClientInfoGetter clientInfoGetter;

	protected boolean limitTrustRemoteAddr = true;

	public void setClientInfoGetter(SpClientInfoGetter informationResolver) {
		this.clientInfoGetter = informationResolver;
	}

	public void setLimitTrustRemoteAddr(boolean limitTrustRemoteAddr) {
		this.limitTrustRemoteAddr = limitTrustRemoteAddr;
	}
	
	public void setRecipientListener(RecipientListener recipientListener) {
		this.recipientListener = recipientListener;
	}

	@Override
	protected void dispatchSubmit(Submit submit, SubmitResponse submit_response) {
		for (int i = 0; i < ListUtils.size(this.recipientListener); i++) {
			RecipientListener listener = (RecipientListener) ListUtils.get(recipientListener, i);
			listener.dataArrive(new RecipientEvent(submit, submit_response));
		}
	}
	
	@Override
	protected BindResponse buildConnectResponse(BindResponse resp, String remoteAddr, String enterpriseId,
			String authenticationToken, long timestamp) {
		resp.result = BindResults.IPERROR;
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
			resp.result = BindResults.ERROR;
			if (StringUtils.hasText(service_number)) {
				String negotiated_secret = clientInfoGetter.getNegotiatedSecret(enterpriseId);
				//String tmpClientToken = SgipUtils.generateClientToken(enterpriseId, negotiated_secret, timestamp);
				if (ObjectUtils.nullSafeEquals(negotiated_secret, authenticationToken)) {
					resp.result = BindResults.SUCCESS;
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
	protected String resolveRequestServiceNumber(String enterpriseId) {
		return clientInfoGetter.getServiceNumber(enterpriseId);
	}
	
	@Override
	protected int resolveRequestMaxConnections(String enterpriseId) {
		return clientInfoGetter.getMaxConnections(enterpriseId);
	}
	
	@Override
	protected String resolveRequestSignature(String enterpriseId) {
		return clientInfoGetter.getSignature(enterpriseId);
	}

}
