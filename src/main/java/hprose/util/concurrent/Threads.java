/**********************************************************\
|                                                          |
|                          hprose                          |
|                                                          |
| Official WebSite: http://www.hprose.com/                 |
|                   http://www.hprose.org/                 |
|                                                          |
\**********************************************************/
/**********************************************************\
 *                                                        *
 * Threads.java                                           *
 *                                                        *
 * Threads class for Java.                                *
 *                                                        *
 * LastModified: Apr 13, 2016                             *
 * Author: Ma Bingyao <andot@hprose.com>                  *
 *                                                        *
\**********************************************************/
package hprose.util.concurrent;

public final class Threads {

    private static final ThreadGroup rootThreadGroup;
    private static final Thread mainThread;
    private static volatile boolean enableShutdownHandler = true;
    private static volatile Runnable defaultHandler = null;

    static {
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup parentThreadGroup;
        while ((parentThreadGroup = threadGroup.getParent()) != null) {
            threadGroup = parentThreadGroup;
        }
        rootThreadGroup = threadGroup;

        Thread thread = Thread.currentThread();
        Thread[] threads = findAllThreads();
        for (Thread t: threads) {
            if (t.getId() == 1) {
                thread = t;
            }
        }
        mainThread = thread;
    }

    public static Thread[] findAllThreads() {
        int estimatedSize = rootThreadGroup.activeCount() * 2;
        Thread[] slackList = new Thread[estimatedSize];
        int actualSize = rootThreadGroup.enumerate(slackList);
        Thread[] list = new Thread[actualSize];
        System.arraycopy(slackList, 0, list, 0, actualSize);
        return list;
    }

    public static Thread getMainThread() {
        return mainThread;
    }

    public static ThreadGroup getRootThreadGroup() {
        return rootThreadGroup;
    }

    public static synchronized void registerShutdownHandler(final Runnable handler) {
        if (enableShutdownHandler) {
            if (defaultHandler == null) {
                defaultHandler = handler;
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        defaultHandler.run();
                    }
                });
                new Thread() {
                    private final Object o = new Object();
                    @Override
                    public void run() {
                        for (;;) {
                            if (!mainThread.isAlive()) {
                                System.exit(0);
                                break;
                            }
                            else {
                                synchronized (o) {
                                    try {
                                        o.wait(100);
                                    }
                                    catch (InterruptedException e) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }.start();
            }
            else {
                final Runnable oldHandler = defaultHandler;
                defaultHandler = new Runnable() {
                    public void run() {
                        oldHandler.run();
                        handler.run();
                    }
                };
            }
        }
    }

    public static void run() {
        defaultHandler.run();
    }

    public static void disabledShutdownHandler() {
        enableShutdownHandler = false;
    }
}
