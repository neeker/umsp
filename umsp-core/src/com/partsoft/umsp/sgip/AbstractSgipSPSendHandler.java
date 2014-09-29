package com.partsoft.umsp.sgip;

import java.io.IOException;
import java.util.List;

import com.partsoft.umsp.Client;
import com.partsoft.umsp.Context;
import com.partsoft.umsp.OriginHandler;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.sgip.SubmitResponse;
import com.partsoft.umsp.sgip.Constants.Commands;
import com.partsoft.umsp.handler.TransmitEvent;
import com.partsoft.umsp.handler.TransmitListener;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.packet.PacketException;
import com.partsoft.umsp.sgip.Constants.BindResults;
import com.partsoft.umsp.sgip.Constants.BindTypes;
import com.partsoft.umsp.sgip.Submit;
import com.partsoft.utils.CalendarUtils;
import com.partsoft.utils.ListUtils;
import com.partsoft.utils.StringUtils;

@SuppressWarnings("unchecked")
public abstract class AbstractSgipSPSendHandler extends AbstractSgipContextSPHandler {

	private boolean retrySubmit = false;

	protected int maxOnceSubmits = 10;

	protected int maxRequestIdleTime = 1000 * 60;

	protected Object transmitListener;

	protected int maxSubmitPerSecond = 50;

	/**
	 * 发生错误时是否返回队列
	 */
	protected boolean errorReturnQueue = true;

	public boolean isAutoReSubmit() {
		return retrySubmit;
	}

	public AbstractSgipSPSendHandler() {
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

	@Override
	protected void handleTimeout(Request request, Response response) throws IOException {
		long request_idle_time = System.currentTimeMillis() - request.getRequestTimestamp();
		boolean request_submiting = SgipUtils.testRequestSubmiting(request);
		boolean throw_packet_timeout = false;
		int wellbeTakeCount = 0;
		if (SgipUtils.testRequestBinded(request)) {
			if (request_idle_time < this.maxRequestIdleTime && !request_submiting
					&& ((wellbeTakeCount = testQueuedSubmits()) > 0)) {
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
				if (submitted_count > 0 && submitted_result_count < submitted_count) {
					List<Submit> submitted_list = (List<Submit>) SgipUtils.extractRequestSubmitteds(request);
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
								SubmitResponse res = (SubmitResponse) context_sgip_packet_maps.get(
										Commands.SUBMIT_RESPONSE).clone();
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
				SgipUtils.cleanRequestSubmitteds(request);
			}
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

		int node_id = getNodeId();
		String enterprise_id = this.enterpriseId;
		String sp_numer = this.spNumber;
		Integer submitted = 0;
		List<Submit> takedPostSubmits = null;
		try {
			takedPostSubmits = takeQueuedSubmits(wellbeTakeCount);
		} catch (Throwable e) {
			Log.warn("从待发队列中获取短信失败: " + e.getMessage(), e);
		}
		if (takedPostSubmits != null) {
			for (Submit sb : takedPostSubmits) {
				if (!StringUtils.hasText(sb.sp_number) || !sb.sp_number.startsWith(sp_numer)) {
					sb.sp_number = sp_numer;
				}
				sb.corporation_id = enterprise_id;

				if (sb.submitCount >= this.packetSubmitRetryTimes) {
					Log.warn(String.format("忽略已重发%d次给用户(%s)的短信:\n%s\n", sb.submitCount,
							sb.getUserNumbersTrimCNPrefix(), sb.toString()));
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
							Log.warn(String.format("被忽略的短信提交前处理错误(%s)", e.getMessage()), e);
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
						returnQueuedSubmits(takedPostSubmits);
					} else {
						returnQueuedSubmits(takedPostSubmits.subList(submitted, takedPostSubmits.size()));
						for (int ei = 0; ei < submitted; ei++) {
							transmit_listener_size = ListUtils.size(transmitListener);
							if (transmit_listener_size > 0) {
								Submit ignSubmitted = takedPostSubmits.get(ei);
								SubmitResponse res = (SubmitResponse) this.context_sgip_packet_maps.get(
										Commands.SUBMIT_RESPONSE).clone();
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
					SgipUtils.cleanRequestSubmitteds(request);
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
	}

	protected void doSubmitResult(Request request, Response response) throws IOException {
		SubmitResponse res = (SubmitResponse) SgipUtils.extractRequestPacket(request);
		if (Log.isDebugEnabled()) {
			Log.debug(res.toString());
		}

		boolean do_unbind = false;

		if (SgipUtils.testRequestSubmiting(request)) {
			int last_pare_submit_index = SgipUtils.extractRequestSubmittedRepliedCount(request);
			List<Submit> posts = (List<Submit>) SgipUtils.extractRequestSubmitteds(request);
			SgipUtils.updateSubmittedRepliedCount(request, last_pare_submit_index + 1);
			Submit submitted = posts.get(last_pare_submit_index);
			if (res.result != 0) {
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
			if (SgipUtils.extractRequestSubmittedRepliedCount(request) >= SgipUtils
					.extractRequestSubmittedCount(request)) {
				returnQueuedSubmits(null);
				SgipUtils.cleanRequestSubmitteds(request);
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
			UnBind unbind = new UnBind();
			unbind.node_id = this.getNodeId();
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
			if ((wellbeTakeCount = testQueuedSubmits()) > 0) {
				doPostSubmit(request, response, wellbeTakeCount);
			}
		} else {
			Log.warn(String.format("连接绑定请求应答码(%d)不正确，连接断开！", res.result));
			throw new BindException("登录错误, 请检查用户或密码错误以及客户IP是否正确, 应答: " + res.toString());
		}
	}

	@Override
	protected void doSubmitResponse(Request request, Response response) throws IOException {
		super.doSubmitResponse(request, response);
		doSubmitResult(request, response);
	}

	@Override
	protected void doBindRequest(Request request, Response response) throws IOException {
		SgipUtils.setupRequestBinding(request, true);
		Bind bind = new Bind();
		bind.user = account;
		bind.pwd = password;
		bind.type = BindTypes.SP_TO_SMG;

		bind.node_id = this.getNodeId();
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
				List<Submit> submitted_list = (List<Submit>) SgipUtils.extractRequestSubmitteds(request);
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
								SubmitResponse res = (SubmitResponse) context_sgip_packet_maps.get(
										Commands.SUBMIT_RESPONSE).clone();
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
			}
			SgipUtils.cleanRequestSubmitteds(request);
		}
		super.handleDisConnect(request, response);
	}

}
