package com.partsoft.umsp.sgip;

import java.io.IOException;
import java.util.List;

import com.partsoft.umsp.Client;
import com.partsoft.umsp.Context;
import com.partsoft.umsp.OriginHandler;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.handler.TransmitEvent;
import com.partsoft.umsp.handler.TransmitListener;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.packet.PacketException;
import com.partsoft.umsp.sgip.Constants.BindResults;
import com.partsoft.umsp.sgip.Constants.BindTypes;
import com.partsoft.umsp.sgip.Constants.Commands;
import com.partsoft.utils.CalendarUtils;
import com.partsoft.utils.ListUtils;

@SuppressWarnings("unchecked")
public abstract class AbstractSgipSMGSendHandler extends AbstractSgipContextHandler {

	private boolean retrySubmit = false;

	protected int maxOnceSubmits = 10;

	protected int maxRequestIdleTime = 1000 * 60;

	protected Object transmitListener;

	protected int maxSubmitPerSecond = 50;

	/**
	 * 发生错误时是否返回队列
	 */
	protected boolean errorReturnQueue = true;

	protected String serviceNumber;

	protected String nodeId;

	protected String account;

	protected String password;

	public void setServiceNumber(String serviceNumber) {
		this.serviceNumber = serviceNumber;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	protected int intNodeId() {
		return Integer.parseInt(nodeId);
	}

	public boolean isAutoReSubmit() {
		return retrySubmit;
	}

	public AbstractSgipSMGSendHandler() {
	}

	public void setErrorReturnQueue(boolean errorReturnQueue) {
		this.errorReturnQueue = errorReturnQueue;
	}

	public void setMaxRequestIdleTime(int maxRequestIdleTime) {
		this.maxRequestIdleTime = maxRequestIdleTime;
	}

	@Override
	protected void doStart() throws Exception {
		super.doStart();
	}

	public void setMaxSubmitPerSecond(int maxSubmitPerSecond) {
		this.maxSubmitPerSecond = maxSubmitPerSecond;
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

	public void setMaxOnceSubmits(int maxOnceSubmits) {
		this.maxOnceSubmits = maxOnceSubmits;
	}

	public int getMaxOnceSubmits() {
		return maxOnceSubmits;
	}

	/**
	 * 返还排队发送的数据
	 * 
	 * @param submits
	 * @param from
	 * @param length
	 */
	protected abstract void returnQueuedSubmits(String number, List<MoForwardPacket> submits);

	/**
	 * 提取排队发送的数据
	 * 
	 * @return
	 */
	protected abstract List<MoForwardPacket> takeQueuedSubmits(String spNumber, int count);

	/**
	 * 判断是否有排队的数据
	 * 
	 * @return
	 */
	protected abstract int testQueuedSubmits(String serviceNumber);

	@Override
	protected void handleTimeout(Request request, Response response) throws IOException {
		long request_idle_time = System.currentTimeMillis() - request.getRequestTimestamp();
		boolean request_submiting = SgipUtils.testRequestSubmiting(request);
		boolean throw_packet_timeout = false;
		int wellbeTakeCount = 0;
		if (SgipUtils.testRequestBinded(request)) {
			if (request_idle_time < this.maxRequestIdleTime && !request_submiting
					&& ((wellbeTakeCount = testQueuedSubmits(this.serviceNumber)) > 0)) {
				doPostSubmit(request, response, wellbeTakeCount);
			} else if (request_idle_time >= this.maxRequestIdleTime) {
				throw_packet_timeout = true;
			}
		} else if (SgipUtils.testRequestBinding(request)) {
			throw_packet_timeout = request_idle_time >= this.maxRequestIdleTime;
		} else {
			throw_packet_timeout = true;
		}

		if (throw_packet_timeout) {
			if (SgipUtils.testRequestSubmiting(request)) {
				int submitted_result_count = SgipUtils.extractRequestSubmittedRepliedCount(request);
				int submitted_count = SgipUtils.extractRequestSubmittedCount(request);
				List<MoForwardPacket> submitted_list = (List<MoForwardPacket>) SgipUtils
						.extractRequestSubmitteds(request);
				List<MoForwardPacket> unresult_list = submitted_list.subList(submitted_result_count, submitted_count);
				if (this.errorReturnQueue) {
					Log.warn(String.format("长时间未收到上行短信提交应答, 把已提交短信返回待发送队列(%d/%d)", submitted_count, submitted_count));
					returnQueuedSubmits(this.serviceNumber, unresult_list);
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
							SgipDataPacket transPacket = unresult_list.get(ii);
							if (transPacket instanceof Deliver) {
								DeliverResponse res = (DeliverResponse) this.context_sgip_packet_maps.get(
										Commands.DELIVER_RESPONSE).clone();
								res.result = 1;
								event = new TransmitEvent(new Object[] { transPacket, res });
							} else {
								ReportResponse res = (ReportResponse) this.context_sgip_packet_maps.get(
										Commands.REPORT_RESPONSE).clone();
								res.result = 1;
								event = new TransmitEvent(new Object[] { transPacket, res });
							}
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
			SgipUtils.cleanRequestSubmitteds(request);
			throw new PacketException(String.format("空闲%d毫秒后未收到应答", maxRequestIdleTime));
		}
	}

	protected void doPostSubmit(Request request, Response response, int wellbeTakeCount) throws IOException {
		// 上次流量统计的开始时间
		long flowLastTime = SgipUtils.extractRequestFlowLastTime(request);
		// 上次统计以来流量总数
		int flowTotal = SgipUtils.extractRequestFlowTotal(request);
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

		int node_id = intNodeId();
		Integer submitted = 0;
		List<MoForwardPacket> takedPostSubmits = null;
		try {
			takedPostSubmits = takeQueuedSubmits(this.serviceNumber, wellbeTakeCount);
		} catch (Throwable e) {
			Log.error("从队列中获取提交数据失败: " + e.getMessage(), e);
		}
		if (takedPostSubmits != null) {
			for (MoForwardPacket sb : takedPostSubmits) {
				if (sb.submitCount >= this.packetSubmitRetryTimes) {
					String phoneNumber = null;
					String msgFormat = null;
					if (sb instanceof Deliver) {
						phoneNumber = ((Deliver) sb).getUserNumberTrimCNPrefix();
						msgFormat = "忽略已转发%d次用户(%s)的上行短信:\n%s\n";
					} else {
						phoneNumber = ((Report) sb).getUserNumberTrimCNPrefix();
						msgFormat = "忽略已转发%d次用户(%s)的短信报告:\n%s\n";
					}
					Log.warn(String.format(msgFormat, sb.submitCount, phoneNumber, sb.toString()));
					continue;
				}

				sb.node_id = node_id;
				sb.timestamp = CalendarUtils.getTimestampInYearDuring(sb.createTimeMillis);
				sb.sequence = SgipUtils.generateContextSequence(request.getContext());

				int transmit_listener_size = ListUtils.size(transmitListener);
				if (transmit_listener_size > 0) {
					TransmitEvent event = new TransmitEvent(sb);
					for (int j = 0; j < transmit_listener_size; j++) {
						TransmitListener evnListener = (TransmitListener) ListUtils.get(transmitListener, j);
						try {
							evnListener.beginTransmit(event);
						} catch (Throwable e) {
							Log.error("被忽略的提交前传输处理错误: " + e.getMessage(), e);
						}
					}
				}
				try {
					SgipUtils.renderDataPacket(request, response, sb);
					response.flushBuffer();
					sb.submitCount++;
					SgipUtils.updateSubmitteds(request, takedPostSubmits, submitted + 1);
					flowTotal++;
					SgipUtils.updateRequestFlowTotal(request, flowLastTime, flowTotal);
				} catch (IOException e) {
					SgipUtils.cleanRequestSubmitteds(request);
					if (this.errorReturnQueue) {
						returnQueuedSubmits(this.serviceNumber, takedPostSubmits);
					} else {
						returnQueuedSubmits(this.serviceNumber,
								takedPostSubmits.subList(submitted, takedPostSubmits.size()));
						for (int ei = 0; ei < submitted; ei++) {
							transmit_listener_size = ListUtils.size(transmitListener);
							if (transmit_listener_size > 0) {
								SgipDataPacket ignSubmitted = takedPostSubmits.get(ei);
								SgipDataPacket res = null;
								if (ignSubmitted instanceof Deliver) {
									DeliverResponse respv = (DeliverResponse) this.context_sgip_packet_maps.get(
											Commands.DELIVER_RESPONSE).clone();
									respv.result = 1;
									res = respv;
								} else {
									ReportResponse respv = (ReportResponse) this.context_sgip_packet_maps.get(
											Commands.REPORT_RESPONSE).clone();
									respv.result = 1;
									res = respv;
								}
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
							Log.error("ignored submit transmit error: " + e.getMessage(), e);
						}
					}
				}
			}
		}
	}

	protected void doSubmitResult(Request request, Response response) throws IOException {
		ResponsePacket res = (ResponsePacket) SgipUtils.extractRequestPacket(request);
		if (Log.isDebugEnabled()) {
			Log.debug(res.toString());
		}

		boolean do_unbind = false;

		if (SgipUtils.testRequestSubmiting(request)) {
			int last_pare_submit_index = SgipUtils.extractRequestSubmittedRepliedCount(request);
			List<SgipDataPacket> posts = (List<SgipDataPacket>) SgipUtils.extractRequestSubmitteds(request);
			SgipUtils.updateSubmittedRepliedCount(request, last_pare_submit_index + 1);
			SgipDataPacket submitted = posts.get(last_pare_submit_index);
			int transmit_listener_size = ListUtils.size(transmitListener);
			if (transmit_listener_size > 0) {
				TransmitEvent event = new TransmitEvent(new Object[] { submitted, res });
				for (int j = 0; j < transmit_listener_size; j++) {
					TransmitListener evnListener = (TransmitListener) ListUtils.get(transmitListener, j);
					try {
						evnListener.endTransmit(event);
					} catch (Throwable e) {
						Log.warn(String.format("被忽略的上行短信提交SP端应答后处理错误(%s)", e.getMessage()), e);
					}
				}
			}
			if (SgipUtils.extractRequestSubmittedRepliedCount(request) >= SgipUtils
					.extractRequestSubmittedCount(request)) {
				returnQueuedSubmits(this.serviceNumber, null);
				SgipUtils.cleanRequestSubmitteds(request);
				int wellbeTakeCount = 0;
				if ((wellbeTakeCount = testQueuedSubmits(this.serviceNumber)) > 0) {
					this.doPostSubmit(request, response, wellbeTakeCount);
				} else {
					do_unbind = isAutoReSubmit() ? false : true;
				}
			}
		} else {
			Log.warn("未提交上行短信，却收到提交应答指令???");
			do_unbind = isAutoReSubmit() ? false : true;
		}

		if (do_unbind) {
			UnBind unbind = new UnBind();
			unbind.node_id = intNodeId();
			unbind.timestamp = CalendarUtils.getTimestampInYearDuring(unbind.createTimeMillis);
			unbind.sequence = SgipUtils.generateRequestSequence(request);
			SgipUtils.renderDataPacket(request, response, unbind);
			response.finalBuffer();
		}
	}

	@Override
	protected void doUnBindResponse(Request request, Response response) throws IOException {
		super.doUnBindResponse(request, response);
	}

	@Override
	protected void doBindResponse(Request request, Response response) throws IOException {
		SgipUtils.setupRequestBinding(request, false);
		BindResponse res = (BindResponse) SgipUtils.extractRequestPacket(request);
		if (res.result == BindResults.SUCCESS) {
			SgipUtils.setupRequestBinded(request, true);
			int wellbeTakeCount = 0;
			if ((wellbeTakeCount = testQueuedSubmits(this.serviceNumber)) > 0) {
				doPostSubmit(request, response, wellbeTakeCount);
			}
		} else {
			throw new BindException("登录错误, 请检查用户或密码错误以及客户IP是否正确, 应答: " + response.toString());
		}
	}

	@Override
	protected void doReportResponse(Request request, Response response) throws IOException {
		super.doReportResponse(request, response);
		doSubmitResult(request, response);
	}

	@Override
	protected void doDeliverResponse(Request request, Response response) throws IOException {
		super.doDeliverResponse(request, response);
		doSubmitResult(request, response);
	}

	@Override
	protected void doBindRequest(Request request, Response response) throws IOException {
		SgipUtils.setupRequestBinding(request, true);
		Bind bind = new Bind();
		bind.user = account;
		bind.pwd = password;
		bind.type = BindTypes.SP_TO_SMG;

		bind.node_id = intNodeId();
		bind.timestamp = CalendarUtils.getTimestampInYearDuring(bind.createTimeMillis);
		bind.sequence = SgipUtils.generateRequestSequence(request);
		SgipUtils.renderDataPacket(request, response, bind);
		response.flushBuffer();
	}

	@Override
	public void setOrigin(OriginHandler server) {
		super.setOrigin(server);
		if (server != null && server instanceof Client) {
			retrySubmit = ((Client) server).isAutoReConnect();
		}
	}

	@Override
	protected void handleContextInitialized(Context ctx) {
		super.handleContextInitialized(ctx);
	}

	@Override
	protected void handleContextDestroyed(Context ctx) {
		super.handleContextDestroyed(ctx);
	}

	@Override
	protected void handleDisConnect(Request request, Response response) {
		if (SgipUtils.testRequestBinded(request) && SgipUtils.testRequestSubmiting(request)) {
			int submitted_count = SgipUtils.extractRequestSubmittedCount(request);
			int submitted_result_count = SgipUtils.extractRequestSubmittedRepliedCount(request);
			if (submitted_count > 0 && submitted_result_count < submitted_count) {
				List<MoForwardPacket> submitted_list = (List<MoForwardPacket>) SgipUtils
						.extractRequestSubmitteds(request);
				List<MoForwardPacket> unresult_list = submitted_list.subList(submitted_result_count, submitted_count);
				if (this.errorReturnQueue) {
					Log.warn(String.format("连接断开，返回已提交未应答上行短信至待发队列(%d/%d)", submitted_result_count, submitted_count));
					this.returnQueuedSubmits(this.serviceNumber, unresult_list);
				} else {
					Log.warn(String.format("连接断开，忽略已提交未应答短信(%d/%d)", submitted_result_count, submitted_count));
					int transmit_listener_size = ListUtils.size(transmitListener);
					if (transmit_listener_size > 0 && unresult_list.size() > 0) {
						for (int ii = 0; ii < unresult_list.size(); ii++) {
							TransmitEvent event = null;
							if (this.errorReturnQueue) {
								event = new TransmitEvent(unresult_list.get(ii));
							} else {
								SgipDataPacket submitted = unresult_list.get(ii);
								SgipDataPacket res = null;
								if (submitted instanceof Deliver) {
									DeliverResponse resvp = (DeliverResponse) context_sgip_packet_maps.get(
											Commands.DELIVER_RESPONSE).clone();
									resvp.result = 1;
									res = resvp;
								} else {
									ReportResponse resvp = (ReportResponse) context_sgip_packet_maps.get(
											Commands.REPORT_RESPONSE).clone();
									resvp.result = 1;
									res = resvp;
								}
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
			SgipUtils.cleanRequestSubmitteds(request);
		}
		super.handleDisConnect(request, response);
	}

}
