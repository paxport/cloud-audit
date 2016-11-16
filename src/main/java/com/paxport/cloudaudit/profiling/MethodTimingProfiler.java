package com.paxport.cloudaudit.profiling;

import com.paxport.cloudaudit.AuditorSingleton;
import com.paxport.cloudaudit.model.AuditItem;
import com.paxport.cloudaudit.model.AuditItemType;
import com.paxport.cloudaudit.model.Tracking;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

/**
 * Subclass this as an @Aspect and add a point cut like
 *
 * @Around("anyPublicMethod() && profilingIsOkay()")
 * public Object profile(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
 *   return super.profile(proceedingJoinPoint);
 * }
 *
 * Add @NoProfiling annotation to any class that you have problems with
 *
 */
public abstract class MethodTimingProfiler {

    private final static Logger logger = LoggerFactory.getLogger(MethodTimingProfiler.class);

    @Pointcut("execution(public * *(..))")
    protected void anyPublicMethod() {}

    @Pointcut("!@within(NoProfiling)")
    protected void profilingIsOkay() {}

    // implement your point cut
    protected abstract void myPointcut();

    private String moduleName = moduleName();

    @Around("myPointcut()")
    public Object profile(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start(proceedingJoinPoint.toShortString());

        boolean isExceptionThrown = false;
        try {
            // execute the profiled method
            return proceedingJoinPoint.proceed();
        } catch (RuntimeException e) {
            isExceptionThrown = true;
            throw e;
        } finally {
            stopWatch.stop();
            StopWatch.TaskInfo taskInfo = stopWatch.getLastTaskInfo();
            // Log the method's profiling result
            String profileMessage = taskInfo.getTaskName() + ": " + taskInfo.getTimeMillis() + " ms" +
                    (isExceptionThrown ? " (thrown Exception)" : "");
            if ( logger.isDebugEnabled() ) {
                logger.debug(profileMessage);
            }
            if (AuditorSingleton.isPopulated() && Tracking.isBound()) {
                AuditItem item = AuditItem.builder()
                        .type(AuditItemType.LOG)
                        .level("DEBUG")
                        .millisTaken(taskInfo.getTimeMillis())
                        .label(taskInfo.getTaskName())
                        .module(moduleName)
                        .build();
                AuditorSingleton.getInstance().audit(item);
            }
        }
    }

    protected String moduleName() {
        String cls = this.getClass().getSimpleName();
        int pos = cls.indexOf("$$");
        if ( pos > -1 ) {
            cls = cls.substring(0,pos);
        }
        return cls;
    }
}
