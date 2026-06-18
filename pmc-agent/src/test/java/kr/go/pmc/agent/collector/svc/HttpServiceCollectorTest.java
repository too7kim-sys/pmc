package kr.go.pmc.agent.collector.svc;

import com.sun.net.httpserver.HttpServer;
import kr.go.pmc.agent.model.ResultItem;
import kr.go.pmc.agent.model.ResultStatus;
import kr.go.pmc.agent.model.SvcTarget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpServiceCollectorTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", ex -> respond(ex, 200, "로그인 페이지입니다"));
        server.createContext("/notfound", ex -> respond(ex, 404, "no"));
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    private void respond(com.sun.net.httpserver.HttpExchange ex, int code, String body) {
        try {
            byte[] b = body.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(code, b.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(b);
            }
        } catch (Exception ignore) {
        }
    }

    private SvcTarget target(String path, int expectedStatus, String expectedContent) {
        SvcTarget t = new SvcTarget();
        t.setSvcName("test");
        t.setUrl("http://127.0.0.1:" + port + path);
        t.setHttpMethod("GET");
        t.setExpectedStatus(expectedStatus);
        t.setExpectedContent(expectedContent);
        t.setTimeoutMs(3000);
        t.setSslCheckYn("N");
        return t;
    }

    private ResultItem find(List<ResultItem> items, String code) {
        for (ResultItem i : items) {
            if (code.equals(i.getItemCode())) return i;
        }
        return null;
    }

    @Test
    void statusNormalWhenExpectedMatches() {
        HttpServiceCollector col = new HttpServiceCollector();
        List<ResultItem> items = col.probe(target("/ok", 200, "로그인"));
        ResultItem status = find(items, "SVC_URL_STATUS");
        assertNotNull(status);
        assertEquals("200", status.getValue());
        assertEquals(ResultStatus.NORMAL, status.getStatus());
    }

    @Test
    void statusCriticalWhenCodeMismatch() {
        HttpServiceCollector col = new HttpServiceCollector();
        // 404 응답인데 200 기대 → CRITICAL
        List<ResultItem> items = col.probe(target("/notfound", 200, null));
        ResultItem status = find(items, "SVC_URL_STATUS");
        assertNotNull(status);
        assertEquals("404", status.getValue());
        assertEquals(ResultStatus.CRITICAL, status.getStatus());
    }

    @Test
    void contentMatchNormalAndWarn() {
        HttpServiceCollector col = new HttpServiceCollector();
        List<ResultItem> ok = col.probe(target("/ok", 200, "로그인"));
        assertEquals(ResultStatus.NORMAL, find(ok, "SVC_CONTENT_MATCH").getStatus());

        List<ResultItem> mismatch = col.probe(target("/ok", 200, "존재하지않는문자열"));
        assertEquals(ResultStatus.WARN, find(mismatch, "SVC_CONTENT_MATCH").getStatus());
    }

    @Test
    void responseTimeMeasured() {
        HttpServiceCollector col = new HttpServiceCollector();
        List<ResultItem> items = col.probe(target("/ok", 200, null));
        ResultItem rt = find(items, "SVC_RESPONSE_TIME");
        assertNotNull(rt);
        assertEquals("ms", rt.getUnit());
        assertNotNull(rt.getValue());
    }

    @Test
    void connectionFailureIsCriticalAndResponseTimeNa() {
        HttpServiceCollector col = new HttpServiceCollector();
        SvcTarget t = new SvcTarget();
        t.setSvcName("down");
        // 사용하지 않는 포트(서버 미기동)
        t.setUrl("http://127.0.0.1:1/down");
        t.setExpectedStatus(200);
        t.setTimeoutMs(1000);
        List<ResultItem> items = col.probe(t);
        ResultItem status = find(items, "SVC_URL_STATUS");
        assertEquals(ResultStatus.CRITICAL, status.getStatus());
        assertEquals("DOWN", status.getValue());

        ResultItem rt = find(items, "SVC_RESPONSE_TIME");
        assertEquals(ResultStatus.NA, rt.getStatus());
        assertNull(rt.getValue());
    }

    @Test
    void sslNaForHttp() {
        HttpServiceCollector col = new HttpServiceCollector();
        SvcTarget t = target("/ok", 200, null);
        t.setSslCheckYn("Y"); // HTTP 인데 SSL 체크 요청 → NA
        List<ResultItem> items = col.probe(t);
        ResultItem ssl = find(items, "SVC_SSL_EXPIRY");
        assertNotNull(ssl);
        assertEquals(ResultStatus.NA, ssl.getStatus());
    }
}
