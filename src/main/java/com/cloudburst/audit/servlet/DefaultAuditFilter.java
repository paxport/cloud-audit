package com.cloudburst.audit.servlet;

import com.cloudburst.audit.Auditor;
import com.cloudburst.audit.model.AuditItem;
import com.cloudburst.audit.model.Tracking;
import com.cloudburst.audit.servlet.wrappers.AuditHttpServletRequestWrapper;
import com.cloudburst.audit.servlet.wrappers.AuditHttpServletResponseWrapper;

import org.slf4j.event.Level;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * Relies on Spring to configure
 *
 * http://stackoverflow.com/questions/32127258/how-to-autowire-bean-in-the-servlet-filters-in-spring-application
 *
 */
public class DefaultAuditFilter extends AbstractAuditFilter<AuditItem> {

    private Auditor<AuditItem> auditor;

    public DefaultAuditFilter(Auditor<AuditItem> auditor) {
        this.auditor = auditor;
    }

    private Set<String> trackingHeaderNames = trackingHeaderNames();

    protected Set<String> trackingHeaderNames(){
        Set<String> headerNames = new HashSet<>();
        headerNames.add("tracingId");
        headerNames.add("logicalSessionId");
        return headerNames;
    }

    protected Map<String,String> createTrackingMap (AuditHttpServletRequestWrapper requestWrapper) {
        return requestWrapper.getHeaders().entrySet().stream()
                .filter(e -> trackingHeaderNames.contains(e.getKey()))
                .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue(),
                        throwingMerger(), LinkedHashMap::new));
    }

    // copied from Collectors to allow us to create linked hash map
    private static <T> BinaryOperator<T> throwingMerger() {
        return (u,v) -> { throw new IllegalStateException(String.format("Duplicate key %s", u)); };
    }

    /**
     * This gets run before the servlet chain is invoked and will create a tracking map from the
     * incoming request headers (like tracingId or logicalSessionId etc) and then bind it to
     * the thread so that
     * @param requestWrapper
     * @param responseWrapper
     * @return
     */
    @Override
    protected void beforeFilterChain(AuditHttpServletRequestWrapper requestWrapper, AuditHttpServletResponseWrapper responseWrapper) {
        Map<String,String> trackingMap = createTrackingMap(requestWrapper);
        Tracking.bindTrackingMap(trackingMap);
    }

    /**
     * After the response is generated create an audit item and audit it then unbind the tracking info
     * @param requestWrapper
     * @param responseWrapper
     * @param startTime - allows you to calculate time taken or set request time
     */
    @Override
    protected void afterFilterChain(AuditHttpServletRequestWrapper requestWrapper, AuditHttpServletResponseWrapper responseWrapper, long startTime) {
        AuditItem item = createAuditItemForPair(requestWrapper,responseWrapper,startTime);
        auditor.audit(item);
        Tracking.unbindTrackingMap();
    }

    protected AuditItem createAuditItemForPair(AuditHttpServletRequestWrapper requestWrapper, AuditHttpServletResponseWrapper responseWrapper, long startTime) {
        return AuditItem.requestResponse(
                startTime,
                Level.INFO.name(),
                requestWrapper.getMethod() + " " + requestWrapper.getRequestURI(),
                "Request Headers --> " + requestWrapper.getHeaders().toString(),
                requestWrapper.getContent(),
                responseWrapper.getContent(),
                System.currentTimeMillis() - startTime
        );
    }
}
