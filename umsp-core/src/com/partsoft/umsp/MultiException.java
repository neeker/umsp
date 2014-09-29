package com.partsoft.umsp;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

import com.partsoft.utils.ListUtils;

public class MultiException extends UmspException {

	private static final long serialVersionUID = -6360840088454185883L;

	private Object nested;

	public MultiException() {
		super("Multiple exceptions");
	}

	public void add(Throwable e) {
		if (e instanceof MultiException) {
			MultiException me = (MultiException) e;
			for (int i = 0; i < ListUtils.size(me.nested); i++)
				nested = ListUtils.add(nested, ListUtils.get(me.nested, i));
		} else
			nested = ListUtils.add(nested, e);
	}

	public int size() {
		return ListUtils.size(nested);
	}

	@SuppressWarnings("unchecked")
	public List<? extends Throwable> getThrowables() {
		return ListUtils.getList(nested);
	}

	public Throwable getThrowable(int i) {
		return (Throwable) ListUtils.get(nested, i);
	}

	public void ifExceptionThrow() throws Exception {
		switch (ListUtils.size(nested)) {
		case 0:
			break;
		case 1:
			Throwable th = (Throwable) ListUtils.get(nested, 0);
			if (th instanceof Error)
				throw (Error) th;
			if (th instanceof Exception)
				throw (Exception) th;
		default:
			throw this;
		}
	}

	public void ifExceptionThrowRuntime() throws Error {
		switch (ListUtils.size(nested)) {
		case 0:
			break;
		case 1:
			Throwable th = (Throwable) ListUtils.get(nested, 0);
			if (th instanceof Error)
				throw (Error) th;
			else if (th instanceof RuntimeException)
				throw (RuntimeException) th;
			else
				throw new RuntimeException(th);
		default:
			throw new RuntimeException(this);
		}
	}

	public void ifExceptionThrowMulti() throws MultiException {
		if (ListUtils.size(nested) > 0)
			throw this;
	}

	public String toString() {
		if (ListUtils.size(nested) > 0)
			return "com.partsoft.umsp.MultiException" + ListUtils.getList(nested);
		return "com.partsoft.umsp.MultiException[]";
	}

	public void printStackTrace() {
		super.printStackTrace();
		for (int i = 0; i < ListUtils.size(nested); i++)
			((Throwable) ListUtils.get(nested, i)).printStackTrace();
	}

	public void printStackTrace(PrintStream out) {
		super.printStackTrace(out);
		for (int i = 0; i < ListUtils.size(nested); i++)
			((Throwable) ListUtils.get(nested, i)).printStackTrace(out);
	}

	public void printStackTrace(PrintWriter out) {
		super.printStackTrace(out);
		for (int i = 0; i < ListUtils.size(nested); i++)
			((Throwable) ListUtils.get(nested, i)).printStackTrace(out);
	}

}
