package com.partsoft.umsp.thread;

public interface ThreadPool
{
    public abstract boolean dispatch(Runnable job);

    public void join() throws InterruptedException;

    public int getThreads();

    public int getIdleThreads();
    
    public boolean isLowOnThreads();
    
}
