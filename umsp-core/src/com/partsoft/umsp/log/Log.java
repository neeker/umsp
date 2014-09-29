package com.partsoft.umsp.log;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.partsoft.utils.Loader;

public class Log {
	private static final String[] __nestedEx = { "getTargetException", "getTargetError", "getException", "getRootCause" };
	
	private static final Class<?>[] __noArgs = new Class[0];

	public final static String EXCEPTION = "EXCEPTION ";
	public final static String IGNORED = "IGNORED";
	public final static String IGNORED_FMT = "IGNORED: {}";
	public final static String NOT_IMPLEMENTED = "NOT IMPLEMENTED ";

	public static String __logClass;
	public static boolean __verbose;
	public static boolean __ignored;

	private static Logger __log;

	static {
		AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
			public Boolean run() {
				__logClass = System.getProperty("com.partsoft.umsp.log.class", "com.partsoft.umsp.log.Slf4jLog");
				__verbose = System.getProperty("VERBOSE", null) != null;
				__ignored = System.getProperty("IGNORED", null) != null;
				return new Boolean(true);
			}
		});

		Class<?> log_class = null;
		try {
			log_class = Loader.loadClass(Log.class, __logClass);
			__log = (Logger) log_class.newInstance();
		} catch (Throwable e) {
			if (__verbose)
				e.printStackTrace();
		}
		if (__log.isDebugEnabled()) {
			__log.debug(String.format("Logging to %s via %s", __log, log_class.getName()));
		}
	}

	public static void setLog(Logger log) {
		Log.__log = log;
	}

	public static Logger getLog() {
		return __log;
	}

	public static void debug(Throwable th) {
		if (__log == null || !isDebugEnabled())
			return;
		__log.debug(EXCEPTION, th);
		unwind(th);
	}

	public static void debug(String msg) {
		if (__log == null)
			return;
		__log.debug(msg, null);
	}

	public static void debug(String msg, Throwable arg) {
		if (__log == null)
			return;
		__log.debug(msg, arg);
	}

	public static void ignore(Throwable th) {
		if (__log == null)
			return;
		if (__ignored) {
			__log.warn(IGNORED, th);
			unwind(th);
		} else if (__verbose) {
			__log.debug(IGNORED, th);
			unwind(th);
		}
	}

	public static void info(String msg) {
		if (__log == null)
			return;
		__log.info(msg);
	}
	

	public static void info(String msg, Throwable arg) {
		if (__log == null)
			return;
		__log.info(msg, arg);
	}
	
	public static void error(String msg) {
		if (__log == null)
			return;
		__log.error(msg);
	}
	
	public static void error(Throwable arg) {
		if (__log == null)
			return;
		__log.error(arg);
	}
	
	public static void error(String msg, Throwable arg) {
		if (__log == null)
			return;
		__log.error(msg, arg);
	}

	public static boolean isDebugEnabled() {
		if (__log == null)
			return false;
		return __log.isDebugEnabled();
	}

	public static void warn(String msg) {
		if (__log == null)
			return;
		__log.warn(msg);
	}

	public static void warn(String msg, Throwable th) {
		if (__log == null)
			return;
		__log.warn(msg, th);
		unwind(th);
	}

	public static void warn(Throwable th) {
		if (__log == null)
			return;
		__log.warn(EXCEPTION, th);
		unwind(th);
	}

	/**
	 * Obtain a named Logger. Obtain a named Logger or the default Logger if
	 * null is passed.
	 */
	public static Logger getLogger(String name) {
		if (__log == null)
			return __log;
		if (name == null)
			return __log;
		return __log.getLogger(name);
	}

	private static void unwind(Throwable th) {
		if (th == null)
			return;
		for (int i = 0; i < __nestedEx.length; i++) {
			try {
				Method get_target = th.getClass().getMethod(__nestedEx[i], __noArgs);
				Throwable th2 = (Throwable) get_target.invoke(th, (Object[]) null);
				if (th2 != null && th2 != th)
					warn("Nested in " + th + ":", th2);
			} catch (Exception ignore) {
			}
		}
	}

}
