package com.partsoft.umsp.cmpp;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.partsoft.umsp.Constants;
import com.partsoft.umsp.Constants.MessageCodes;
import com.partsoft.umsp.PhoneNumberValidator;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.cmpp.Constants.Commands;
import com.partsoft.umsp.handler.TransmitEvent;
import com.partsoft.umsp.handler.TransmitListener;
import com.partsoft.umsp.io.ByteArrayBuffer;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.packet.PacketException;
import com.partsoft.umsp.utils.UmspUtils;
import com.partsoft.utils.CalendarUtils;
import com.partsoft.utils.ListUtils;
import com.partsoft.utils.StringUtils;

public abstract class AbstractCmppSMGContextHandler extends AbstractCmppContextHandler {

	protected int gatewayId;

	/**
	 * 链路测试间隔时间
	 */
	protected long _activeTestIntervalTime = 1000 * 60 * 3;

	/**
	 * @brief 最大链路测试数量
	 */
	protected int _maxActiveTestCount = 3;

	/**
	 * @brief 一次最多发送16条
	 */
	protected int _maxOnceDelivers = 10;

	/**
	 * 最大每秒提交数(默认50条)
	 */
	protected int maxSubmitPerSecond = 50;

	/**
	 * 客户转发最大每秒100条(默认)
	 */
	protected int maxDeliverPerSecond = 100;

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

	public long getActiveTestIntervalTime() {
		return _activeTestIntervalTime;
	}

	public void setActiveTestIntervalTime(long activeTestIntervalTime) {
		this._activeTestIntervalTime = activeTestIntervalTime;
	}

	public void setMaxOnceDelivers(int maxOnceSubmits) {
		this._maxOnceDelivers = maxOnceSubmits;
	}

	public void setMaxSubmitPerSecond(int maxSubmitPerSecond) {
		this.maxSubmitPerSecond = maxSubmitPerSecond;
	}

	public int getMaxOnceDelivers() {
		return _maxOnceDelivers;
	}

	public int getMaxActiveTestCount() {
		return _maxActiveTestCount;
	}

	public void setMaxActiveTestCount(int maxActiveTestCount) {
		this._maxActiveTestCount = maxActiveTestCount;
	}

	@Override
	protected void doBindRequest(Request request, Response response) throws IOException {
		CmppUtils.setupRequestBinding(request, true);
	}

	@Override
	protected final void doBind(Request request, Response response) throws IOException {
		super.doBind(request, response);
		CmppUtils.setupRequestBinding(request, false);
		String requestServiceNumber = null;
		String requestServiceSign = null;
		int requestMaxSubmitsPerSecond = this.maxSubmitPerSecond;
		int requestMaxDeliversPerSecond = this.maxDeliverPerSecond;

		Connect bind = (Connect) CmppUtils.extractRequestPacket(request);
		ConnectResponse resp = (ConnectResponse) context_cmpp_packet_maps.get(Commands.CMPP_CONNECT_RESP).clone();
		resp.sequenceId = bind.sequenceId;
		resp.status = 5;
		if (resp.protocolVersion < bind.protocolVersion) {
			resp.status = 4;
		} else {
			try {
				resp = buildConnectResponse(resp, request.getRemoteAddr(), bind.enterpriseId, bind.authenticationToken,
						bind.timestamp);
			} catch (Throwable e) {
				resp.status = 9;
			}
			if (resp.status == 0) {
				requestServiceNumber = resolveRequestServiceNumber(bind.enterpriseId);
				requestServiceSign = resolveRequestSignature(bind.enterpriseId);
				requestMaxSubmitsPerSecond = resolveRequestMaxSubmitsPerSecond(bind.enterpriseId);
				if (requestMaxSubmitsPerSecond < 10) {
					requestMaxSubmitsPerSecond = this.maxSubmitPerSecond;
				}

				requestMaxDeliversPerSecond = resolveRequestMaxDeliversPerSecond(bind.enterpriseId);
				if (requestMaxDeliversPerSecond < 10) {
					requestMaxDeliversPerSecond = this.maxDeliverPerSecond;
				}

				if (StringUtils.hasText(requestServiceNumber)) {
					synchronized (request.getContext()) {
						int request_connected = CmppUtils.extractRequestConnectionTotal(request, requestServiceNumber);
						int max_connects = resolveRequestMaxConnections(bind.enterpriseId);
						if (max_connects == 0) {
							resp.status = 6;
						} else if (max_connects > 0) {
							if (request_connected >= max_connects) {
								resp.status = 6;
							}
						}
					}
				} else {
					resp.status = 5;
				}
			}
		}
		if (resp.status != 0) {
			resp.authenticationToken = null;
		}
		CmppUtils.renderDataPacket(request, response, resp);
		if (resp.status == 0) {
			CmppUtils.stepIncreaseRequestConnection(request, requestServiceNumber);
			CmppUtils.setupRequestBinded(request, true);
			CmppUtils.setupRequestServiceNumber(request, requestServiceNumber);
			CmppUtils.setupRequestServiceSignature(request, requestServiceSign);
			CmppUtils.setupRequestMaxSubmitPerSecond(request, requestMaxSubmitsPerSecond);
			CmppUtils.setupRequestMaxDeliverPerSecond(request, requestMaxDeliversPerSecond);
			response.flushBuffer();
			afterSuccessClientConnected(request, response);

			int wellbeTakeCount = 0;
			if ((wellbeTakeCount = testQueuedDelivers(requestServiceNumber)) > 0) {
				doPostDeliver(request, response, wellbeTakeCount);
			}

		} else {
			response.finalBuffer();
		}
	}

	protected abstract ConnectResponse buildConnectResponse(final ConnectResponse resp, String remoteAddr,
			String enterpriseId, String authenticationToken, int timestamp);

	protected abstract void afterSuccessClientConnected(Request request, Response response);

	protected abstract String resolveRequestServiceNumber(String enterpriseId);

	protected abstract int resolveRequestMaxConnections(String enterpriseId);

	protected abstract String resolveRequestSignature(String enterpriseId);

	protected abstract int resolveRequestMaxSubmitsPerSecond(String enterpriseId);

	protected abstract int resolveRequestMaxDeliversPerSecond(String enterpriseId);

	/**
	 * 返还排队发送的数据
	 * 
	 * @param submits
	 *            等于null或者size=0表示全面提取的任务已完成。
	 */
	protected abstract void returnQueuedDelivers(String serviceNumber, List<Deliver> submits);

	/**
	 * 提取排队发送的数据
	 * 
	 * @return
	 */
	protected abstract List<Deliver> takeQueuedDelivers(String serviceNumber, int wellbeTakeCount);

	/**
	 * 判断是否有排队的数据
	 * 
	 * @return
	 */
	protected abstract int testQueuedDelivers(String serviceNumber);

	public void setErrorReturnQueue(boolean errorReturnQueue) {
		this.errorReturnQueue = errorReturnQueue;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void handleTimeout(Request request, Response response) throws IOException {
		long request_idle_time = System.currentTimeMillis() - request.getRequestTimestamp();
		boolean request_submiting = CmppUtils.testRequestSubmiting(request);
		int request_activetest_count = CmppUtils.extractRequestActiveTestCount(request);
		String serviceNumber = CmppUtils.extractRequestServiceNumber(request);
		boolean throw_packet_timeout = false;
		int wellbeTakeCount = 0;
		if (CmppUtils.testRequestBinded(request) && request_activetest_count < getMaxActiveTestCount()) {
			if (request_idle_time >= (this._activeTestIntervalTime * (request_activetest_count + 1))) {
				doActiveTestRequest(request, response);
			} else if (request_activetest_count <= 0 && !request_submiting
					&& ((wellbeTakeCount = testQueuedDelivers(serviceNumber)) > 0)) {
				doPostDeliver(request, response, wellbeTakeCount);
			}
		} else if (CmppUtils.testRequestBinding(request)) {
			throw_packet_timeout = request_idle_time >= this._activeTestIntervalTime;
		} else {
			throw_packet_timeout = true;
		}

		if (throw_packet_timeout) {
			if (request_submiting) {
				int submitted_result_count = CmppUtils.extractRequestSubmittedRepliedCount(request);
				int submitted_count = CmppUtils.extractRequestSubmittedCount(request);

				if (submitted_count > 0 && submitted_result_count < submitted_count) {
					List<Deliver> submitted_list = (List<Deliver>) CmppUtils.extractRequestSubmitteds(request);
					List<Deliver> unresult_list = submitted_list.subList(submitted_result_count, submitted_count);
					if (this.errorReturnQueue) {
						Log.warn(String
								.format("长时间未收到上行短信提交应答, 把已提交短信返回待发送队列(%d/%d)", submitted_count, submitted_count));
						returnQueuedDelivers(serviceNumber, unresult_list);
					} else {
						Log.warn(String.format("长时间未收到上行短信提交应答，忽略返回待发队列(%d/%d)", submitted_count, submitted_count));
					}
					int transmit_listener_size = ListUtils.size(transmitListener);
					if (transmit_listener_size > 0 && unresult_list.size() > 0) {
						for (int ii = 0; ii < unresult_list.size(); ii++) {
							TransmitEvent event = null;
							if (this.errorReturnQueue) {
								event = new TransmitEvent(unresult_list.get(ii));
							} else {
								Deliver submittedDeliver = unresult_list.get(ii);
								DeliverResponse res = (DeliverResponse) this.context_cmpp_packet_maps.get(
										Commands.CMPP_DELIVER_RESP).clone();
								res.result = 1;
								event = new TransmitEvent(new Object[] { submittedDeliver, res });
							}
							for (int i = 0; i < transmit_listener_size; i++) {
								TransmitListener listener = (TransmitListener) ListUtils.get(transmitListener, i);
								try {
									if (this.errorReturnQueue) {
										listener.transmitTimeout(event);
									} else {
										listener.endTransmit(event);
									}
								} catch (Exception e) {
									Log.warn(String.format("忽略应答超时后处理错误(%s)", e.getMessage()), e);
								}
							}
						}
					}
				}
			}
			CmppUtils.cleanRequestSubmitteds(request);
			throw new PacketException(String.format("%d次存活测试未收到应答", request_activetest_count));
		}

	}

	protected void doPostDeliver(Request request, Response response, int wellbeTakeCount) throws IOException {

		int currentMaxDeliverPerSecond = CmppUtils.extractRequestMaxDeliverPerSecond(request);

		// TODO 需要实现从客户配置信息中获取流量控制数目
		// 上次流量统计的开始时间
		long flowLastTime = CmppUtils.extractRequestFlowLastTime(request);
		// 上次统计以来流量总数
		int flowTotal = CmppUtils.extractRequestFlowTotal(request);
		// 当前时间
		long currentTimeMilles = System.currentTimeMillis();
		// 如果间隔小于1秒和发送总数大于
		if ((currentTimeMilles - flowLastTime) < 1000 && flowTotal >= currentMaxDeliverPerSecond) {
			if (Log.isDebugEnabled()) {
				Log.debug(String.format("流量超限(%d秒内已发%d条)，最大允许每次秒发送%d条", (currentTimeMilles - flowLastTime) / 1000,
						flowTotal, currentMaxDeliverPerSecond));
			}
			return;
		} else if ((currentTimeMilles - flowLastTime) >= 1000) {
			flowLastTime = currentTimeMilles;
			flowTotal = 0;
		}

		Integer submitted = Integer.valueOf(0);
		String serviceNumber = CmppUtils.extractRequestServiceNumber(request);
		List<Deliver> takedPostSubmits = null;
		try {
			takedPostSubmits = takeQueuedDelivers(serviceNumber, wellbeTakeCount);
		} catch (Throwable e) {
			Log.warn("从待发上行队列中获取短信失败: " + e.getMessage(), e);
		}
		if (takedPostSubmits != null) {
			for (Deliver sb : takedPostSubmits) {
				sb.protocolVersion = this.protocolVersion;

				sb.nodeId = this.gatewayId;
				sb.nodeTime = CalendarUtils.nowTimestampInYearDuring();
				sb.nodeSeq = CmppUtils.generateContextSequence(request.getContext());
				if (sb.submitCount >= this.packetSubmitRetryTimes) {
					Log.warn(String.format("忽略已转发%d次来自用户(%s)的%s:\n%s\n", sb.submitCount, sb.getSourceIdTrimCNPrefix(),
							sb.registeredDelivery == (byte) 1 ? "状态报告" : "上行短信", sb.toString()));
					continue;
				}
				sb.sequenceId = CmppUtils.generateRequestSequence(request);

				int transmit_listener_size = ListUtils.size(transmitListener);
				if (transmit_listener_size > 0) {
					TransmitEvent event = new TransmitEvent(sb);
					for (int j = 0; j < transmit_listener_size; j++) {
						TransmitListener evnListener = (TransmitListener) ListUtils.get(transmitListener, j);
						try {
							evnListener.beginTransmit(event);
						} catch (Throwable e) {
							Log.warn(String.format("被忽略的上行短信提交前处理错误(%s)", e.getMessage()), e);
						}
					}
				}
				try {
					CmppUtils.renderDataPacket(request, response, sb);
					response.flushBuffer();
					sb.submitCount++;
					CmppUtils.updateSubmitteds(request, takedPostSubmits, submitted + 1);
					flowTotal++;
					CmppUtils.updateRequestFlowTotal(request, flowLastTime, flowTotal);
				} catch (IOException e) {
					if (this.errorReturnQueue) {
						returnQueuedDelivers(serviceNumber, takedPostSubmits);
					} else {
						returnQueuedDelivers(serviceNumber,
								takedPostSubmits.subList(submitted, takedPostSubmits.size()));
						for (int ei = 0; ei < submitted; ei++) {
							transmit_listener_size = ListUtils.size(transmitListener);
							if (transmit_listener_size > 0) {
								Deliver ignSubmitted = takedPostSubmits.get(ei);
								DeliverResponse res = (DeliverResponse) this.context_cmpp_packet_maps.get(
										Commands.CMPP_DELIVER_RESP).clone();
								res.result = 1;
								TransmitEvent event = new TransmitEvent(new Object[] { ignSubmitted, res });
								for (int j = 0; j < transmit_listener_size; j++) {
									TransmitListener evnListener = (TransmitListener) ListUtils
											.get(transmitListener, j);
									try {
										evnListener.endTransmit(event);
									} catch (Throwable ee) {
										Log.warn(String.format("上行短信提交出错后被忽略的提交后处理错误(%s)", e.getMessage()), ee);
									}
								}
							}
						}
					}
					CmppUtils.cleanRequestSubmitteds(request);
					throw e;
				}
				submitted++;
				transmit_listener_size = ListUtils.size(transmitListener);
				if (transmit_listener_size > 0) {
					TransmitEvent event = new TransmitEvent(sb);
					for (int j = 0; j < transmit_listener_size; j++) {
						TransmitListener evnListener = (TransmitListener) ListUtils.get(transmitListener, j);
						try {
							evnListener.transmitted(event);
						} catch (Throwable e) {
							Log.warn(String.format("被忽略的上行短信提交后处理错误(%s)", e.getMessage()), e);
						}
					}
				}
			}
		}
	}

	@Override
	protected void doActiveTestResponse(Request request, Response response) throws IOException {
		super.doActiveTestResponse(request, response);
		String serviceNumber = CmppUtils.extractRequestServiceNumber(request);
		int wellbeTakeCount = 0;
		if (!CmppUtils.testRequestSubmiting(request) && ((wellbeTakeCount = testQueuedDelivers(serviceNumber)) > 0)) {
			doPostDeliver(request, response, wellbeTakeCount);
		}
	}

	@Override
	protected void doActiveTest(Request request, Response response) throws IOException {
		super.doActiveTest(request, response);
		String serviceNumber = CmppUtils.extractRequestServiceNumber(request);
		int wellbeTakeCount = 0;
		if (!CmppUtils.testRequestSubmiting(request) && ((wellbeTakeCount = testQueuedDelivers(serviceNumber)) > 0)) {
			doPostDeliver(request, response, wellbeTakeCount);
		}
	}

	@SuppressWarnings("unchecked")
	protected void doDeliverResult(Request request, Response response) throws IOException {
		DeliverResponse res = (DeliverResponse) CmppUtils.extractRequestPacket(request);
		if (Log.isDebugEnabled()) {
			Log.debug(res.toString());
		}
		String serviceNumber = CmppUtils.extractRequestServiceNumber(request);
		if (CmppUtils.testRequestSubmiting(request)) {
			int last_pare_submit_index = CmppUtils.extractRequestSubmittedRepliedCount(request);
			List<Deliver> posts = (List<Deliver>) CmppUtils.extractRequestSubmitteds(request);
			CmppUtils.updateSubmittedRepliedCount(request, last_pare_submit_index + 1);
			Deliver submitted = posts.get(last_pare_submit_index);
			int transmit_listener_size = ListUtils.size(transmitListener);
			if (transmit_listener_size > 0) {
				TransmitEvent event = new TransmitEvent(new Object[] { submitted, res });
				for (int j = 0; j < ListUtils.size(transmitListener); j++) {
					TransmitListener evnListener = (TransmitListener) ListUtils.get(transmitListener, j);
					try {
						evnListener.endTransmit(event);
					} catch (Throwable e) {
						Log.warn(String.format("被忽略的上行短信提交SP端应答后处理错误(%s)", e.getMessage()), e);
					}
				}
			}
			if (CmppUtils.extractRequestSubmittedRepliedCount(request) >= CmppUtils
					.extractRequestSubmittedCount(request)) {
				returnQueuedDelivers(serviceNumber, null);
				CmppUtils.cleanRequestSubmitteds(request);
				int wellbeTakeCount = 0;
				if ((wellbeTakeCount = testQueuedDelivers(serviceNumber)) > 0) {
					this.doPostDeliver(request, response, wellbeTakeCount);
				}
			}
		} else {
			Log.warn("未提交上行短信，却收到提交应答指令???");
		}
	}

	protected boolean testClientSubmitValid(Submit submit, int signLen) {
		if ((submit.registeredDelivery == (byte) 1) && submit.destUserCount > 1) {
			return false;
		}

		if (this.phoneNumberValidator != null) {
			for (int i = 0; i < submit.destUserCount; i++) {
				if (!submit.destTerminalIds[i].matches("\\d+")
						|| !this.phoneNumberValidator.testValid(submit.destTerminalIds[i])) {
					return false;
				}
			}
		} else {
			Log.warn("未发现号码校验器实现");
			for (int i = 0; i < submit.destUserCount; i++) {
				if (!submit.destTerminalIds[i].matches("\\d+")) {
					return false;
				}
			}
		}

		String message_context = submit.getMessageContent();
		int cascadeCount = submit.getMessageCascadeCount();
		int cascadeOrder = submit.getMessageCascadeOrder();

		if (cascadeCount > 0 && submit.msgFormat != MessageCodes.UCS2) {
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
		String serviceNumber = CmppUtils.extractRequestServiceNumber(request);

		// 获取当前请求的最大发送没秒发送数
		int currentMaxSubmitPerSecond = CmppUtils.extractRequestMaxSubmitPerSecond(request);

		// TODO 需要实现从客户配置信息中获取流量控制数目
		// 上次流量统计的开始时间
		long flowLastTime = CmppUtils.extractRequestReceiveFlowLastTime(request);
		// 上次统计以来流量总数
		int flowTotal = CmppUtils.extractRequestReceiveFlowTotal(request);

		// 当前时间
		long currentTimeMilles = System.currentTimeMillis();

		boolean isMaxReceiveFlowLimited = false;
		// 是否超限
		// 如果间隔小于1秒和发送总数大于
		if ((currentTimeMilles - flowLastTime) < 1000 && flowTotal > currentMaxSubmitPerSecond) {
			isMaxReceiveFlowLimited = true;
		} else if ((currentTimeMilles - flowLastTime) >= 1000) {
			flowLastTime = currentTimeMilles;
			flowTotal = 0;
		}

		Submit submit = (Submit) CmppUtils.extractRequestPacket(request);
		SubmitResponse resp = (SubmitResponse) this.context_cmpp_packet_maps.get(Commands.CMPP_SUBMIT_RESP).clone();
		resp.protocolVersion = this.protocolVersion;
		resp.sequenceId = submit.sequenceId;

		resp.nodeId = this.gatewayId;
		resp.nodeTime = CalendarUtils.getTimestampInYearDuring(submit.createTimeMillis);
		resp.nodeSeq = CmppUtils.generateContextSequence(request.getContext());
		resp.result = 9;

		flowTotal++;
		CmppUtils.updateRequestReceiveFlowTotal(request, flowLastTime, flowTotal);

		if (isMaxReceiveFlowLimited) {
			resp.result = -1; // 流量超限
		} else if (!StringUtils.hasText(submit.sourceId) || !submit.sourceId.startsWith(serviceNumber)) {
			resp.result = 1;
		} else {
			boolean submit_valid = false;
			String serviceSign = CmppUtils.extractRequestServiceSignature(request);
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
						byte[] sign_bytes = UmspUtils.toGsmBytes(serviceSign, submit.msgFormat);
						ByteArrayBuffer message_content_buffer = new ByteArrayBuffer(submit.msgLength
								+ sign_bytes.length);
						message_content_buffer.put(submit.msgContent);
						message_content_buffer.put(sign_bytes);
						submit.msgContent = message_content_buffer.array();
						submit.msgLength = message_content_buffer.length();
					}
					submit.setDataPacketId(UUID.randomUUID().toString());
					// 复制应答的序列号
					submit.nodeId = resp.nodeId;
					submit.nodeTime = resp.nodeTime;
					submit.nodeSeq = resp.nodeSeq;
					submit.sequenceId = resp.sequenceId;
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
		CmppUtils.renderDataPacket(request, response, resp);

		// 20130423添加
		int wellbeTakeCount = 0;
		if (!CmppUtils.testRequestSubmiting(request) && ((wellbeTakeCount = testQueuedDelivers(serviceNumber)) > 0)) {
			doPostDeliver(request, response, wellbeTakeCount);
		}

	}

	protected abstract void dispatchSubmit(Submit submit, SubmitResponse submit_response);

	@Override
	protected void doDeliverResponse(Request request, Response response) throws IOException {
		super.doDeliverResponse(request, response);
		doDeliverResult(request, response);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void handleDisConnect(Request request, Response response) {
		if (CmppUtils.testRequestBinded(request)) {
			String serviceNumber = CmppUtils.extractRequestServiceNumber(request);
			CmppUtils.stepDecrementRequestConnection(request, serviceNumber);
			if (CmppUtils.testRequestSubmiting(request)) {
				int submitted_result_count = CmppUtils.extractRequestSubmittedRepliedCount(request);
				int submitted_count = CmppUtils.extractRequestSubmittedCount(request);
				if (submitted_count > 0 && submitted_result_count < submitted_count) {
					List<Deliver> submitted_list = (List<Deliver>) CmppUtils.extractRequestSubmitteds(request);
					List<Deliver> unresult_list = submitted_list.subList(submitted_result_count, submitted_count);
					if (this.errorReturnQueue) {
						Log.warn(String
								.format("连接断开，返回已提交未应答上行短信至待发队列(%d/%d)", submitted_result_count, submitted_count));
						this.returnQueuedDelivers(serviceNumber, unresult_list);
					} else {
						Log.warn(String.format("连接断开，忽略已提交未应答短信(%d/%d)", submitted_result_count, submitted_count));
						int transmit_listener_size = ListUtils.size(transmitListener);
						if (transmit_listener_size > 0 && unresult_list.size() > 0) {
							for (int ii = 0; ii < unresult_list.size(); ii++) {
								TransmitEvent event = null;
								if (this.errorReturnQueue) {
									event = new TransmitEvent(unresult_list.get(ii));
								} else {
									Deliver submitted = unresult_list.get(ii);
									DeliverResponse res = (DeliverResponse) context_cmpp_packet_maps.get(
											Commands.CMPP_DELIVER_RESP).clone();
									res.result = 1;
									event = new TransmitEvent(new Object[] { submitted, res });
								}
								for (int i = 0; i < transmit_listener_size; i++) {
									TransmitListener listener = (TransmitListener) ListUtils.get(transmitListener, i);
									try {
										if (this.errorReturnQueue) {
											listener.transmitTimeout(event);
										} else {
											listener.endTransmit(event);
										}
									} catch (Exception e) {
										Log.warn(String.format("忽略上行短信应答断开后处理错误(%s)", e.getMessage()), e);
									}
								}
							}
						}
					}
				}
				CmppUtils.cleanRequestSubmitteds(request);
			}
		}
		super.handleDisConnect(request, response);
	}

}
