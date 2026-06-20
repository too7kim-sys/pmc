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

    /** 압축 해제 결과 본문 최대 크기(zip-bomb 방지). 초과 시 IOException. */
    private static final long MAX_INFLATED_BYTES = 32L * 1024 * 1024; // 32MB

    private static class GzipRequestWrapper extends HttpServletRequestWrapper {
        private final ServletInputStream stream;

        GzipRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            final GZIPInputStream gz = new GZIPInputStream(request.getInputStream());
            this.stream = new ServletInputStream() {
                private boolean finished = false;
                private long total = 0;

                @Override public int read() throws IOException {
                    int b = gz.read();
                    if (b < 0) {
                        finished = true;          // EOF 도달 → isFinished 정확히 반영
                        gz.close();               // 해제 스트림 닫기(자원 누수 방지)
                        return -1;
                    }
                    if (++total > MAX_INFLATED_BYTES) {
                        gz.close();
                        throw new IOException("압축 해제 본문이 허용 크기를 초과했습니다.");
                    }
                    return b;
                }

                @Override public boolean isFinished() { return finished; }
                @Override public boolean isReady() { return !finished; }
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
