package egovframework.let.pmc.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * REST 직렬화용 공통 ObjectMapper.
 * - java.time 지원(jsr310), ISO 문자열, null 미포함, 미지 필드 무시.
 */
public final class JsonConfig {

    private static final ObjectMapper MAPPER = build();

    private JsonConfig() {
    }

    public static ObjectMapper objectMapper() {
        return MAPPER;
    }

    private static ObjectMapper build() {
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        m.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        m.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return m;
    }
}
