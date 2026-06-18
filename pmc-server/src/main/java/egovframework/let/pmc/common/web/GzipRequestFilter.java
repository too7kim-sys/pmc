package egovframework.let.pmc.common.web;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

/**
 * Content-Encoding: gzip 요청 본문을 해제하는 필터.
 * Agent는 결과/등록 요청 본문을 gzip 압축하여 전송하므로 서버에서 inflate 한다.
 */
public class GzipRequestFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // no-op
    }

    @Override
    public void destroy() {
        // no-op
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest http = (HttpServletRequest) request;
            String enc = http.getHeader("Content-Encoding");
            if (enc != null && enc.toLowerCase().contains("gzip")) {
                chain.doFilter(new GzipRequestWrapper(http), response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static class GzipRequestWrapper extends HttpServletRequestWrapper {
        private final ServletInputStream stream;

        GzipRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            final GZIPInputStream gz = new GZIPInputStream(request.getInputStream());
            this.stream = new ServletInputStream() {
                @Override public int read() throws IOException { return gz.read(); }
                @Override public boolean isFinished() { return false; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener l) { }
            };
        }

        @Override
        public ServletInputStream getInputStream() {
            return stream;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }
}
