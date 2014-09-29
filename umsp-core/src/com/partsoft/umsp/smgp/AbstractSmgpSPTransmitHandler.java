package com.partsoft.umsp.smgp;

import java.io.IOException;
import java.util.List;

import com.partsoft.umsp.Client;
import com.partsoft.umsp.OriginHandler;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.handler.TransmitEvent;
import com.partsoft.umsp.handler.TransmitListener;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.packet.PacketException;
import com.partsoft.umsp.smgp.Constants.RequestIDs;
import com.partsoft.umsp.smgp.Submit;
import com.partsoft.umsp.smgp.Constants.LoginModes;
import com.partsoft.umsp.smgp.Constants.StatusCodes;
import com.partsoft.utils.CalendarUtils;
import com.partsoft.utils.ListUtils;
import com.partsoft.utils.StringUtils;

/**
 * 电信协议用于收发双向通道的抽象实现
 * 
 * @author neeker
 */
public abstract class AbstractSmgpSPTransmitHandler extends AbstractSmgpContextSPHandler {

	private boolean retrySubmit = false;

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
	protected int _maxOnceSubmits = 10;

	/**
	 * 最大每秒提交数
	 */
	protected int maxSubmitPerSecond = 50;

	/**
	 * 发生错误时是否返回队列
	 */
	protected boolean errorReturnQueue = true;

	protected int _loginMode = LoginModes.TRANSMIT;

	protected Object transmitListener;

	public AbstractSmgpSPTransmitHandler() {
	}

	public void setLoginMode(int loginMode) {
		this._loginMode = loginMode;
	}

	public void setErrorReturnQueue(boolean errorReturnQueue) {
		this.errorReturnQueue = errorReturnQueue;
	}

	public void setTransmitListener(TransmitListener listener) {
		if (listener == null) {
			this.transmitListener = null;
		} else {
			this.transmitListener = ListUtils.add(this.transmitListener, listener);
		}
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

	public void setMaxOnceSubmits(int maxOnceSubmits) {
		this._maxOnceSubmits = maxOnceSubmits;
	}

	public int getMaxOnceSubmits() {
		return _maxOnceSubmits;
	}

	public int getMaxActiveTestCount() {
		return _maxActiveTestCount;
	}

	public void setMaxActiveTestCount(int maxActiveTestCount) {
		this._maxActiveTestCount = maxActiveTestCount;
	}

	/**
	 * 返还排队发送的数据
	 * 
	 * @param submits
	 * @param from
	 * @param length
	 */
	protected abstract void returnQueuedSubmits(List<Submit> submits);

	/**
	 * 提取排队发送的数据
	 * 
	 * @return
	 */
	protected abstract List<Submit> takeQueuedSubmits(int count);

	/**
	 * 判断是否有排队的数据
	 * 
	 * @return
	 */
	protected abstract int testQueuedSubmits();

	/**
	 * 接收消息
	 * 
	 * @param deliver
	 */
	protected abstract void doReceivedMessage(Deliver deliver);

	/**
	 * 接收报告
	 * 
	 * @param report
	 */
	protected abstract void doReceivedReport(Deliver report);

	@SuppressWarnings("unchecked")
	@Override
	protected void handleTimeout(Request request, Response response) throws IOException {
		long request_idle_time = System.currentTimeMillis() - request.getRequestTimestamp();
		boolean request_submitting = SmgpUtils.testRequestSubmiting(request);
		int request_activetest_count = SmgpUtils.extractRequestActiveTestCount(request);

		boolean throw_packet_timeout = false;
		int wellbeTakeCount = 0;
		if (SmgpUtils.testRequestBinded(request) && request_activetest_count < getMaxActiveTestCount()) {
			if (request_idle_time >= this._activeTestIntervalTime * (request_activetest_count + 1)) {
				doActiveTestRequest(request, response);
			} else if (request_activetest_count <= 0 && !request_submitting
					&& ((wellbeTakeCount = testQueuedSubmits()) > 0)) {
				doPostSubmit(request, response, wellbeTakeCount);
			}
		} else if (SmgpUtils.testRequestBinding(request)) {
			throw_packet_timeout = request_idle_time >= this._activeTestIntervalTime;
		} else {
			throw_packet_timeout = true;
		}

		if (throw_packet_timeout) {
			if (request_submitting) {
				// 如果有submit再队列中，则回退。
				int submitted_result_count = SmgpUtils.extractRequestSubmittedRepliedCount(request);
				int submitted_count = SmgpUtils.extractRequestSubmittedCount(request);
				if (submitted_count > 0 && submitted_result_count < submitted_count) {
					List<Submit> submitted_list = (List<Submit>) SmgpUtils.extractRequestSubmitteds(request);
					List<Submit> unresult_list = submitted_list.subList(submitted_result_count, submitted_count);
					if (this.errorReturnQueue) {
						Log.warn(String.format("长时间未收到短信提交应答, 把已提交短信返回待发送队列(%d/%d)", submitted_count, submitted_count));
						returnQueuedSubmits(unresult_list);
					} else {
						Log.warn(String.format("长时间未收到短信提交应答，忽略返回待发队列(%d/%d)", submitted_count, submitted_count));
					}
					int transmit_listener_size = ListUtils.size(transmitListener);
					if (transmit_listener_size > 0 && unresult_list.size() > 0) {
						for (int ii = 0; ii < unresult_list.size(); ii++) {
							TransmitEvent event = null;
							if (this.errorReturnQueue) {
								event = new TransmitEvent(unresult_list.get(ii));
							} else {
								Submit submitted = unresult_list.get(ii);
								SubmitResponse res = (SubmitResponse) this.context_smgp_packet_maps.get(
										RequestIDs.submit_resp).clone();
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
									Log.warn(String.format("忽略应答超时后处理错误(%s)", e.getMessage()), e);
								}
							}
						}
					}
				}
				SmgpUtils.cleanRequestSubmitteds(request);
			}
			throw new PacketException(String.format("%d次存活测试未收到应答", request_activetest_count));
		}
	}

	public void setMaxSubmitPerSecond(int maxSubmitPerSecond) {
		this.maxSubmitPerSecond = maxSubmitPerSecond;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void handleDisConnect(Request request, Response response) {
		if (SmgpUtils.testRequestBinded(request) && SmgpUtils.testRequestSubmiting(request)) {
			int submitted_count = SmgpUtils.extractRequestSubmittedCount(request);
			int submitted_result_count = SmgpUtils.extractRequestSubmittedRepliedCount(request);
			if (submitted_count > 0 && submitted_result_count < submitted_count) {
				List<Submit> submitted_list = (List<Submit>) SmgpUtils.extractRequestSubmitteds(request);
				List<Submit> unresult_list = submitted_list.subList(submitted_result_count, submitted_count);
				if (this.errorReturnQueue) {
					Log.warn(String.format("连接断开，返回已提交未应答短信至待发队列(%d/%d)", submitted_result_count, submitted_count));
					returnQueuedSubmits(unresult_list);
				} else {
					Log.warn(String.format("连接断开，忽略已提交未应答短信(%d/%d)", submitted_result_count, submitted_count));
					int transmit_listener_size = ListUtils.size(transmitListener);
					if (transmit_listener_size > 0 && unresult_list.size() > 0) {
						for (int ii = 0; ii < unresult_list.size(); ii++) {
							TransmitEvent event = null;
							if (this.errorReturnQueue) {
								event = new TransmitEvent(unresult_list.get(ii));
							} else {
								Submit submitted = unresult_list.get(ii);
								SubmitResponse res = (SubmitResponse) context_smgp_packet_maps.get(
										RequestIDs.submit_resp).clone();
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
									Log.warn(String.format("忽略应答超时后处理错误(%s)", e.getMessage()), e);
								}
							}
						}
					}
				}
			}
			SmgpUtils.cleanRequestSubmitteds(request);
		}
		super.handleDisConnect(request, response);
	}

	protected void doPostSubmit(Request request, Response response, int wellbeTakeCount) throws IOException {

		// 上次流量统计的开始时间
		long flowLastTime = SmgpUtils.extractRequestFlowLastTime(request);
		// 上次统计以来流量总数
		int flowTotal = SmgpUtils.extractRequestFlowTotal(request);
		// 当前时间
		long currentTimeMilles = System.currentTimeMillis();

		// 如果间隔小于1秒和发送总数大于
		if ((currentTimeMilles - flowLastTime) < 1000 && flowTotal >= this.maxSubmitPerSecond) {
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
		// String enterprise_id = Integer.toString(this.enterpriseId);
		String sp_numer = Integer.toString(this.spNumber);

		List<Submit> takedPostSubmits = null;
		try {
			takedPostSubmits = takeQueuedSubmits(wellbeTakeCount);
		} catch (Throwable e) {
			Log.warn("从待发队列中获取短信失败: " + e.getMessage(), e);
		}
		for (Submit sb : takedPostSubmits) {
			if (!StringUtils.hasText(sb.SrcTermID) || !sb.SrcTermID.startsWith(sp_numer)) {
				sb.SrcTermID = sp_numer;
			}
			if (sb.submitCount >= this.packetSubmitRetryTimes) {
				Log.warn(String.format("忽略已重发%d次给用户(%s)的短信:\n%s\n", sb.submitCount, sb.getUserNumbersTrimCNPrefix(),
						sb.toString()));
				continue;
			}
			sb.MsgType = 6;
			sb.sequenceId = SmgpUtils.generateRequestSequence(request);

			int transmit_listener_size = ListUtils.size(transmitListener);
			if (transmit_listener_size > 0) {
				TransmitEvent event = new TransmitEvent(sb);
				for (int j = 0; j < transmit_listener_size; j++) {
					TransmitListener evnListener = (TransmitListener) ListUtils.get(transmitListener, j);
					try {
						evnListener.beginTransmit(event);
					} catch (Throwable e) {
						Log.warn(String.format("被忽略的短信提交前处理错误(%s)", e.getMessage()), e);
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
				SmgpUtils.cleanRequestSubmitteds(request);
				if (this.errorReturnQueue) {
					returnQueuedSubmits(takedPostSubmits);
				} else {
					returnQueuedSubmits(takedPostSubmits.subList(submitted, takedPostSubmits.size()));
					for (int ei = 0; ei < submitted; ei++) {
						transmit_listener_size = ListUtils.size(transmitListener);
						if (transmit_listener_size > 0) {
							Submit ignSubmitted = takedPostSubmits.get(ei);
							SubmitResponse res = (SubmitResponse) this.context_smgp_packet_maps.get(
									RequestIDs.submit_resp).clone();
							res.Status = 1;
							TransmitEvent event = new TransmitEvent(new Object[] { ignSubmitted, res });
							for (int j = 0; j < transmit_listener_size; j++) {
								TransmitListener evnListener = (TransmitListener) ListUtils.get(transmitListener, j);
								try {
									evnListener.endTransmit(event);
								} catch (Throwable ee) {
									Log.warn(String.format("提交出错后被忽略的提交后处理错误(%s)", e.getMessage()), ee);
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
						Log.warn(String.format("被忽略的提交后处理错误(%s)", e.getMessage()), e);
					}
				}
			}
		}
	}

	@Override
	protected void doDeliver(Request request, Response response) throws IOException {
		super.doDeliver(request, response);
		Deliver deliver_packet = (Deliver) SmgpUtils.extractRequestPacket(request);
		if (Log.isDebugEnabled()) {
			Log.debug(deliver_packet.toString());
		}
		int responseStatus = StatusCodes.ERR_SUCCESS;
		try {
			if (deliver_packet.IsReport != 1) {
				doReceivedMessage(deliver_packet);
			} else {
				doReceivedReport(deliver_packet);
			}
		} catch (Throwable e) {
			responseStatus = 40;
			Log.error(e.getMessage(), e);
		}
		DeliverResponse deliver_response = new DeliverResponse();
		deliver_response.sequenceId = deliver_packet.sequenceId;
		// 设置MSGID
		deliver_response.NodeId = deliver_packet.NodeId;
		deliver_response.NodeTime = deliver_packet.NodeTime;
		deliver_response.NodeSequenceId = deliver_packet.NodeSequenceId;
		deliver_response.Status = responseStatus;
		SmgpUtils.renderDataPacket(request, response, deliver_response);
		response.flushBuffer();

		int wellbeTakeCount = 0;
		if (!SmgpUtils.testRequestSubmiting(request) && ((wellbeTakeCount = testQueuedSubmits()) > 0)) {
			doPostSubmit(request, response, wellbeTakeCount);
		}
	}

	@Override
	protected void doBindRequest(Request request, Response response) throws IOException {
		SmgpUtils.setupRequestBinding(request, true);
		Login login = new Login();
		login.TimeStamp = CalendarUtils.getTimestampInYearDuring(System.currentTimeMillis());
		login.ClientID = account;
		login.AuthenticatorClient = SmgpUtils.generateClientToken(login.ClientID, password, login.TimeStamp);
		login.LoginMode = this._loginMode;
		login.sequenceId = SmgpUtils.generateRequestSequence(request);
		SmgpUtils.renderDataPacket(request, response, login);
		response.flushBuffer();
	}

	@SuppressWarnings("unchecked")
	protected void doSubmitResult(Request request, Response response) throws IOException {
		SubmitResponse res = (SubmitResponse) SmgpUtils.extractRequestPacket(request);
		if (Log.isDebugEnabled()) {
			Log.debug(res.toString());
		}
		boolean do_unbind = false;
		if (SmgpUtils.testRequestSubmiting(request)) {
			int last_pare_submit_index = SmgpUtils.extractRequestSubmittedRepliedCount(request);
			List<Submit> posts = (List<Submit>) SmgpUtils.extractRequestSubmitteds(request);
			SmgpUtils.updateSubmittedRepliedCount(request, last_pare_submit_index + 1);
			Submit submitted = posts.get(last_pare_submit_index);
			if (res.Status != 0) {
				Log.warn(String.format("发送包:\n%s\n收到错误应答码: \n%s\n", submitted.toString(), res.toString()));
			}

			int transmit_listener_size = ListUtils.size(transmitListener);
			if (transmit_listener_size > 0) {
				TransmitEvent event = new TransmitEvent(new Object[] { submitted, res });
				for (int j = 0; j < transmit_listener_size; j++) {
					TransmitListener evnListener = (TransmitListener) ListUtils.get(transmitListener, j);
					try {
						evnListener.endTransmit(event);
					} catch (Throwable e) {
						Log.warn(String.format("被忽略的短信提交网关应答后处理错误(%s)", e.getMessage()), e);
					}
				}
			}
			if (SmgpUtils.extractRequestSubmittedRepliedCount(request) >= SmgpUtils
					.extractRequestSubmittedCount(request)) {
				returnQueuedSubmits(null);
				SmgpUtils.cleanRequestSubmitteds(request);
				int wellbeTakeCount = 0;
				if ((wellbeTakeCount = testQueuedSubmits()) > 0) {
					this.doPostSubmit(request, response, wellbeTakeCount);
				} else {
					do_unbind = isAutoReSubmit() ? false : true;
				}
			}
		} else {
			Log.warn("未提交短信，却收到提交应答指令???");
			do_unbind = isAutoReSubmit() ? false : true;
		}

		if (do_unbind) {
			Exit unbind = new Exit();
			unbind.sequenceId = SmgpUtils.generateRequestSequence(request);
			SmgpUtils.renderDataPacket(request, response, unbind);
			response.finalBuffer();
		}
	}

	@Override
	protected void doSubmitResponse(Request request, Response response) throws IOException {
		super.doSubmitResponse(request, response);
		doSubmitResult(request, response);
	}

	@Override
	protected void doBindResponse(Request request, Response response) throws IOException {
		SmgpUtils.setupRequestBinding(request, false);
		LoginResponse res = (LoginResponse) SmgpUtils.extractRequestPacket(request);
		if (res.Status == StatusCodes.ERR_SUCCESS) {
			SmgpUtils.setupRequestBinded(request, true);
			SmgpUtils.updateRequestProtocolVersion(request, res.Version);
			int wellbeTakeCount = 0;
			if ((wellbeTakeCount = testQueuedSubmits()) > 0) {
				doPostSubmit(request, response, wellbeTakeCount);
			}
		} else {
			Log.warn(String.format("连接绑定请求应答码(%d)不正确，连接断开！", res.Status));
			throw new LoginException("登录错误, 请检查用户或密码错误以及客户IP是否正确, 应答: " + res.toString());
		}
	}

	@Override
	protected void doActiveTest(Request request, Response response) throws IOException {
		super.doActiveTest(request, response);
		int wellbeTakeCount = 0;
		if (!SmgpUtils.testRequestSubmiting(request) && ((wellbeTakeCount = testQueuedSubmits()) > 0)) {
			doPostSubmit(request, response, wellbeTakeCount);
		}
	}

	@Override
	protected void doActiveTestResponse(Request request, Response response) throws IOException {
		super.doActiveTestResponse(request, response);
		int wellbeTakeCount = 0;
		if (!SmgpUtils.testRequestSubmiting(request) && ((wellbeTakeCount = testQueuedSubmits()) > 0)) {
			doPostSubmit(request, response, wellbeTakeCount);
		}
	}

	@Override
	public void setOrigin(OriginHandler server) {
		super.setOrigin(server);
		if (server != null && server instanceof Client) {
			retrySubmit = ((Client) server).isAutoReConnect();
		}
	}

	public boolean isAutoReSubmit() {
		return retrySubmit;
	}

}
