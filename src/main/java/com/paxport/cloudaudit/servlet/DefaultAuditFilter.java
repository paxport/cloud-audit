package com.paxport.cloudaudit.servlet;

import com.google.api.client.json.jackson2.JacksonFactory;

import com.paxport.cloudaudit.AuditorSingleton;
import com.paxport.cloudaudit.BackgroundAuditor;
import com.paxport.cloudaudit.model.AuditItem;
import com.paxport.cloudaudit.model.ImmutableAuditItem;
import com.paxport.cloudaudit.model.Tracking;
import com.paxport.cloudaudit.servlet.wrappers.AuditHttpServletRequestWrapper;
import com.paxport.cloudaudit.servlet.wrappers.AuditHttpServletResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.HashSet;
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
 * By default will look for tracing_id and logical_session_id headers and will bind them
 * into the trackingMap along with a generated request_id.
 *
 */
public class DefaultAuditFilter extends AbstractAuditFilter<AuditItem> implements InitializingBean {

    private final static Logger logger = LoggerFactory.getLogger(DefaultAuditFilter.class);
    public static final String TRACING_ID = "tracing_id";
    public static final String LOGICAL_SESSION_ID = "logical_session_id";
    public static final String REQUEST_ID = "request_id";

    @Autowired
    private BackgroundAuditor<AuditItem> auditor;

    public DefaultAuditFilter() {
    }

    public DefaultAuditFilter(BackgroundAuditor<AuditItem> backgroundAuditor) {
        this.auditor = backgroundAuditor;
    }

    private Set<String> trackingHeaderNames = trackingHeaderNames();

    private JacksonFactory jsonFactory = new JacksonFactory();

    protected Set<String> trackingHeaderNames(){
        Set<String> headerNames = new HashSet<>();
        headerNames.add(TRACING_ID);
        headerNames.add(LOGICAL_SESSION_ID);
        return headerNames;
    }

    protected Map<String,String> createTrackingMap (AuditHttpServletRequestWrapper requestWrapper) {
        Map<String,String> result = requestWrapper.getHeaders().entrySet().stream()
                .filter(e -> trackingHeaderNames.contains(e.getKey()))
                .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue(),
                        throwingMerger(), LinkedHashMap::new));
        // add in new request id for just this request
        String guid = UUID.randomUUID().toString();
        result.put(REQUEST_ID, guid);
        // propogate tracing id
        if ( !result.containsKey(TRACING_ID) ) {
            result.put(TRACING_ID,guid);
        }
        return result;
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

        // create request item but don't audit yet in case tracking info is missing
        AuditItem requestItem = createAuditItemForRequest(requestWrapper);
        return requestItem;
    }

    protected AuditItem createAuditItemForRequest(AuditHttpServletRequestWrapper requestWrapper) {
        return AuditItem.request(
                url(requestWrapper),
                module(requestWrapper),
                requestWrapper.getMethod(),
                headers(requestWrapper.getHeaders()),
                requestWrapper.getContent(),
                requestWrapper.getContentType()
                );
    }

    protected String url(AuditHttpServletRequestWrapper requestWrapper) {
        String url = requestWrapper.getRequestURL().toString();
        String queryString = requestWrapper.getQueryString();
        if ( queryString != null ) {
            url += "?" + queryString;
        }
        return url;
    }

    protected String module(AuditHttpServletRequestWrapper requestWrapper) {
        return requestWrapper.getRequestURI();
    }

    /**
     * After the response is generated create an audit item and audit it then unbind the tracking info
     * @param requestWrapper
     * @param responseWrapper
     * @param requestItem - use request guid to set seriesGuid on response item so they can be tied together
     */
    @Override
    protected void afterFilterChain(AuditHttpServletRequestWrapper requestWrapper, AuditHttpServletResponseWrapper responseWrapper, AuditItem requestItem) {
        if ( requestItem != null ) {
            if (requestItem.getTracking().isEmpty() ){
                // this means that the servlet in play could bind extra tracking info
                // and we will pick it up before auditing the request and response
                Map<String,String> updatedTrackingMap = Tracking.getTrackingMap();
                if ( !updatedTrackingMap.isEmpty() ){
                    requestItem = ImmutableAuditItem.builder()
                            .from(requestItem)
                            .tracking(updatedTrackingMap)
                            .build();
                }
            }
            auditor.audit(requestItem);
        }
        AuditItem responseItem = createAuditItemForResponse(requestWrapper,responseWrapper,requestItem);
        if ( responseItem != null ) {
            auditor.audit(responseItem);
        }
        Tracking.unbindTrackingMap();
    }

    protected AuditItem createAuditItemForResponse(AuditHttpServletRequestWrapper requestWrapper, AuditHttpServletResponseWrapper responseWrapper, AuditItem requestItem) {
        return AuditItem.response(
                requestItem,
                url(requestWrapper),
                module(requestWrapper),
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

    @Override
    public void afterPropertiesSet() throws Exception {
        AuditorSingleton.setInstance(auditor);
    }
}
