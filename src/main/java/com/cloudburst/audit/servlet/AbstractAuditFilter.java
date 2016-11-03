package com.cloudburst.audit.servlet;

import com.cloudburst.audit.model.AuditItem;
import com.cloudburst.audit.servlet.wrappers.AuditHttpServletRequestWrapper;
import com.cloudburst.audit.servlet.wrappers.AuditHttpServletResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet Filter that wraps the http request and response and gives an opportunity to
 * do things with the wrappers before and after the filter chain
 *
 */
public abstract class AbstractAuditFilter<E> implements Filter {

    private final static Logger logger = LoggerFactory.getLogger(AbstractAuditFilter.class);

    private Set<String> includedPaths = null;

    protected Set<String> includedPaths(){
        Set<String> paths = new HashSet<>();
        return paths;
    }

    private Set<String> ensureIncludedPaths() {
        if ( includedPaths == null ) {
            includedPaths = includedPaths();
        }
        return includedPaths==null?Collections.emptySet():includedPaths;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            throw new ServletException("LoggingFilter just supports HTTP requests");
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // ignore requests for excluded paths
        for (String excludedPath : ensureIncludedPaths()) {
            String requestURI = httpRequest.getRequestURI();
            if (requestURI.startsWith(excludedPath)) {
                filterChain.doFilter(httpRequest, httpResponse);
                return;
            }
        }

        AuditHttpServletRequestWrapper requestWrapper = new AuditHttpServletRequestWrapper(httpRequest);
        AuditHttpServletResponseWrapper responseWrapper = new AuditHttpServletResponseWrapper(httpResponse);
        AuditItem requestItem = beforeFilterChain(requestWrapper,responseWrapper);
        try{
            filterChain.doFilter(requestWrapper, responseWrapper);
        }
        finally {
            afterFilterChain(requestWrapper,responseWrapper,requestItem);
        }
        try{
            httpResponse.getOutputStream().write(responseWrapper.getContentAsBytes());
        }
        catch (IllegalStateException e) {
            logger.info("having to use writer due to --> " + e.getMessage() );
            httpResponse.getWriter().write(responseWrapper.getContent());
        }
    }

    /**
     * Do stuff before the servlet chain executes like binding tracking info
     * @param requestWrapper
     * @param responseWrapper
     * @return AuditItem for request object which will get passed into the afterFilterChain method
     */
    protected abstract AuditItem beforeFilterChain(AuditHttpServletRequestWrapper requestWrapper, AuditHttpServletResponseWrapper responseWrapper);

    /**
     * Do stuff after the servlet chain like auditing the request and response
     * @param requestWrapper
     * @param responseWrapper
     * @param requestItem - link to response
     */
    protected abstract void afterFilterChain(AuditHttpServletRequestWrapper requestWrapper, AuditHttpServletResponseWrapper responseWrapper, AuditItem requestItem);

    @Override
    public void destroy() {

    }
}
