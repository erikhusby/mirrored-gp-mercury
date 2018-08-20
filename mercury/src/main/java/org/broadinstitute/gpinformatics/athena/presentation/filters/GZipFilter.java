package org.broadinstitute.gpinformatics.athena.presentation.filters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

/**
 * This filter is used for compressing streams for web requests.  Typically used for more static content since most of
 * the time spent for page HTML is the dynamic creation of the page -- not downloading the few KB of HTML text in it.
 */
public class GZipFilter implements Filter {
    private static final Log log = LogFactory.getLog(GZipFilter.class);

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String acceptEncoding = httpRequest.getHeader(HttpHeaders.ACCEPT_ENCODING);
        if (acceptEncoding != null) {
            if (acceptEncoding.contains("gzip")) {
                log.trace(String.format("Compressing %s", httpRequest.getServletPath()));
                GZIPHttpServletResponseWrapper gzipResponse = new GZIPHttpServletResponseWrapper(httpResponse);
                chain.doFilter(request, gzipResponse);
                gzipResponse.finish();
                return;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

}

class GZIPHttpServletResponseWrapper extends HttpServletResponseWrapper {

    private ServletResponseGZIPOutputStream gzipStream;
    private ServletOutputStream outputStream;
    private PrintWriter printWriter;

    GZIPHttpServletResponseWrapper(HttpServletResponse response) throws IOException {
        super(response);
        response.addHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
    }

    void finish() throws IOException {
        if (printWriter != null) {
            printWriter.close();
        }
        if (outputStream != null) {
            outputStream.close();
        }
        if (gzipStream != null) {
            gzipStream.close();
        }
    }

    @Override
    public void flushBuffer() throws IOException {
        if (printWriter != null) {
            printWriter.flush();
        }
        if (outputStream != null) {
            outputStream.flush();
        }
        super.flushBuffer();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (printWriter != null) {
            throw new IllegalStateException("printWriter already defined");
        }
        if (outputStream == null) {
            initGzip();
            outputStream = gzipStream;
        }
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (outputStream != null) {
            throw new IllegalStateException("printWriter already defined");
        }
        if (printWriter == null) {
            initGzip();
            printWriter = new PrintWriter(new OutputStreamWriter(gzipStream, getResponse().getCharacterEncoding()));
        }
        return printWriter;
    }

    @Override
    public void setContentLength(int len) {
    }

    private void initGzip() throws IOException {
        gzipStream = new ServletResponseGZIPOutputStream(getResponse().getOutputStream());
    }

}

class ServletResponseGZIPOutputStream extends ServletOutputStream {

    private GZIPOutputStream gzipStream;
    private final AtomicBoolean open = new AtomicBoolean(true);

    ServletResponseGZIPOutputStream(OutputStream output) throws IOException {
        gzipStream = new GZIPOutputStream(output);
    }

    @Override
    public void close() throws IOException {
        if (open.compareAndSet(true, false)) {
            gzipStream.close();
        }
    }

    @Override
    public void flush() throws IOException {
        gzipStream.flush();
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (!open.get()) {
            throw new IOException("Stream closed!");
        }
        gzipStream.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        if (!open.get()) {
            throw new IOException("Stream closed!");
        }
        gzipStream.write(b);
    }

    @Override
    public boolean isReady() {
        return true;

    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
    }
}
