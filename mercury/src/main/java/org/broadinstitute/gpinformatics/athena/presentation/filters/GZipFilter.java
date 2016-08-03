package org.broadinstitute.gpinformatics.athena.presentation.filters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;

/**
 * This filter is used for compressing streams for web requests.  Typically used for more static content since most of
 * the time spent for page HTML is the dynamic creation of the page -- not downloading the few KB of HTML text in it.
 */
public class GZipFilter implements Filter {
    /**
     * Process filter.
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
            ServletException {
        if (req instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;
            String acceptEncoding = request.getHeader("accept-encoding");

            // Check to see if the browser supports compression before doing it.
            if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
                GZIPResponseWrapper wrappedResponse = new GZIPResponseWrapper(response);
                wrappedResponse.finishResponse();
                chain.doFilter(req, wrappedResponse);
            } else {
                chain.doFilter(req, res);
            }
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // noop
    }

    @Override
    public void destroy() {
        // noop
    }


    /**
     * Class to handle the gzipping of the content as a stream.
     */
    class GZIPResponseStream extends ServletOutputStream {
        protected ByteArrayOutputStream byteArrayOutputStream;

        protected GZIPOutputStream gzipStream;

        protected boolean closed = false;

        protected HttpServletResponse response;

        protected ServletOutputStream output;

        public GZIPResponseStream(HttpServletResponse response) throws IOException {
            super();
            closed = false;
            this.response = response;
            this.output = response.getOutputStream();

            byteArrayOutputStream = new ByteArrayOutputStream();
            gzipStream = new GZIPOutputStream(byteArrayOutputStream);
        }

        @Override
        public void flush() throws IOException {
            if (closed) {
                throw new IOException("Cannot flush a closed output stream");
            }
            gzipStream.flush();
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                throw new IOException("This output stream has already been closed");
            }

            gzipStream.finish();

            byte[] bytes = byteArrayOutputStream.toByteArray();

            response.addHeader("Content-Length", Integer.toString(bytes.length));
            response.addHeader("Content-Encoding", "gzip");
            output.write(bytes);
            output.flush();
            output.close();
            closed = true;
        }

        public boolean closed() {
            return (this.closed);
        }

        public void reset() {
            // noop
        }

        @Override
        public void write(int b) throws IOException {
            if (closed) {
                throw new IOException("Cannot write to a closed output stream");
            }
            gzipStream.write((byte) b);
        }

        @Override
        public void write(byte b[]) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
            if (closed) {
                throw new IOException("Cannot write to a closed output stream");
            }
            gzipStream.write(b, off, len);
        }

        @Override
        public boolean isReady() {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            throw new RuntimeException("Not implemented");
        }
    }

    /**
     * Response wrapper for the {@link HttpServletResponseWrapper}.
     */
    class GZIPResponseWrapper extends HttpServletResponseWrapper {
        private Log log = LogFactory.getLog(GZIPResponseStream.class);

        protected HttpServletResponse origResponse;

        protected ServletOutputStream stream;

        protected PrintWriter writer;

        protected int error;

        public GZIPResponseWrapper(HttpServletResponse response) {
            super(response);
            origResponse = response;
        }

        public ServletOutputStream createOutputStream() throws IOException {
            return (new GZIPResponseStream(origResponse));
        }

        public void finishResponse() {
            try {
                if (writer != null) {
                    writer.close();
                } else {
                    if (stream != null) {
                        stream.close();
                    }
                }
            } catch (IOException e) {
                log.error("Problem finishing response", e);
            }
        }

        @Override
        public void flushBuffer() throws IOException {
            stream.flush();
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (writer != null) {
                throw new IllegalStateException("getWriter() has already been called!");
            }

            if (stream == null) {
                stream = createOutputStream();
            }
            return (stream);
        }

        @Override
        public void sendError(int error, String message) throws IOException {
            super.sendError(error, message);
            this.error = error;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            // If denied access, don't create new stream or write because it causes the web.xml's 403 page to not render.
            if (this.error == HttpServletResponse.SC_FORBIDDEN) {
                return super.getWriter();
            }

            if (writer != null) {
                return (writer);
            }

            if (stream != null) {
                throw new IllegalStateException("getOutputStream() has already been called!");
            }

            stream = createOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(stream, "UTF-8"));
            return (writer);
        }

        /**
         * Content length calculated when we close the stream and set on response, based on actual data, not some other
         * value.
         */
        @Override
        public void setContentLength(int length) {
        }
    }
}