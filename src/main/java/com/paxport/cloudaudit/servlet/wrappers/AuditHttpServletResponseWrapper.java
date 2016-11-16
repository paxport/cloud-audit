package com.paxport.cloudaudit.servlet.wrappers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AuditHttpServletResponseWrapper extends HttpServletResponseWrapper {

    private final AuditServletOutpuStream auditServletOutpuStream = new AuditServletOutpuStream();

    private final HttpServletResponse delegate;

    public AuditHttpServletResponseWrapper(HttpServletResponse response) {
        super(response);
        delegate = response;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return auditServletOutpuStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(auditServletOutpuStream.baos);
    }

    public String getContent() {
        try {
            String responseEncoding = delegate.getCharacterEncoding();
            return auditServletOutpuStream.baos.toString(responseEncoding != null ? responseEncoding : UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("failed to get content from response wrapper",e);
        }
    }

    public byte[] getContentAsBytes() {
        return auditServletOutpuStream.baos.toByteArray();
    }

    private class AuditServletOutpuStream extends ServletOutputStream {

        private ByteArrayOutputStream baos = new ByteArrayOutputStream();

        @Override
        public void write(int b) throws IOException {
            baos.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            baos.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            baos.write(b, off, len);
        }

    }


}