package com.cloudburst.audit.servlet;

import com.cloudburst.audit.Auditor;
import com.cloudburst.audit.model.AuditItem;
import com.cloudburst.audit.model.Tracking;
import com.cloudburst.audit.servlet.wrappers.AuditHttpServletRequestWrapper;
import com.cloudburst.audit.servlet.wrappers.AuditHttpServletResponseWrapper;

import org.slf4j.event.Level;

import java.util.Map;
import java.util.UUID;

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

    @Override
    protected AuditItem beforeFilterChain(AuditHttpServletRequestWrapper requestWrapper, AuditHttpServletResponseWrapper responseWrapper) {
        AuditItem item = createAuditItemFromRequest(requestWrapper);
        // this will bind the tracking info from the request headers into a thread local
        Tracking.bind(item);
        return item;
    }

    protected AuditItem createAuditItemFromRequest(AuditHttpServletRequestWrapper requestWrapper) {
        AuditItem item = AuditItem.requestResponse(Level.INFO.name(),
                    this.getClass().getSimpleName(),
                    "Request Headers --> " + requestWrapper.getHeaders().toString(),
                    requestWrapper.getContent(),
                    null,
                    System.currentTimeMillis()
                );
        Map<String,String> trackingHeaders = mapTrackingHeaders(requestWrapper);
        ensureRequestId(trackingHeaders);
        return AuditItem.withTrackingInfo(item,trackingHeaders);
    }

    /**
     * Entry point so generate a requestId if none is found in the headers
     */
    protected void ensureRequestId(Map<String, String> trackingHeaders) {
        if ( !trackingHeaders.containsKey("requestId") ) {
            trackingHeaders.put("requestId", UUID.randomUUID().toString());
        }
    }

    /**
     * Opportunity to convert real headers into those expected in AuditItem which are:
     * principal,requestId,tracingId,sessionId and correlationId
     * all are optional
     */
    protected Map<String,String> mapTrackingHeaders(AuditHttpServletRequestWrapper requestWrapper) {
        return requestWrapper.getHeaders();
    }

    @Override
    protected void afterFilterChain(AuditHttpServletRequestWrapper requestWrapper, AuditHttpServletResponseWrapper responseWrapper, AuditItem requestItem) {
        AuditItem item = createAuditItemForPair(requestItem,responseWrapper);
        auditor.audit(item);
        Tracking.unbind();
    }

    protected AuditItem createAuditItemForPair(AuditItem requestItem, AuditHttpServletResponseWrapper responseWrapper) {
        return AuditItem.from(requestItem)
                .millisTaken(System.currentTimeMillis() - requestItem.getMillisTaken().get())
                .response(responseWrapper.getContent())
                .build();
    }
}
