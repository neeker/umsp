package com.partsoft.umsp.smgp;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.partsoft.umsp.Constants;
import com.partsoft.umsp.Constants.MessageCodes;
import com.partsoft.umsp.PhoneNumberValidator;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.smgp.Constants.RequestIDs;
import com.partsoft.umsp.smgp.Constants.StatusCodes;
import com.partsoft.umsp.handler.TransmitEvent;
import com.partsoft.umsp.handler.TransmitListener;
import com.partsoft.umsp.io.ByteArrayBuffer;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.packet.PacketException;
import com.partsoft.umsp.utils.UmspUtils;
import com.partsoft.utils.CalendarUtils;
import com.partsoft.utils.ListUtils;
import com.partsoft.utils.StringUtils;

public abstract class AbstractSmgpSMGContextHandler extends AbstractSmgpContextHandler {

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
		SmgpUtils.setupRequestBinding(request, true);
	}

	@Override
	protected final void doBind(Request request, Response response) throws IOException {
		super.doBind(request, response);
		SmgpUtils.setupRequestBinding(request, false);
		String requestServiceNumber = null;
		String requestServiceSign = null;
		int requestMaxSubmitsPerSecond = this.maxSubmitPerSecond;
		int requestMaxDeliversPerSecond = this.maxDeliverPerSecond;

		Login bind = (Login) SmgpUtils.extractRequestPacket(request);
		LoginResponse resp = (LoginResponse) context_smgp_packet_maps.get(RequestIDs.login_resp).clone();
		resp.sequenceId = bind.sequenceId;
		resp.Status = StatusCodes.ERR_AUTH;
		try {
			resp = buildConnectResponse(resp, request.getRemoteAddr(), bind.ClientID, bind.AuthenticatorClient,
					bind.TimeStamp);
		} catch (Throwable e) {
			resp.Status = StatusCodes.ERR_AUTH;
		}
		if (resp.Status == StatusCodes.ERR_SUCCESS) {
			requestServiceNumber = resolveRequestServiceNumber(bind.ClientID);
			requestServiceSign = resolveRequestSignature(bind.ClientID);
			requestMaxSubmitsPerSecond = resolveRequestMaxSubmitsPerSecond(bind.ClientID);
			requestMaxDeliversPerSecond = resolveRequestMaxDeliversPerSecond(bind.ClientID);

			if (StringUtils.hasText(requestServiceNumber)) {
				synchronized (request.getContext()) {
					int request_connected = SmgpUtils.extractRequestConnectionTotal(request, requestServiceNumber);
					int max_connects = resolveRequestMaxConnections(bind.ClientID);
					if (max_connects == 0) {
						resp.Status = StatusCodes.ERR_MAXCONNS;
					} else if (max_connects > 0) {
						if (request_connected >= max_connects) {
							resp.Status = StatusCodes.ERR_MAXCONNS;
						}
					}
				}
			} else {
				resp.Status = StatusCodes.ERR_AUTH;
			}
		}
		if (resp.Status != StatusCodes.ERR_SUCCESS) {
			resp.AuthenticatorServer = null;
		}
		SmgpUtils.renderDataPacket(request, response, resp);
		if (resp.Status == StatusCodes.ERR_SUCCESS) {
			SmgpUtils.stepIncreaseRequestConnection(request, requestServiceNumber);
			SmgpUtils.setupRequestBinded(request, true);
			SmgpUtils.setupRequestServiceNumber(request, requestServiceNumber);
			SmgpUtils.setupRequestServiceSignature(request, requestServiceSign);
			SmgpUtils.setupRequestMaxSubmitPerSecond(request, requestMaxSubmitsPerSecond);
			SmgpUtils.setupRequestMaxDeliverPerSecond(request, requestMaxDeliversPerSecond);
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

	protected abstract LoginResponse buildConnectResponse(final LoginResponse resp, String remoteAddr,
			String enterpriseId, byte[] authenticationToken, int timestamp);

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
		boolean request_submiting = SmgpUtils.testRequestSubmiting(request);
		int request_activetest_count = SmgpUtils.extractRequestActiveTestCount(request);
		String serviceNumber = SmgpUtils.extractRequestServiceNumber(request);
		boolean throw_packet_timeout = false;
		int wellbeTakeCount = 0;
		if (SmgpUtils.testRequestBinded(request) && request_activetest_count < getMaxActiveTestCount()) {
			if (request_idle_time >= (this._activeTestIntervalTime * (request_activetest_count + 1))) {
				doActiveTestRequest(request, response);
			} else if (request_activetest_count <= 0 && !request_submiting
					&& ((wellbeTakeCount = testQueuedDelivers(serviceNumber)) > 0)) {
				doPostDeliver(request, response, wellbeTakeCount);
			}
		} else if (SmgpUtils.testRequestBinding(request)) {
			throw_packet_timeout = request_idle_time >= this._activeTestIntervalTime;
		} else {
			throw_packet_timeout = true;
		}

		if (throw_packet_timeout) {
			if (request_submiting) {
				int submitted_result_count = SmgpUtils.extractRequestSubmittedRepliedCount(request);
				int submitted_count = SmgpUtils.extractRequestSubmittedCount(request);

				if (submitted_count > 0 && submitted_result_count < submitted_count) {
					List<Deliver> submitted_list = (List<Deliver>) SmgpUtils.extractRequestSubmitteds(request);
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
								DeliverResponse res = (DeliverResponse) this.context_smgp_packet_maps.get(
										RequestIDs.deliver_resp).clone();
								res.Status = 1;
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
			SmgpUtils.cleanRequestSubmitteds(request);
			throw new PacketException(String.format("%d次存活测试未收到应答", request_activetest_count));
		}

	}

	protected void doPostDeliver(Request request, Response response, int wellbeTakeCount) throws IOException {

		int currentMaxDeliverPerSecond = SmgpUtils.extractRequestMaxDeliverPerSecond(request);

		// TODO 需要实现从客户配置信息中获取流量控制数目
		// 上次流量统计的开始时间
		long flowLastTime = SmgpUtils.extractRequestFlowLastTime(request);
		// 上次统计以来流量总数
		int flowTotal = SmgpUtils.extractRequestFlowTotal(request);
		// 当前时间
		long currentTimeMilles = System.currentTimeMillis();

		// 如果间隔小于1秒和发送总数大于
		if ((currentTimeMilles - flowLastTime) < 1000 && flowTotal >= currentMaxDeliverPerSecond) {
			if (Log.isDebugEnabled()) {
				Log.debug(String.format("流量超限(%d秒内已发%d条)，最大允许每次秒发送%d条", (currentTimeMilles - flowLastTime) / 1000,
						flowTotal, this.maxSubmitPerSecond));
			}
			return;
		} else if ((currentTimeMilles - flowLastTime) >= 1000) {
			flowLastTime = currentTimeMilles;
			flowTotal = 0;
		}

		Integer submitted = Integer.valueOf(0);
		String serviceNumber = SmgpUtils.extractRequestServiceNumber(request);
		List<Deliver> takedPostSubmits = null;
		try {
			takedPostSubmits = takeQueuedDelivers(serviceNumber, wellbeTakeCount);
		} catch (Throwable e) {
			Log.warn("从待发上行队列中获取短信失败: " + e.getMessage(), e);
		}
		if (takedPostSubmits != null) {
			for (Deliver sb : takedPostSubmits) {
				sb.NodeId = this.gatewayId;
				sb.NodeTime = CalendarUtils.nowTimestampInYearDuring();
				if (sb.submitCount >= this.packetSubmitRetryTimes) {
					Log.warn(String.format("忽略已转发%d次用户(%s)的%s:\n%s\n", sb.submitCount, sb.getSrcTermIdTrimCNPrefix(),
							sb.IsReport == (byte) 1 ? "状态报告" : "上行短信", sb.toString()));
					continue;
				}
				sb.NodeSequenceId = SmgpUtils.generateContextSequence(request.getContext());
				sb.sequenceId = SmgpUtils.generateRequestSequence(request);

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
					SmgpUtils.renderDataPacket(request, response, sb);
					response.flushBuffer();
					sb.submitCount++;
					SmgpUtils.updateSubmitteds(request, takedPostSubmits, submitted + 1);
					flowTotal++;
					SmgpUtils.updateRequestFlowTotal(request, flowLastTime, flowTotal);
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
								DeliverResponse res = (DeliverResponse) this.context_smgp_packet_maps.get(
										RequestIDs.deliver_resp).clone();
								res.Status = 1;
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
					SmgpUtils.cleanRequestSubmitteds(request);
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
		// TODO 临时日志
		// Log.warn("发送至客户端的链路侦测包已应答");
		String serviceNumber = SmgpUtils.extractRequestServiceNumber(request);
		int wellbeTakeCount = 0;
		if (!SmgpUtils.testRequestSubmiting(request) && ((wellbeTakeCount = testQueuedDelivers(serviceNumber)) > 0)) {
			doPostDeliver(request, response, wellbeTakeCount);
		}
	}

	@Override
	protected void doActiveTest(Request request, Response response) throws IOException {
		super.doActiveTest(request, response);

		String serviceNumber = SmgpUtils.extractRequestServiceNumber(request);
		int wellbeTakeCount = 0;
		if (!SmgpUtils.testRequestSubmiting(request) && ((wellbeTakeCount = testQueuedDelivers(serviceNumber)) > 0)) {
			doPostDeliver(request, response, wellbeTakeCount);
		}
	}

	@SuppressWarnings("unchecked")
	protected void doDeliverResult(Request request, Response response) throws IOException {
		DeliverResponse res = (DeliverResponse) SmgpUtils.extractRequestPacket(request);
		if (Log.isDebugEnabled()) {
			Log.debug(res.toString());
		}
		String serviceNumber = SmgpUtils.extractRequestServiceNumber(request);
		if (SmgpUtils.testRequestSubmiting(request)) {
			int last_pare_submit_index = SmgpUtils.extractRequestSubmittedRepliedCount(request);
			List<Deliver> posts = (List<Deliver>) SmgpUtils.extractRequestSubmitteds(request);
			SmgpUtils.updateSubmittedRepliedCount(request, last_pare_submit_index + 1);
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
			if (SmgpUtils.extractRequestSubmittedRepliedCount(request) >= SmgpUtils
					.extractRequestSubmittedCount(request)) {
				returnQueuedDelivers(serviceNumber, null);
				SmgpUtils.cleanRequestSubmitteds(request);
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
		if (this.phoneNumberValidator != null) {
			for (int i = 0; i < submit.DestTermIDCount; i++) {
				if (!submit.DestTermID[i].matches("\\d+") || !this.phoneNumberValidator.testValid(submit.DestTermID[i])) {
					return false;
				}
			}
		} else {
			Log.warn("找不到号码校验器");
			for (int i = 0; i < submit.DestTermIDCount; i++) {
				if (!submit.DestTermID[i].matches("\\d+")) {
					return false;
				}
			}
		}

		String message_context = submit.getMessageContent();
		int cascadeCount = submit.getMessageCascadeCount();
		int cascadeOrder = submit.getMessageCascadeOrder();

		if (cascadeCount > 0 && submit.MsgFormat != MessageCodes.UCS2) {
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
		String serviceNumber = SmgpUtils.extractRequestServiceNumber(request);

		// 获取当前请求的最大发送没秒发送数
		int currentMaxSubmitPerSecond = SmgpUtils.extractRequestMaxSubmitPerSecond(request);

		// TODO 需要实现从客户配置信息中获取流量控制数目
		// 上次流量统计的开始时间
		long flowLastTime = SmgpUtils.extractRequestReceiveFlowLastTime(request);
		// 上次统计以来流量总数
		int flowTotal = SmgpUtils.extractRequestReceiveFlowTotal(request);

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

		Submit submit = (Submit) SmgpUtils.extractRequestPacket(request);
		SubmitResponse resp = (SubmitResponse) this.context_smgp_packet_maps.get(RequestIDs.submit_resp).clone();
		resp.sequenceId = submit.sequenceId;

		resp.NodeId = this.gatewayId;
		resp.NodeTime = (CalendarUtils.getTimestampInYearDuring(submit.createTimeMillis) / 100);
		resp.NodeSequenceId = SmgpUtils.generateContextSequence(request.getContext());
		resp.Status = 9;

		flowTotal++;
		SmgpUtils.updateRequestReceiveFlowTotal(request, flowLastTime, flowTotal);

		if (isMaxReceiveFlowLimited) {
			resp.Status = -1; // 流量超限
		} else if (!StringUtils.hasText(submit.SrcTermID) || !submit.SrcTermID.startsWith(serviceNumber)) {
			resp.Status = 1;
		} else {
			boolean submit_valid = false;
			String serviceSign = SmgpUtils.extractRequestServiceSignature(request);
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
						byte[] sign_bytes = UmspUtils.toGsmBytes(serviceSign, submit.MsgFormat);
						ByteArrayBuffer message_content_buffer = new ByteArrayBuffer(submit.MsgLength
								+ sign_bytes.length);
						message_content_buffer.put(submit.MsgContent);
						message_content_buffer.put(sign_bytes);
						submit.MsgContent = message_content_buffer.array();
						submit.MsgLength = message_content_buffer.length();
					}
					submit.setDataPacketId(UUID.randomUUID().toString());
					submit.sequenceId = resp.sequenceId;
					// 分发客户端提交上来的发送请求
					dispatchSubmit(submit, resp);
					resp.Status = 0;
				} catch (Throwable e) {
					Log.warn(e.getMessage(), e);
				}
			} else {
				resp.Status = 2;
			}
		}
		SmgpUtils.renderDataPacket(request, response, resp);

		// 20130423添加
		int wellbeTakeCount = 0;
		if (!SmgpUtils.testRequestSubmiting(request) && ((wellbeTakeCount = testQueuedDelivers(serviceNumber)) > 0)) {
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
		if (SmgpUtils.testRequestBinded(request)) {
			String serviceNumber = SmgpUtils.extractRequestServiceNumber(request);
			SmgpUtils.stepDecrementRequestConnection(request, serviceNumber);
			if (SmgpUtils.testRequestSubmiting(request)) {
				int submitted_result_count = SmgpUtils.extractRequestSubmittedRepliedCount(request);
				int submitted_count = SmgpUtils.extractRequestSubmittedCount(request);
				if (submitted_count > 0 && submitted_result_count < submitted_count) {
					List<Deliver> submitted_list = (List<Deliver>) SmgpUtils.extractRequestSubmitteds(request);
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
									DeliverResponse res = (DeliverResponse) context_smgp_packet_maps.get(
											RequestIDs.deliver_resp).clone();
									res.Status = 1;
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
				SmgpUtils.cleanRequestSubmitteds(request);
			}
		}
		super.handleDisConnect(request, response);
	}

}
