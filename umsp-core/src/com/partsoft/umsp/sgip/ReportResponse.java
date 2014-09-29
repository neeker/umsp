package com.partsoft.umsp.sgip;

import com.partsoft.umsp.sgip.Constants.Commands;

public class ReportResponse extends ResponsePacket {
	
	private static final long serialVersionUID = 0x80000005L;

	public ReportResponse() {
		super(Commands.REPORT_RESPONSE);
	}

	@Override
	public ReportResponse clone() {
		return (ReportResponse) super.clone();
	}

}
