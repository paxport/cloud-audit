package com.cloudburst.audit;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class BackgroundAuditor<E> implements Auditor<E>, InitializingBean, DisposableBean {

    private final static Logger logger = LoggerFactory.getLogger(BackgroundAuditor.class);

    @Value("${audit.background.coreThreads:2}")
    private int coreThreads = 2;

    @Value("${audit.background.maxThreads:64}")
    private int maxThreads = 64;

    @Value("${audit.background.keepAliveSecs:60}")
    private int keepAliveSecs = 60;

    @Value("${audit.background.maxQueueSize:256}")
    private int maxQueueSize = 256;

    private ExecutorService executor;

    @Autowired
    private Auditor<E> delegate;

    protected ExecutorService createExecutor() {
        return new ThreadPoolExecutor(coreThreads,maxThreads,keepAliveSecs, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(maxQueueSize), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Override
    public void audit(E item) {
        executor.execute(() -> delegate.audit(item));
    }

    @Override
    public void destroy() throws Exception {
        try {
            logger.info("attempt to shutdown executor");
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            logger.warn("tasks interrupted",e);
        }
        finally {
            if (!executor.isTerminated()) {
                logger.warn("cancel non-finished tasks");
            }
            executor.shutdownNow();
            logger.info("shutdown finished");
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        executor = createExecutor();
    }
}
