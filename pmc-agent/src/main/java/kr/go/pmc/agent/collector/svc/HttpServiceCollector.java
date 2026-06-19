package kr.go.pmc.agent.collector.svc;

import kr.go.pmc.agent.collector.AbstractCollector;
import kr.go.pmc.agent.collector.CollectContext;
import kr.go.pmc.agent.model.Category;
import kr.go.pmc.agent.model.ResultItem;
import kr.go.pmc.agent.model.ResultStatus;
import kr.go.pmc.agent.model.SvcTarget;
import kr.go.pmc.agent.platform.Platform;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SVC 카테고리 수집기. 정책의 svcTargets 각 대상에 대해 순수 Java(HttpURLConnection)로
 * 다음 항목을 점검한다:
 * <ul>
 *   <li>SVC_URL_STATUS — HTTP 응답 코드 / 기대코드 일치 여부</li>
 *   <li>SVC_RESPONSE_TIME — 응답 시간(ms)</li>
 *   <li>SVC_CONTENT_MATCH — 본문에 기대 문자열 포함 여부</li>
 *   <li>SVC_SSL_EXPIRY — 서버 인증서 만료까지 일수(HTTPS, sslCheckYn=Y)</li>
 * </ul>
 * 보안: URL 은 셸을 거치지 않고 HttpURLConnection 에 직접 전달된다.
 */
public class HttpServiceCollector extends AbstractCollector {

    @Override
    public Category category() {
        return Category.SVC;
    }

    @Override
    public boolean supports(Platform p) {
        return true; // 순수 Java — 모든 플랫폼 지원
    }

    @Override
    public List<ResultItem> collect(CollectContext ctx) {
        List<ResultItem> out = new ArrayList<>();
        if (ctx.policy() == null || ctx.policy().getSvcTargets() == null
                || ctx.policy().getSvcTargets().isEmpty()) {
            out.add(na(Category.SVC, "SVC_TARGETS", "서비스 점검 대상",
                    "정책에 svcTargets 가 정의되지 않음"));
            return out;
        }
        for (SvcTarget t : ctx.policy().getSvcTargets()) {
            try {
                out.addAll(probe(t));
            } catch (RuntimeException e) {
                out.add(error(Category.SVC, "SVC_URL_STATUS",
                        "URL 상태(" + safeName(t) + ")", "점검 실패: " + e.getMessage()));
            }
        }
        return out;
    }

    /**
     * 단일 대상에 대한 점검(테스트 가능하도록 public).
     */
    public List<ResultItem> probe(SvcTarget t) {
        List<ResultItem> items = new ArrayList<>();
        String name = safeName(t);
        if (t.getUrl() == null || t.getUrl().trim().isEmpty()) {
            items.add(error(Category.SVC, "SVC_URL_STATUS", "URL 상태(" + name + ")", "URL 미지정"));
            return items;
        }

        int timeout = t.getTimeoutMs() == null ? 5000 : t.getTimeoutMs();
        String method = t.getHttpMethod() == null ? "GET" : t.getHttpMethod().toUpperCase();
        int expectedStatus = t.getExpectedStatus() == null ? 200 : t.getExpectedStatus();

        HttpURLConnection conn = null;
        long start = System.nanoTime();
        Integer respCode = null;
        String body = null;
        String connError = null;
        try {
            URL url = new URL(t.getUrl());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "PMC-Agent/1.0");
            conn.connect();
            respCode = conn.getResponseCode();
            body = readBody(conn);
        } catch (Exception e) {
            connError = e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            if (conn != null) conn.disconnect();
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        // 1) URL 상태
        ResultItem statusItem = item(Category.SVC, "SVC_URL_STATUS", "URL 상태(" + name + ")");
        if (respCode == null) {
            statusItem.setStatus(ResultStatus.CRITICAL);
            statusItem.setValue("DOWN");
            statusItem.setError(connError);
        } else {
            statusItem.setValue(String.valueOf(respCode));
            statusItem.setRaw("expected=" + expectedStatus);
            statusItem.setStatus(respCode == expectedStatus ? ResultStatus.NORMAL : ResultStatus.CRITICAL);
            statusItem.setThresholdCritical("!= " + expectedStatus);
        }
        items.add(statusItem);

        // 2) 응답시간(연결 성공한 경우만 의미있음)
        ResultItem rtItem = value(Category.SVC, "SVC_RESPONSE_TIME",
                "응답시간(" + name + ")", String.valueOf(elapsedMs), "ms");
        if (respCode == null) {
            rtItem.setStatus(ResultStatus.NA);
            rtItem.setError("연결 실패로 응답시간 측정 불가");
            rtItem.setValue(null);
        }
        items.add(rtItem);

        // 3) 본문 매칭(기대 문자열 지정시)
        if (t.getExpectedContent() != null && !t.getExpectedContent().isEmpty()) {
            ResultItem cm = item(Category.SVC, "SVC_CONTENT_MATCH", "본문 일치(" + name + ")");
            cm.setRaw("expect=" + t.getExpectedContent());
            if (body == null) {
                cm.setStatus(ResultStatus.CRITICAL);
                cm.setValue("NO_BODY");
                cm.setError(connError);
            } else {
                boolean matched = body.contains(t.getExpectedContent());
                cm.setValue(matched ? "MATCH" : "MISMATCH");
                cm.setStatus(matched ? ResultStatus.NORMAL : ResultStatus.WARN);
            }
            items.add(cm);
        }

        // 4) SSL 만료(HTTPS + sslCheckYn=Y)
        if (t.isSslCheck()) {
            items.add(checkSsl(t, name, timeout));
        }

        return items;
    }

    private ResultItem checkSsl(SvcTarget t, String name, int timeout) {
        ResultItem ssl = item(Category.SVC, "SVC_SSL_EXPIRY", "SSL 만료(" + name + ")");
        if (t.getUrl() == null || !t.getUrl().toLowerCase().startsWith("https")) {
            ssl.setStatus(ResultStatus.NA);
            ssl.setError("HTTPS 가 아님");
            return ssl;
        }
        HttpsURLConnection conn = null;
        try {
            URL url = new URL(t.getUrl());
            conn = (HttpsURLConnection) url.openConnection();
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setRequestMethod("HEAD");
            conn.connect();
            Certificate[] certs = conn.getServerCertificates();
            Instant earliest = null;
            for (Certificate c : certs) {
                if (c instanceof X509Certificate) {
                    Instant exp = ((X509Certificate) c).getNotAfter().toInstant();
                    if (earliest == null || exp.isBefore(earliest)) {
                        earliest = exp;
                    }
                }
            }
            if (earliest == null) {
                ssl.setStatus(ResultStatus.ERROR);
                ssl.setError("서버 인증서를 읽을 수 없음");
                return ssl;
            }
            long days = ChronoUnit.DAYS.between(Instant.now(), earliest);
            ssl.setValue(String.valueOf(days));
            ssl.setUnit("days");
            ssl.setRaw("notAfter=" + earliest);
            // 기본 임계치: 7일 이내 CRITICAL, 30일 이내 WARN, 만료 CRITICAL
            ssl.setThresholdWarn("<= 30");
            ssl.setThresholdCritical("<= 7");
            if (days <= 7) {
                ssl.setStatus(ResultStatus.CRITICAL);
            } else if (days <= 30) {
                ssl.setStatus(ResultStatus.WARN);
            } else {
                ssl.setStatus(ResultStatus.NORMAL);
            }
        } catch (Exception e) {
            ssl.setStatus(ResultStatus.ERROR);
            ssl.setError("SSL 점검 실패: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return ssl;
    }

    private String readBody(HttpURLConnection conn) {
        InputStream is = null;
        try {
            try {
                is = conn.getInputStream();
            } catch (Exception e) {
                is = conn.getErrorStream();
            }
            if (is == null) return "";
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                char[] buf = new char[4096];
                int n;
                int total = 0;
                // 본문은 최대 1MB 까지만 읽음(매칭 목적)
                while ((n = br.read(buf)) != -1 && total < 1_048_576) {
                    sb.append(buf, 0, n);
                    total += n;
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String safeName(SvcTarget t) {
        if (t.getSvcName() != null && !t.getSvcName().isEmpty()) return t.getSvcName();
        return t.getUrl() == null ? "?" : t.getUrl();
    }
}
