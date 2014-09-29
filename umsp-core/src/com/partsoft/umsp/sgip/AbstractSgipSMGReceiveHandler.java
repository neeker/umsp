package com.partsoft.umsp.sgip;

import java.io.IOException;
import java.util.UUID;

import com.partsoft.umsp.Constants;
import com.partsoft.umsp.Constants.MessageCodes;
import com.partsoft.umsp.PhoneNumberValidator;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.sgip.Constants.Commands;
import com.partsoft.umsp.handler.TransmitListener;
import com.partsoft.umsp.io.ByteArrayBuffer;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.sgip.Constants.BindResults;
import com.partsoft.umsp.utils.UmspUtils;
import com.partsoft.utils.CalendarUtils;
import com.partsoft.utils.ListUtils;
import com.partsoft.utils.StringUtils;

public abstract class AbstractSgipSMGReceiveHandler extends AbstractSgipContextHandler {

	protected int gatewayId;

	protected int maxRequestIdleTime = 1000 * 60;

	/**
	 * 最大每秒提交数(默认50条)
	 */
	protected int maxSubmitPerSecond = 50;
	
	/**
	 * 发生错误时是否返回队列
	 */
	protected boolean errorReturnQueue = true;

	protected Object transmitListener;

	protected PhoneNumberValidator phoneNumberValidator;

	/**
	 * 消息占位长度
	 */
	protected int maxMessageLengthPlace = 0;
	
	
	public void setMaxRequestIdleTime(int maxRequestIdleTime) {
		this.maxRequestIdleTime = maxRequestIdleTime;
	}

	public void setGatewayId(int gatewayId) {
		this.gatewayId = gatewayId;
	}

	public int getGatewayId() {
		return gatewayId;
	}

	public void setMaxMessageLengthPlace(int maxMessageLengthPlace) {
		this.maxMessageLengthPlace = maxMessageLengthPlace;
	}

	public void setTransmitListener(TransmitListener listener) {
		if (listener == null) {
			this.transmitListener = null;
		} else {
			this.transmitListener = ListUtils.add(this.transmitListener, listener);
		}
	}
	
	public void setPhoneNumberValidator(PhoneNumberValidator phoneNumberValidator) {
		this.phoneNumberValidator = phoneNumberValidator;
	}

	public void addTransmitListener(TransmitListener listener) {
		this.transmitListener = ListUtils.add(this.transmitListener, listener);
	}

	public void removeTransmitListener(TransmitListener listener) {
		this.transmitListener = ListUtils.remove(this.transmitListener, listener);
	}

	public void setMaxSubmitPerSecond(int maxSubmitPerSecond) {
		this.maxSubmitPerSecond = maxSubmitPerSecond;
	}

	@Override
	protected void doBindRequest(Request request, Response response) throws IOException {
		SgipUtils.setupRequestBinding(request, true);
	}

	@Override
	protected final void doBind(Request request, Response response) throws IOException {
		super.doBind(request, response);
		SgipUtils.setupRequestBinding(request, false);
		String requestServiceNumber = null;
		String requestServiceSign = null;
		int requestMaxSubmitsPerSecond = this.maxSubmitPerSecond;
		
		Bind bind = (Bind) SgipUtils.extractRequestPacket(request);
		BindResponse resp = (BindResponse) context_sgip_packet_maps.get(Commands.BIND_RESPONSE).clone();
		try {
			resp = buildConnectResponse(resp, request.getRemoteAddr(), bind.user, bind.pwd,
					bind.timestamp);
		} catch (Throwable e) {
			resp.result = BindResults.ERROR;
		}
		if (resp.result == BindResults.SUCCESS) {
			requestServiceNumber = resolveRequestServiceNumber(bind.user);
			requestServiceSign = resolveRequestSignature(bind.user);
			requestMaxSubmitsPerSecond = resolveRequestMaxSubmitsPerSecond(bind.user);
			
			if (StringUtils.hasText(requestServiceNumber)) {
				synchronized (request.getContext()) {
					int request_connected = SgipUtils.extractRequestConnectionTotal(request, requestServiceNumber);
					int max_connects = resolveRequestMaxConnections(bind.user);
					if (max_connects == 0) {
						resp.result = BindResults.ERROR;
					} else if (max_connects > 0) {
						if (request_connected >= max_connects) {
							resp.result = BindResults.ERROR;
						}
					}
				}
			} else {
				resp.result = BindResults.ERROR;
			}
		}
		SgipUtils.renderDataPacket(request, response, resp);
		if (resp.result == BindResults.SUCCESS) {
			SgipUtils.stepIncreaseRequestConnection(request, requestServiceNumber);
			SgipUtils.setupRequestBinded(request, true);
			SgipUtils.setupRequestServiceNumber(request, requestServiceNumber);
			SgipUtils.setupRequestServiceSignature(request, requestServiceSign);
			SgipUtils.setupRequestMaxSubmitPerSecond(request, requestMaxSubmitsPerSecond);
			response.flushBuffer();
			afterSuccessClientConnected(request, response);
		} else {
			response.finalBuffer();
		}
	}

	protected abstract BindResponse buildConnectResponse(final BindResponse resp, String remoteAddr,
			String enterpriseId, String authenticationToken, long timestamp);

	protected abstract void afterSuccessClientConnected(Request request, Response response);

	protected abstract String resolveRequestServiceNumber(String enterpriseId);

	protected abstract int resolveRequestMaxConnections(String enterpriseId);

	protected abstract String resolveRequestSignature(String enterpriseId);
	
	protected abstract int resolveRequestMaxSubmitsPerSecond(String enterpriseId);
	
	public void setErrorReturnQueue(boolean errorReturnQueue) {
		this.errorReturnQueue = errorReturnQueue;
	}

	@Override
	protected void handleTimeout(Request request, Response response) throws IOException {
		long request_idle_time = System.currentTimeMillis() - request.getRequestTimestamp();
		if (SgipUtils.testRequestBinded(request) || SgipUtils.testRequestBinding(request)) {
			if (request_idle_time >= maxRequestIdleTime) {
				super.handleTimeout(request, response);
			}
		} else {
			super.handleTimeout(request, response);
		}
	}
	
	protected boolean testClientSubmitValid(Submit submit, int signLen) {
		if (this.phoneNumberValidator != null) {
			for (int i = 0; i < submit.user_count; i++) {
				if (!submit.user_number[i].matches("\\d+")
						|| !this.phoneNumberValidator.testValid(submit.user_number[i])) {
					return false;
				}
			}
		} else {
			Log.warn("not found phone number validator");
			for (int i = 0; i < submit.user_count; i++) {
				if (!submit.user_number[i].matches("\\d+")) {
					return false;
				}
			}
		}

		String message_context = submit.getMessageContent();
		int cascadeCount = submit.getMessageCascadeCount();
		int cascadeOrder = submit.getMessageCascadeOrder();

		if (cascadeCount > 0 && submit.message_coding != MessageCodes.UCS2) {
			return false;
		}

		signLen = signLen < 0 ? 0 : signLen;
		int maxMessageLen = Constants.SMS.MAX_SMS_ONEMSG_CONTENT;
		if (cascadeCount == 0) {
			maxMessageLen = (Constants.SMS.MAX_SMS_ONEMSG_CONTENT - maxMessageLengthPlace - signLen);
		} else {
			if (cascadeOrder < cascadeCount) {
				maxMessageLen = Constants.SMS.MAX_SMS_CASCADEMSG_CONTENT - maxMessageLengthPlace;
			} else {
				maxMessageLen = Constants.SMS.MAX_SMS_CASCADEMSG_CONTENT - maxMessageLengthPlace - signLen;
			}
		}
		return message_context.length() <= maxMessageLen;
	}

	@Override
	protected void doSubmit(Request request, Response response) throws IOException {
		super.doSubmit(request, response);
		String serviceNumber = SgipUtils.extractRequestServiceNumber(request);
		
		//获取当前请求的最大发送没秒发送数
		int currentMaxSubmitPerSecond = SgipUtils.extractRequestMaxSubmitPerSecond(request);
		
		//TODO 需要实现从客户配置信息中获取流量控制数目
		// 上次流量统计的开始时间
		long flowLastTime = SgipUtils.extractRequestReceiveFlowLastTime(request);
		// 上次统计以来流量总数
		int flowTotal = SgipUtils.extractRequestReceiveFlowTotal(request);
		
		// 当前时间
		long currentTimeMilles = System.currentTimeMillis();
		
		boolean isMaxReceiveFlowLimited = false;
		//是否超限
		// 如果间隔小于1秒和发送总数大于
		if ((currentTimeMilles - flowLastTime) < 1000 && flowTotal > currentMaxSubmitPerSecond) {
			isMaxReceiveFlowLimited = true;
		} else if ((currentTimeMilles - flowLastTime) >= 1000) {
			flowLastTime = currentTimeMilles;
			flowTotal = 0;
		}
		
		Submit submit = (Submit) SgipUtils.extractRequestPacket(request);
		SubmitResponse resp = (SubmitResponse) this.context_sgip_packet_maps.get(Commands.SUBMIT_RESPONSE).clone();
		
		resp.node_id = this.gatewayId;
		resp.timestamp = CalendarUtils.getTimestampInYearDuring(submit.createTimeMillis);
		resp.sequence = SgipUtils.generateContextSequence(request.getContext());
		resp.result = 9;
		
		flowTotal++;
		SgipUtils.updateRequestReceiveFlowTotal(request, flowLastTime, flowTotal);
		
		if (isMaxReceiveFlowLimited) {
			resp.result = -1; //流量超限
		} else if (!StringUtils.hasText(submit.sp_number) || !submit.sp_number.startsWith(serviceNumber)) {
			resp.result = 1;
		} else {
			boolean submit_valid = false;
			String serviceSign = SgipUtils.extractRequestServiceSignature(request);
			int signLen = StringUtils.hasText(serviceSign) ? serviceSign.length() : 0;
			try {
				submit_valid = testClientSubmitValid(submit, signLen);
			} catch (Throwable e) {
				Log.warn(String.format("测试发送提交包(%s)是否有效时出错: %s", submit.toString(), e.getMessage()), e);
				submit_valid = false;
			}

			if (submit_valid) {
				try {
					int cascade_count = submit.getMessageCascadeCount();
					int cascade_order = submit.getMessageCascadeOrder();
					if (signLen > 0 && cascade_order == cascade_count) {
						byte[] sign_bytes = UmspUtils.toGsmBytes(serviceSign, submit.message_coding);
						ByteArrayBuffer message_content_buffer = new ByteArrayBuffer(submit.message_length
								+ sign_bytes.length);
						message_content_buffer.put(submit.message_content);
						message_content_buffer.put(sign_bytes);
						submit.message_content = message_content_buffer.array();
						submit.message_length = message_content_buffer.length();
					}
					submit.setDataPacketId(UUID.randomUUID().toString());
					// 分发客户端提交上来的发送请求
					dispatchSubmit(submit, resp);
					resp.result = 0;
				} catch (Throwable e) {
					Log.warn(e.getMessage(), e);
				}
			} else {
				resp.result = 2;
			}
		}
		SgipUtils.renderDataPacket(request, response, resp);
	}

	protected abstract void dispatchSubmit(Submit submit, SubmitResponse submit_response);

	@Override
	protected void handleDisConnect(Request request, Response response) {
		if (SgipUtils.testRequestBinded(request)){
			String serviceNumber = SgipUtils.extractRequestServiceNumber(request);
			SgipUtils.stepDecrementRequestConnection(request, serviceNumber);
		}
		super.handleDisConnect(request, response);
	}

}
