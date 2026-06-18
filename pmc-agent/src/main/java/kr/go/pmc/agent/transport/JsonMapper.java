package kr.go.pmc.agent.transport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * 공용 ObjectMapper 제공자. JSR-310(Instant) ISO 직렬화, non-null 포함, 미지정 필드 무시.
 */
public final class JsonMapper {

    private static final ObjectMapper MAPPER = build();

    private JsonMapper() {
    }

    public static ObjectMapper get() {
        return MAPPER;
    }

    private static ObjectMapper build() {
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ISO-8601 문자열
        m.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        m.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return m;
    }
}
