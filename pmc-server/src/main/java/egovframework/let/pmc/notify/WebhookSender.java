package egovframework.let.pmc.notify;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Slack/Teams 호환 Incoming Webhook 전송기({"text": ...} JSON POST).
 * 외부 의존성 없이 HttpURLConnection 사용. 전송 성공 여부 반환.
 */
@Component
public class WebhookSender {

    private static final Logger log = LoggerFactory.getLogger(WebhookSender.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public boolean send(String webhookUrl, String text) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            return false;
        }
        HttpURLConnection conn = null;
        try {
            byte[] body = MAPPER.writeValueAsBytes(Collections.singletonMap("text", text));
            conn = (HttpURLConnection) new URL(webhookUrl.trim()).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(body.length);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }
            int code = conn.getResponseCode();
            boolean ok = code >= 200 && code < 300;
            if (!ok) {
                log.warn("Webhook 전송 비정상 응답: HTTP {}", code);
            }
            return ok;
        } catch (Exception e) {
            log.warn("Webhook 전송 실패: {}", e.getMessage());
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
