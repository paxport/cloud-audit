package com.cloudburst.audit.servlet;

import com.cloudburst.audit.Auditor;
import com.cloudburst.audit.model.AuditItem;

import org.apache.commons.collections4.IteratorUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class DefaultAuditFilterTest {

    private MockAuditor mockAuditor = new MockAuditor();

    @Mock
    private HttpServletRequest mockHttpServletRequest;

    @Mock
    private HttpServletResponse mockHttpServletResponse;

    private DefaultAuditFilter filter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        filter = new DefaultAuditFilter(mockAuditor){
            @Override
            protected Set<String> excludedPaths() {
                return Collections.singleton("/_ah");
            }
        };
    }

    @Test
    public void testGetIsAudited() throws IOException, ServletException {

        // mock out request

        when(mockHttpServletRequest.getMethod()).thenReturn("GET");
        when(mockHttpServletRequest.getRequestURI()).thenReturn("/foo");

        Map<String,String> headers = new HashMap<>();

        Enumeration<String> headerNames = IteratorUtils.asEnumeration(headers.keySet().iterator());
        when(mockHttpServletRequest.getHeaderNames()).thenReturn(headerNames);

        String requestBody = "";
        when(mockHttpServletRequest.getInputStream()).thenReturn(new MockServletInputStream(requestBody));

        MockServletOutputStream mockServletOutputStream = new MockServletOutputStream();
        when(mockHttpServletResponse.getOutputStream()).thenReturn(mockServletOutputStream);

        FilterChain filterChain = (req, res) -> {
            res.getOutputStream().println("Line one");
            res.getOutputStream().println("Line two");
            res.getOutputStream().println("Line three");
            //res.getOutputStream().close();
        };

        filter.doFilter(mockHttpServletRequest, mockHttpServletResponse, filterChain);

        List<AuditItem> items = mockAuditor.getItems();
        assertEquals(1,items.size());

        AuditItem item = mockAuditor.getItems().get(0);

        assertEquals("RRPAIR",item.getType());
        assertEquals("INFO",item.getLevel());
        assertEquals("GET /foo",item.getModule());
        assertEquals("",item.getMessage());
        assertEquals("[EMPTY]",item.getRequest().get());
        assertEquals("Line one\r\nLine two\r\nLine three\r\n",item.getResponse().get());
    }

    @Test
    public void testPostIsAudited() throws IOException, ServletException {

        // mock out request

        when(mockHttpServletRequest.getMethod()).thenReturn("POST");
        when(mockHttpServletRequest.getRequestURI()).thenReturn("/foo");

        Map<String,String> headers = new LinkedHashMap<>();
        headers.put("Content-Type","text/plain");
        headers.put("logicalSessionId","12345");

        when(mockHttpServletRequest.getHeaderNames())
                .thenAnswer(i -> IteratorUtils.asEnumeration(headers.keySet().iterator()));
        when(mockHttpServletRequest.getHeader(anyString()))
                .thenAnswer(i -> headers.get(i.getArgument(0)));

        String requestBody = "Line One of Request.\r\nLine two of request.\r\n";
        when(mockHttpServletRequest.getInputStream()).thenReturn(new MockServletInputStream(requestBody));

        MockServletOutputStream mockServletOutputStream = new MockServletOutputStream();
        when(mockHttpServletResponse.getOutputStream()).thenReturn(mockServletOutputStream);

        FilterChain filterChain = (req, res) -> {
            res.getOutputStream().println("Line one");
            res.getOutputStream().println("Line two");
            res.getOutputStream().println("Line three");
            //res.getOutputStream().close();
        };

        filter.doFilter(mockHttpServletRequest, mockHttpServletResponse, filterChain);

        List<AuditItem> items = mockAuditor.getItems();
        assertEquals(1,items.size());

        AuditItem item = mockAuditor.getItems().get(0);

        assertEquals("RRPAIR",item.getType());
        assertEquals("INFO",item.getLevel());
        assertEquals("POST /foo",item.getModule());
        assertEquals("Content-Type: text/plain\nlogicalSessionId: 12345",item.getMessage());
        assertEquals("Line One of Request.\r\nLine two of request.\r\n",item.getRequest().get());
        assertEquals("Line one\r\nLine two\r\nLine three\r\n",item.getResponse().get());
    }

    @Test
    public void testExcluded() throws IOException, ServletException {

        // mock out request

        when(mockHttpServletRequest.getMethod()).thenReturn("GET");
        when(mockHttpServletRequest.getRequestURI()).thenReturn("/_ah/health");

        Map<String,String> headers = new HashMap<>();

        Enumeration<String> headerNames = IteratorUtils.asEnumeration(headers.keySet().iterator());
        when(mockHttpServletRequest.getHeaderNames()).thenReturn(headerNames);

        String requestBody = "";
        when(mockHttpServletRequest.getInputStream()).thenReturn(new MockServletInputStream(requestBody));

        MockServletOutputStream mockServletOutputStream = new MockServletOutputStream();
        when(mockHttpServletResponse.getOutputStream()).thenReturn(mockServletOutputStream);

        FilterChain filterChain = (req, res) -> {
            res.getOutputStream().println("Line one");
            res.getOutputStream().println("Line two");
            res.getOutputStream().println("Line three");
            //res.getOutputStream().close();
        };

        filter.doFilter(mockHttpServletRequest, mockHttpServletResponse, filterChain);

        List<AuditItem> items = mockAuditor.getItems();
        assertEquals(0,items.size());
    }

    private static class MockServletInputStream extends ServletInputStream {

        private ByteArrayInputStream delegate;

        public MockServletInputStream (String contents) {
            delegate = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }
    }

    private static class MockServletOutputStream extends ServletOutputStream {

        private ByteArrayOutputStream delegate = new ByteArrayOutputStream();

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
        }

        public String getContents(){
            return delegate.toString();
        }
    }

    private static class MockAuditor implements Auditor<AuditItem> {

        private List<AuditItem> items = new ArrayList<>();

        @Override
        public void audit(AuditItem item) {
            items.add(item);
        }

        public List<AuditItem> getItems(){
            return items;
        }
    }

}
