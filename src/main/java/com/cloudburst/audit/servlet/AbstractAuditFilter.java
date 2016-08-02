package com.cloudburst.audit.servlet;

import com.cloudburst.audit.servlet.wrappers.AuditHttpServletRequestWrapper;
import com.cloudburst.audit.servlet.wrappers.AuditHttpServletResponseWrapper;

import java.io.IOException;
import java.util.Collections;
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

    private Set<String> excludedPaths = Collections.emptySet();

    public Set<String> getExcludedPaths() {
        return excludedPaths;
    }

    public AbstractAuditFilter setExcludedPaths(Set<String> excludedPaths) {
        this.excludedPaths = excludedPaths;
        return this;
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

        for (String excludedPath : excludedPaths) {
            String requestURI = httpRequest.getRequestURI();
            if (requestURI.startsWith(excludedPath)) {
                filterChain.doFilter(httpRequest, httpResponse);
                return;
            }
        }

        AuditHttpServletRequestWrapper requestWrapper = new AuditHttpServletRequestWrapper(httpRequest);
        AuditHttpServletResponseWrapper responseWrapper = new AuditHttpServletResponseWrapper(httpResponse);

        E context = beforeFilterChain(requestWrapper,responseWrapper);
        try{
            filterChain.doFilter(requestWrapper, responseWrapper);
        }
        finally {
            afterFilterChain(requestWrapper,responseWrapper,context);
        }
        httpResponse.getOutputStream().write(responseWrapper.getContentAsBytes());
    }

    protected abstract E beforeFilterChain(AuditHttpServletRequestWrapper requestWrapper, AuditHttpServletResponseWrapper responseWrapper);

    protected abstract void afterFilterChain(AuditHttpServletRequestWrapper requestWrapper, AuditHttpServletResponseWrapper responseWrapper, E context);


    @Override
    public void destroy() {

    }
}
