package kr.go.pmc.agent.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.GZIPOutputStream;

/**
 * HttpURLConnection 기반 JSON 전송기. gzip 요청 본문, Bearer 인증,
 * connect/read 타임아웃, 지수 백오프 재시도(1,2,4,8s + 지터, 최대 5회)를 제공한다.
 * HTTP 클라이언트 외부 의존성을 추가하지 않는다.
 */
public class HttpTransport {

    private static final Logger log = LoggerFactory.getLogger(HttpTransport.class);
    private static final int MAX_ATTEMPTS = 5;

    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public HttpTransport(int timeoutMs) {
        int t = timeoutMs <= 0 ? 10000 : timeoutMs;
        this.connectTimeoutMs = t;
        this.readTimeoutMs = t;
    }

    /** JSON POST(gzip 본문). 재시도 소진 시 IOException. */
    public String postJson(String url, String jsonBody, String bearer) throws IOException {
        return sendWithRetry("POST", url, jsonBody, bearer);
    }

    /** JSON GET. 재시도 소진 시 IOException. */
    public String getJson(String url, String bearer) throws IOException {
        return sendWithRetry("GET", url, null, bearer);
    }

    private String sendWithRetry(String method, String url, String body, String bearer)
            throws IOException {
        IOException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return sendOnce(method, url, body, bearer);
            } catch (RetriableHttpException | IOException e) {
                last = (e instanceof IOException) ? (IOException) e
                        : new IOException(e.getMessage());
                if (attempt == MAX_ATTEMPTS) break;
                long backoff = (1000L << (attempt - 1)); // 1,2,4,8s
                long jitter = ThreadLocalRandom.current().nextLong(0, 500);
                long sleep = backoff + jitter;
                log.warn("{} {} 실패(시도 {}/{}): {} — {}ms 후 재시도",
                        method, url, attempt, MAX_ATTEMPTS, e.getMessage(), sleep);
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("재시도 중 인터럽트", ie);
                }
            }
        }
        throw new IOException("재시도 소진(" + MAX_ATTEMPTS + "회): " + url, last);
    }

    private String sendOnce(String method, String url, String body, String bearer)
            throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod(method);
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "PMC-Agent/1.0");
            if (bearer != null && !bearer.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + bearer);
            }
            if (body != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Content-Encoding", "gzip");
                byte[] gz = gzip(body.getBytes(StandardCharsets.UTF_8));
                conn.setFixedLengthStreamingMode(gz.length);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(gz);
                }
            }

            int status = conn.getResponseCode();
            String resp = readStream(status >= 400 ? conn.getErrorStream() : conn.getInputStream());

            if (status >= 200 && status < 300) {
                return resp;
            }
            // 5xx / 429 는 재시도 대상, 4xx 는 즉시 실패
            if (status >= 500 || status == 429) {
                throw new RetriableHttpException("HTTP " + status + ": " + resp);
            }
            throw new IOException("HTTP " + status + ": " + resp);
        } finally {
            conn.disconnect();
        }
    }

    private String readStream(InputStream in) throws IOException {
        if (in == null) return "";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    private byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(bos)) {
            gz.write(data);
        }
        return bos.toByteArray();
    }

    /** 5xx/429 등 재시도 가능한 HTTP 오류. */
    private static final class RetriableHttpException extends RuntimeException {
        RetriableHttpException(String msg) {
            super(msg);
        }
    }
}
