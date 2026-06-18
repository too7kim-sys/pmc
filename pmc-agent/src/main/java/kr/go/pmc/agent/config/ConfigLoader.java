package kr.go.pmc.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML 설정 파일 + 환경변수 오버라이드로 {@link AgentConfig} 를 로딩/저장한다.
 * <p>환경변수: PMC_SERVER_URL, PMC_API_KEY, PMC_AGENT_ID</p>
 */
public final class ConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);

    private ConfigLoader() {
    }

    /**
     * 지정 경로의 YAML 을 로딩한다. 파일이 없으면 기본값 + 환경변수만 적용.
     */
    @SuppressWarnings("unchecked")
    public static AgentConfig load(String path) {
        AgentConfig cfg = new AgentConfig();
        Map<String, Object> map = null;

        if (path != null) {
            Path p = Paths.get(path);
            if (Files.isReadable(p)) {
                try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                    map = new Yaml().load(r);
                } catch (IOException | RuntimeException e) {
                    log.warn("설정 파일 로딩 실패({}). 기본값 사용: {}", path, e.getMessage());
                }
            } else {
                // classpath fallback (예: 패키징된 agent.yml)
                try (InputStream in = ConfigLoader.class.getClassLoader()
                        .getResourceAsStream("agent.yml")) {
                    if (in != null) {
                        map = new Yaml().load(in);
                        log.info("설정 파일 경로가 없어 classpath:agent.yml 사용");
                    } else {
                        log.info("설정 파일 없음({}). 기본값 + 환경변수 사용", path);
                    }
                } catch (IOException | RuntimeException e) {
                    log.warn("classpath 설정 로딩 실패: {}", e.getMessage());
                }
            }
        }

        if (map != null) {
            applyMap(cfg, map);
        }
        applyEnvOverrides(cfg);
        return cfg;
    }

    @SuppressWarnings("unchecked")
    private static void applyMap(AgentConfig cfg, Map<String, Object> m) {
        if (m.containsKey("serverUrl")) cfg.setServerUrl(str(m.get("serverUrl")));
        if (m.containsKey("agentId")) cfg.setAgentId(str(m.get("agentId")));
        if (m.containsKey("apiKey")) cfg.setApiKey(str(m.get("apiKey")));
        if (m.containsKey("enrollToken")) cfg.setEnrollToken(str(m.get("enrollToken")));
        if (m.containsKey("hostname")) cfg.setHostname(str(m.get("hostname")));
        if (m.containsKey("ipAddr")) cfg.setIpAddr(str(m.get("ipAddr")));
        if (m.containsKey("osType")) cfg.setOsType(str(m.get("osType")));
        if (m.containsKey("scheduleCron")) cfg.setScheduleCron(str(m.get("scheduleCron")));
        if (m.containsKey("spoolDir")) cfg.setSpoolDir(str(m.get("spoolDir")));
        if (m.containsKey("logLevel")) cfg.setLogLevel(str(m.get("logLevel")));
        if (m.containsKey("intervalSeconds")) cfg.setIntervalSeconds(intVal(m.get("intervalSeconds"), cfg.getIntervalSeconds()));
        if (m.containsKey("heartbeatSeconds")) cfg.setHeartbeatSeconds(intVal(m.get("heartbeatSeconds"), cfg.getHeartbeatSeconds()));
        if (m.containsKey("timeoutMs")) cfg.setTimeoutMs(intVal(m.get("timeoutMs"), cfg.getTimeoutMs()));
        Object cats = m.get("categories");
        if (cats instanceof List) {
            List<String> list = new ArrayList<>();
            for (Object o : (List<Object>) cats) {
                if (o != null) list.add(o.toString());
            }
            cfg.setCategories(list);
        }
    }

    private static void applyEnvOverrides(AgentConfig cfg) {
        String url = System.getenv("PMC_SERVER_URL");
        String key = System.getenv("PMC_API_KEY");
        String id = System.getenv("PMC_AGENT_ID");
        if (url != null && !url.isEmpty()) cfg.setServerUrl(url);
        if (key != null && !key.isEmpty()) cfg.setApiKey(key);
        if (id != null && !id.isEmpty()) cfg.setAgentId(id);
    }

    /**
     * 등록/스케줄 변경 등 런타임 갱신값을 설정 파일에 다시 저장한다.
     */
    public static synchronized void save(String path, AgentConfig cfg) {
        if (path == null) {
            log.warn("설정 저장 경로 없음 - 저장 건너뜀");
            return;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("serverUrl", cfg.getServerUrl());
        if (cfg.getAgentId() != null) m.put("agentId", cfg.getAgentId());
        if (cfg.getApiKey() != null) m.put("apiKey", cfg.getApiKey());
        if (cfg.getEnrollToken() != null) m.put("enrollToken", cfg.getEnrollToken());
        if (cfg.getHostname() != null) m.put("hostname", cfg.getHostname());
        if (cfg.getIpAddr() != null) m.put("ipAddr", cfg.getIpAddr());
        if (cfg.getOsType() != null) m.put("osType", cfg.getOsType());
        m.put("categories", cfg.getCategories());
        if (cfg.getScheduleCron() != null) m.put("scheduleCron", cfg.getScheduleCron());
        m.put("intervalSeconds", cfg.getIntervalSeconds());
        m.put("heartbeatSeconds", cfg.getHeartbeatSeconds());
        m.put("timeoutMs", cfg.getTimeoutMs());
        m.put("spoolDir", cfg.getSpoolDir());
        m.put("logLevel", cfg.getLogLevel());

        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setPrettyFlow(true);
        Yaml yaml = new Yaml(opts);

        try {
            Path p = Paths.get(path);
            if (p.getParent() != null) {
                Files.createDirectories(p.getParent());
            }
            try (Writer w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
                yaml.dump(m, w);
            }
            log.info("설정 저장 완료: {}", path);
        } catch (IOException e) {
            log.warn("설정 저장 실패({}): {}", path, e.getMessage());
        }
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static int intVal(Object o, int def) {
        if (o == null) return def;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
