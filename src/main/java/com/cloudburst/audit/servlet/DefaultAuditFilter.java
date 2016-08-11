package com.cloudburst.audit.servlet;

import com.google.api.client.json.jackson2.JacksonFactory;

import com.cloudburst.audit.Auditor;
import com.cloudburst.audit.AuditorSingleton;
import com.cloudburst.audit.BackgroundAuditor;
import com.cloudburst.audit.model.AuditItem;
import com.cloudburst.audit.model.Tracking;
import com.cloudburst.audit.servlet.wrappers.AuditHttpServletRequestWrapper;
import com.cloudburst.audit.servlet.wrappers.AuditHttpServletResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * Relies on Spring to configure
 *
 * http://stackoverflow.com/questions/32127258/how-to-autowire-bean-in-the-servlet-filters-in-spring-application
 *
 */
public class DefaultAuditFilter extends AbstractAuditFilter<AuditItem> {

    private final static Logger logger = LoggerFactory.getLogger(DefaultAuditFilter.class);

    private Auditor<AuditItem> auditor;

    /**
     * Prefer background auditor to avoid adding latency to req/res
     * @param auditor
     */
    public DefaultAuditFilter(BackgroundAuditor<AuditItem> auditor) {
        this.auditor = auditor;
        // set Logback Appender Auditor in case it is in use
        AuditorSingleton.setInstance(auditor);
    }

    private Set<String> trackingHeaderNames = trackingHeaderNames();

    private JacksonFactory jsonFactory = new JacksonFactory();

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
    protected AuditItem beforeFilterChain(AuditHttpServletRequestWrapper requestWrapper, AuditHttpServletResponseWrapper responseWrapper) {
        if ( logger.isDebugEnabled() ){
            logger.debug("new request being audited: " + requestWrapper.getRequestURI() + "?" + requestWrapper.getQueryString() );
        }
        // need to call this otherwise it won't get populated
        requestWrapper.getContent();
        Map<String,String> trackingMap = createTrackingMap(requestWrapper);
        Tracking.bindTrackingMap(trackingMap);

        // return tracking details in response
        trackingMap.entrySet().forEach(e -> responseWrapper.addHeader(e.getKey(),e.getValue()));

        AuditItem requestItem = createAuditItemForRequest(requestWrapper);
        if ( requestItem != null ) {
            auditor.audit(requestItem);
        }
        return requestItem;
    }

    protected AuditItem createAuditItemForRequest(AuditHttpServletRequestWrapper requestWrapper) {
        return AuditItem.request(
                requestWrapper.getRequestURL().toString(),
                this.getClass().getSimpleName(),
                requestWrapper.getMethod(),
                headers(requestWrapper.getHeaders()),
                requestWrapper.getContent(),
                requestWrapper.getContentType()
                );
    }

    /**
     * After the response is generated create an audit item and audit it then unbind the tracking info
     * @param requestWrapper
     * @param responseWrapper
     * @param requestItem - use request guid to set seriesGuid on response item so they can be tied together
     */
    @Override
    protected void afterFilterChain(AuditHttpServletRequestWrapper requestWrapper, AuditHttpServletResponseWrapper responseWrapper, AuditItem requestItem) {
        AuditItem responseItem = createAuditItemForResponse(requestWrapper,responseWrapper,requestItem);
        if ( responseItem != null ) {
            auditor.audit(responseItem);
        }
        Tracking.unbindTrackingMap();
    }

    protected AuditItem createAuditItemForResponse(AuditHttpServletRequestWrapper requestWrapper, AuditHttpServletResponseWrapper responseWrapper, AuditItem requestItem) {
        return AuditItem.response(
                requestItem,
                requestWrapper.getRequestURL().toString(),
                this.getClass().getSimpleName(),
                requestWrapper.getMethod(),
                null,
                responseWrapper.getContent(),
                responseWrapper.getContentType()
                );
    }

    /**
     * Encode headers as JSON
     */
    protected String headers(Map<String,String> headers){
        try {
            return jsonFactory.toString(headers);
        } catch (IOException e) {
            logger.error("headers", e);
            return "error encoding headers: " + e.getMessage();
        }
    }

}
