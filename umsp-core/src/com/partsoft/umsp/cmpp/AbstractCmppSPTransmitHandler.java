package com.partsoft.umsp.cmpp;

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
import com.partsoft.umsp.cmpp.Submit;
import com.partsoft.umsp.cmpp.Constants.Commands;
import com.partsoft.utils.CalendarUtils;
import com.partsoft.utils.ListUtils;
import com.partsoft.utils.StringUtils;

public abstract class AbstractCmppSPTransmitHandler extends AbstractCmppContextSPHandler {

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
	 * @brief 一次最多发送16条 (默认参数)
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

	/**
	 * 确认是否返回度列
	 */

	protected Object transmitListener;

	public AbstractCmppSPTransmitHandler() {
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

	public void setMaxSubmitPerSecond(int maxSubmitPerSecond) {
		this.maxSubmitPerSecond = maxSubmitPerSecond;
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
	 *            等于null或者size=0表示全面提取的任务已完成。
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
		boolean request_submiting = CmppUtils.testRequestSubmiting(request);
		int request_activetest_count = CmppUtils.extractRequestActiveTestCount(request);
		boolean throw_packet_timeout = false;
		int wellbeTakeCount = 0;

		if (CmppUtils.testRequestBinded(request) && request_activetest_count < getMaxActiveTestCount()) {
			if (request_idle_time >= this._activeTestIntervalTime * (request_activetest_count + 1)) {
				doActiveTestRequest(request, response);
			} else if (request_activetest_count <= 0 && !request_submiting
					&& ((wellbeTakeCount = testQueuedSubmits()) > 0)) {
				doPostSubmit(request, response, wellbeTakeCount);
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
					List<Submit> submitted_list = (List<Submit>) CmppUtils.extractRequestSubmitteds(request);
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
								SubmitResponse res = (SubmitResponse) context_cmpp_packet_maps.get(
										Commands.CMPP_SUBMIT_RESP).clone();
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
									Log.warn(String.format("忽略应答超时后处理错误(%s)", e.getMessage()), e);
								}
							}
						}
					}
				}

				CmppUtils.cleanRequestSubmitteds(request);
			}
			throw new PacketException(String.format("%d次存活测试未收到应答", request_activetest_count));
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void handleDisConnect(Request request, Response response) {
		if (Log.isDebugEnabled()) {
			Log.debug("传输上下文已断开!");
		}

		if (CmppUtils.testRequestBinded(request) && CmppUtils.testRequestSubmiting(request)) {
			int submitted_result_count = CmppUtils.extractRequestSubmittedRepliedCount(request);
			int submitted_count = CmppUtils.extractRequestSubmittedCount(request);
			if (submitted_count > 0 && submitted_result_count < submitted_count) {
				List<Submit> submitted_list = (List<Submit>) CmppUtils.extractRequestSubmitteds(request);
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
								SubmitResponse res = (SubmitResponse) context_cmpp_packet_maps.get(
										Commands.CMPP_SUBMIT_RESP).clone();
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
									Log.warn(String.format("忽略应答断开后处理错误(%s)", e.getMessage()), e);
								}
							}
						}
					}
				}
			}
			CmppUtils.cleanRequestSubmitteds(request);
		}
		super.handleDisConnect(request, response);
	}

	protected void doPostSubmit(Request request, Response response, int wellbeTakeCount) throws IOException {
		// 上次流量统计的开始时间
		long flowLastTime = CmppUtils.extractRequestFlowLastTime(request);
		// 上次统计以来流量总数
		int flowTotal = CmppUtils.extractRequestFlowTotal(request);
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
		
		if (Log.isDebugEnabled()) {
			Log.debug("准备向服务器提交短信包...");
		}

		String sp_number = Integer.toString(this.spNumber);
		Integer submitted = Integer.valueOf(0);
		List<Submit> takedPostSubmits = null;
		try {
			takedPostSubmits = takeQueuedSubmits(wellbeTakeCount);
		} catch (Throwable e) {
			Log.warn("从待发队列中获取短信失败: " + e.getMessage(), e);
		}
		if (!ListUtils.isEmpty(takedPostSubmits)) {
			if (Log.isDebugEnabled()) {
				Log.debug(String.format("从发送缓冲队列中获取%s条短信进行发送...", takedPostSubmits.size()));
			}
			for (Submit sb : takedPostSubmits) {
				sb.spId = "" + this.enterpriseId;
				if (!StringUtils.hasText(sb.sourceId) || !sb.sourceId.startsWith(sp_number)) {
					sb.sourceId = "" + this.spNumber;
				}
				if (sb.submitCount >= this.packetSubmitRetryTimes) {
					Log.warn(String.format("忽略已重发%d次给用户(%s)的短信:\n%s\n", sb.submitCount,
							sb.getUserNumbersTrimCNPrefix(), sb.toString()));
					continue;
				}
				sb.sequenceId = CmppUtils.generateRequestSequence(request);
				sb.protocolVersion = this.protocolVersion;
				if (Log.isDebugEnabled()) {
					Log.debug(String.format("正在执行第%d条短信提交前处理...", submitted + 1));
				}
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
				if (Log.isDebugEnabled()) {
					Log.debug(String.format("已完成第%d条短信提交前处理!", submitted + 1));
				}
				try {
					if (Log.isDebugEnabled()) {
						Log.debug(String.format("正在提交第%d条短信...", submitted + 1));
					}
					CmppUtils.renderDataPacket(request, response, sb);
					response.flushBuffer();
					if (Log.isDebugEnabled()) {
						Log.debug(String.format("已提交第%d条短信!", submitted + 1));
					}
					sb.submitCount++;
					CmppUtils.updateSubmitteds(request, takedPostSubmits, submitted + 1);
					flowTotal++;
					CmppUtils.updateRequestFlowTotal(request, flowLastTime, flowTotal);
				} catch (IOException e) {
					if (this.errorReturnQueue) {
						returnQueuedSubmits(takedPostSubmits);
					} else {
						returnQueuedSubmits(takedPostSubmits.subList(submitted, takedPostSubmits.size()));
						for (int ei = 0; ei < submitted; ei++) {
							transmit_listener_size = ListUtils.size(transmitListener);
							if (transmit_listener_size > 0) {
								Submit ignSubmitted = takedPostSubmits.get(ei);
								SubmitResponse res = (SubmitResponse) this.context_cmpp_packet_maps.get(
										Commands.CMPP_SUBMIT_RESP).clone();
								res.result = 1;
								TransmitEvent event = new TransmitEvent(new Object[] { ignSubmitted, res });
								for (int j = 0; j < transmit_listener_size; j++) {
									TransmitListener evnListener = (TransmitListener) ListUtils
											.get(transmitListener, j);
									try {
										evnListener.endTransmit(event);
									} catch (Throwable ee) {
										Log.warn(String.format("提交出错后被忽略的提交后处理错误(%s)", e.getMessage()), ee);
									}
								}
							}
						}
					}
					CmppUtils.cleanRequestSubmitteds(request);
					throw e;
				}
				submitted++;
				if (Log.isDebugEnabled()) {
					Log.debug(String.format("执行第%d条短信的提交后处理...", submitted));
				}
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
				if (Log.isDebugEnabled()) {
					Log.debug(String.format("完成第%d条短信的提交后处理...", submitted));
				}
			}
			
			if (Log.isDebugEnabled()) {
				Log.debug(String.format("已提交%d条短信至网关", takedPostSubmits.size()));
			}
			
		} else if (Log.isDebugEnabled()) {
			Log.debug("短信发送缓冲队列为空!");
		}
	}

	@Override
	protected void doDeliver(Request request, Response response) throws IOException {
		super.doDeliver(request, response);
		Deliver deliver_packet = (Deliver) CmppUtils.extractRequestPacket(request);
		if (Log.isDebugEnabled()) {
			Log.debug(deliver_packet.toString());
		}

		int result = 0;
		try {
			if (deliver_packet.registeredDelivery != 1) {
				doReceivedMessage(deliver_packet);
			} else {
				doReceivedReport(deliver_packet);
			}
		} catch (Throwable e) {
			Log.error(e.getMessage(), e);
			result = 9;
		}

		DeliverResponse deliver_response = new DeliverResponse();

		// 必须相同
		deliver_response.sequenceId = deliver_packet.sequenceId;

		// 构建msgid
		deliver_response.nodeId = deliver_packet.nodeId;
		deliver_response.nodeTime = deliver_packet.nodeTime;
		deliver_response.nodeSeq = deliver_packet.nodeSeq;

		deliver_response.result = result;
		CmppUtils.renderDataPacket(request, response, deliver_response);
		response.flushBuffer();
		int wellbeTakeCount = 0;
		if (!CmppUtils.testRequestSubmiting(request) && ((wellbeTakeCount = testQueuedSubmits()) > 0)) {
			doPostSubmit(request, response, wellbeTakeCount);
		}
	}

	@Override
	protected void doBindRequest(Request request, Response response) throws IOException {
		CmppUtils.setupRequestBinding(request, true);
		Connect login = new Connect();
		login.timestamp = CalendarUtils.getTimestampInYearDuring(System.currentTimeMillis());
		login.enterpriseId = this.account;
		login.authenticationToken = CmppUtils.generateClientToken(login.enterpriseId, password, login.timestamp);
		login.sequenceId = CmppUtils.generateRequestSequence(request);
		CmppUtils.renderDataPacket(request, response, login);
		response.flushBuffer();
		if (Log.isDebugEnabled()) {
			Log.debug("已提交登陆请求...");
		}
	}

	@SuppressWarnings("unchecked")
	protected void doSubmitResult(Request request, Response response) throws IOException {
		SubmitResponse res = (SubmitResponse) CmppUtils.extractRequestPacket(request);
		if (Log.isDebugEnabled()) {
			Log.debug(res.toString());
		}

		boolean do_unbind = false;
		if (CmppUtils.testRequestSubmiting(request)) {
			int last_pare_submit_index = CmppUtils.extractRequestSubmittedRepliedCount(request);
			List<Submit> posts = (List<Submit>) CmppUtils.extractRequestSubmitteds(request);
			CmppUtils.updateSubmittedRepliedCount(request, last_pare_submit_index + 1);
			Submit submitted = posts.get(last_pare_submit_index);
			if (Log.isDebugEnabled()) {
				Log.debug(String.format("收到短信发送应答(%d/%d)", last_pare_submit_index, posts.size()));
			}
			if (res.result != 0) {
				Log.warn(String.format("发送包:\n%s\n收到错误应答码: \n%s\n", submitted.toString(), res.toString()));
			}

			int transmit_listener_size = ListUtils.size(transmitListener);
			if (transmit_listener_size > 0) {
				TransmitEvent event = new TransmitEvent(new Object[] { submitted, res });
				for (int j = 0; j < ListUtils.size(transmitListener); j++) {
					TransmitListener evnListener = (TransmitListener) ListUtils.get(transmitListener, j);
					try {
						evnListener.endTransmit(event);
					} catch (Throwable e) {
						Log.warn(String.format("被忽略的短信提交网关应答后处理错误(%s)", e.getMessage()), e);
					}
				}
			}

			if (CmppUtils.extractRequestSubmittedRepliedCount(request) >= CmppUtils
					.extractRequestSubmittedCount(request)) {
				returnQueuedSubmits(null);
				CmppUtils.cleanRequestSubmitteds(request);
				do_unbind = isAutoReSubmit() ? false : true;
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
			Terminate unbind = new Terminate();
			unbind.sequenceId = CmppUtils.generateRequestSequence(request);
			CmppUtils.renderDataPacket(request, response, unbind);
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
		if (Log.isDebugEnabled()) {
			Log.debug("已获得登陆请求应答!");
		}
		CmppUtils.setupRequestBinding(request, false);
		ConnectResponse res = (ConnectResponse) CmppUtils.extractRequestPacket(request);
		if (res.status == 0) {
			CmppUtils.setupRequestBinded(request, true);
			CmppUtils.updateRequestProtocolVersion(request, res.protocolVersion);
			int wellbeTakeCount = 0;
			if ((wellbeTakeCount = testQueuedSubmits()) > 0) {
				doPostSubmit(request, response, wellbeTakeCount);
			}
		} else {
			Log.warn(String.format("连接绑定请求应答码(%d)不正确，连接断开！", res.status));
			throw new ConnectException("登录错误, 请检查用户或密码错误以及客户IP是否正确, 应答: " + res.toString());
		}
	}

	@Override
	protected void doActiveTest(Request request, Response response) throws IOException {
		super.doActiveTest(request, response);
		int wellbeTakeCount = 0;
		if (!CmppUtils.testRequestSubmiting(request) && ((wellbeTakeCount = testQueuedSubmits()) > 0)) {
			doPostSubmit(request, response, wellbeTakeCount);
		}
	}

	@Override
	protected void doActiveTestResponse(Request request, Response response) throws IOException {
		super.doActiveTestResponse(request, response);
		int wellbeTakeCount = 0;
		if (!CmppUtils.testRequestSubmiting(request) && ((wellbeTakeCount = testQueuedSubmits()) > 0)) {
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
