package com.partsoft.umsp.sgip;

import java.io.IOException;

import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.packet.PacketException;
import com.partsoft.umsp.sgip.Constants.BindResults;
import com.partsoft.utils.CompareUtils;
import com.partsoft.utils.StringUtils;

public abstract class AbstractSgipSPReceiveHandler extends AbstractSgipContextSPHandler {

	protected int maxRequestIdleTime = 1000 * 60;

	public AbstractSgipSPReceiveHandler() {

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

	@Override
	protected void doBindRequest(Request request, Response response) throws IOException {
		SgipUtils.setupRequestBinding(request, true);
	}

	@Override
	protected void doBind(Request request, Response response) throws IOException {
		SgipUtils.setupRequestBinding(request, false);
		Bind bind = (Bind) SgipUtils.extractRequestPacket(request);
		BindResponse bind_resp = new BindResponse();
		SgipUtils.copySerialNumber(bind_resp, bind);

		bind_resp.result = BindResults.SUCCESS;
		if (limitClientIp && StringUtils.hasText(smgHost)) {
			if (!(CompareUtils.nullSafeEquals(smgHost, request.getRemoteAddr()) || CompareUtils.nullSafeEquals(smgHost,
					request.getRemoteHost()))) {
				bind_resp.result = BindResults.IPERROR;
				Log.warn(String.format("网关(%s)转发请求绑定不被允许", request.getRemoteAddr()));
			}
		}

		if (!(CompareUtils.nullSafeEquals(bind.user, this.account) && CompareUtils.nullSafeEquals(bind.pwd,
				this.password))) {
			bind_resp.result = BindResults.ERROR;
			Log.warn(String.format("网关(%s)转发请求绑定用户名(%s)密码(%s)错误", request.getRemoteAddr(), bind.user, bind.pwd));
		}

		SgipUtils.renderDataPacket(request, response, bind_resp);
		if (bind_resp.result == BindResults.SUCCESS) {
			SgipUtils.setupRequestBinded(request, true);
			response.flushBuffer();
		} else {
			response.finalBuffer();
		}
	}

	@Override
	protected void doDeliver(Request request, Response response) throws IOException {
		super.doDeliver(request, response);
		Deliver deliver_packet = (Deliver) SgipUtils.extractRequestPacket(request);
		if (Log.isDebugEnabled()) {
			Log.debug(deliver_packet.toString());
		}
		byte result = 9;
		try {
			doReceivedMessage(deliver_packet);
			result = 0;
		} catch (Throwable e) {
			Log.error(e.getMessage(), e);
		}
		DeliverResponse resp = new DeliverResponse();
		SgipUtils.copySerialNumber(resp, deliver_packet);
		resp.result = result;
		SgipUtils.renderDataPacket(request, response, resp);
		response.flushBuffer();
	}

	protected abstract void doReceivedMessage(Deliver deliver);

	protected abstract void doReceivedReport(Report report);

	@Override
	protected void doReport(Request request, Response response) throws IOException {
		super.doReport(request, response);
		Report report_packet = (Report) SgipUtils.extractRequestPacket(request);
		if (Log.isDebugEnabled()) {
			Log.debug(report_packet.toString());
		}
		byte result = 9;
		try {
			doReceivedReport(report_packet);
			result = 0;
		} catch (Throwable e) {
			Log.error(e.getMessage(), e);
		}
		ReportResponse resp = new ReportResponse();
		SgipUtils.copySerialNumber(resp, report_packet);
		resp.result = result;
		SgipUtils.renderDataPacket(request, response, resp);
		response.flushBuffer();
	}

	@Override
	protected void doSubmit(Request request, Response response) throws IOException {
		throw new PacketException("not support");
	}

}
