package com.partsoft.umsp.log;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Slf4jLog implements Logger {
	
	private Log logger;

	public Slf4jLog() throws Exception {
		this("com.partsoft.umsp.log");
	}

	public Slf4jLog(String name) {
		logger = LogFactory.getLog(name);
	}

	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	/* ------------------------------------------------------------ */
	public Logger getLogger(String name) {
		return new Slf4jLog(name);
	}

	/* ------------------------------------------------------------ */
	public String toString() {
		return logger.toString();
	}
	
	

	public void debug(Object arg0, Throwable arg1) {
		logger.debug(arg0, arg1);
	}

	public void debug(Object arg0) {
		logger.debug(arg0);
	}

	public void error(Object arg0, Throwable arg1) {
		logger.error(arg0, arg1);
	}

	public void error(Object arg0) {
		logger.error(arg0);
	}

	public void fatal(Object arg0, Throwable arg1) {
		logger.fatal(arg0, arg1);
	}

	public void fatal(Object arg0) {
		logger.fatal(arg0);
	}

	public void info(Object arg0, Throwable arg1) {
		logger.info(arg0, arg1);
	}

	public void info(Object arg0) {
		logger.info(arg0);
	}

	public boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}

	public boolean isFatalEnabled() {
		return logger.isFatalEnabled();
	}

	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	public boolean isTraceEnabled() {
		return logger.isTraceEnabled();
	}

	public boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

	public void trace(Object arg0, Throwable arg1) {
		logger.trace(arg0, arg1);
	}

	public void trace(Object arg0) {
		logger.trace(arg0);
	}

	public void warn(Object arg0, Throwable arg1) {
		logger.warn(arg0, arg1);
	}

	public void warn(Object arg0) {
		logger.warn(arg0);
	}

	/* ------------------------------------------------------------ */
	public void setDebugEnabled(boolean enabled) {
		warn("setDebugEnabled not implemented");
	}
}