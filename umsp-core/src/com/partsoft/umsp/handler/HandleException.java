package com.partsoft.umsp.handler;

import java.util.Date;

import com.partsoft.umsp.UmspException;
import com.partsoft.utils.CalendarUtils;

/**
 * @brief 处理异常
 * @author neeker
 */
public class HandleException extends UmspException {

	private static final long serialVersionUID = -1870326620030346018L;

	private Object handleObject;
	
	private Date exceptionTime = CalendarUtils.now();

	public Object getHandleObject() {
		return handleObject;
	}

	public HandleException(String message, Throwable cause, Object transmitObject) {
		super(message, cause);
		this.handleObject = transmitObject;
	}

	public HandleException(int errorCode, Throwable cause, Object transmitObject) {
		super(errorCode, cause);
		this.handleObject = transmitObject;
	}

	public HandleException(Throwable cause, Object transmitObject) {
		super(cause);
		this.handleObject = transmitObject;
	}

	public HandleException(Object transmitObject) {
		super();
		this.handleObject = transmitObject;
	}
	
	public Date getExceptionTime() {
		return exceptionTime;
	}

	@Override
	public void finalize() {
		super.finalize();
		this.handleObject = null;
	}

	public HandleException(String message, Object transmitObject) {
		super(message);
		this.handleObject = transmitObject;
	}

}
