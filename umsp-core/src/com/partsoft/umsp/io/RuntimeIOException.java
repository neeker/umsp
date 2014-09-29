package com.partsoft.umsp.io;

/* ------------------------------------------------------------ */
/**
 * Subclass of {@link java.lang.RuntimeException} used to signal that there
 * was an {@link java.io.IOException} thrown by underlying {@link java.io.Writer}
 */
@SuppressWarnings("serial")
public class RuntimeIOException extends RuntimeException
{
    public RuntimeIOException()
    {
        super();
    }

    public RuntimeIOException(String message)
    {
        super(message);
    }

    public RuntimeIOException(Throwable cause)
    {
        super(cause);
    }

    public RuntimeIOException(String message, Throwable cause)
    {
        super(message,cause);
    }
}
