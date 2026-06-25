package kr.go.pmc.agent.collector.sec;

import kr.go.pmc.agent.collector.AbstractCollector;
import kr.go.pmc.agent.model.Category;
import kr.go.pmc.agent.model.ResultItem;
import kr.go.pmc.agent.model.ResultStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 보안 취약점/구성(config) 진단 수집기 공통 베이스(KISA U-코드 기준).
 * <p>
 * 판정: 양호=NORMAL, 취약(상→CRITICAL / 중·하→WARN), 미해당=NA, 판정불가=ERROR.
 * 등급(상/중/하)은 결과 JSON 스키마를 바꾸지 않기 위해 {@code raw="SEV=상; <증거>"} 로 인코딩한다
 * (서버 ingest 가 raw 의 {@code SEV=} 토큰을 파싱해 finding 등급으로 사용).
 */
public abstract class AbstractSecCollector extends AbstractCollector {

    @Override
    public Category category() {
        return Category.SEC;
    }

    /** 단일 점검 실패가 전체 run 을 깨지 않도록 보호. */
    protected interface Producer {
        ResultItem get() throws Exception;
    }

    protected void addSafe(List<ResultItem> out, String code, Producer p) {
        try {
            ResultItem it = p.get();
            if (it != null) out.add(it);
        } catch (Exception e) {
            out.add(error(Category.SEC, code, code, "진단 오류: " + e.getMessage()));
        }
    }

    /** 취약 항목. 등급 상→CRITICAL, 중·하→WARN. raw 에 SEV 등급+증거 인코딩. */
    protected ResultItem vuln(String code, String name, String severity, String evidence) {
        ResultItem it = item(Category.SEC, code, name);
        it.setValue("취약");
        it.setStatus("상".equals(severity) ? ResultStatus.CRITICAL : ResultStatus.WARN);
        it.setRaw("SEV=" + severity + "; " + (evidence == null ? "" : evidence));
        return it;
    }

    /** 양호 항목. 등급은 참고로 raw 에 남긴다. */
    protected ResultItem good(String code, String name, String severity, String evidence) {
        ResultItem it = item(Category.SEC, code, name);
        it.setValue("양호");
        it.setStatus(ResultStatus.NORMAL);
        it.setRaw("SEV=" + severity + "; " + (evidence == null ? "" : evidence));
        return it;
    }

    /** 미해당(점검 대상 파일/도구 없음). */
    protected ResultItem notApplicable(String code, String name, String reason) {
        return na(Category.SEC, code, name, reason);
    }

    /** 파일 존재 여부. */
    protected boolean exists(String path) {
        return Files.exists(Paths.get(path));
    }

    /** 파일 전체 읽기(없으면 null). 외부 명령 비경유(allowlist 무관). */
    protected List<String> readLines(String path) {
        try {
            Path p = Paths.get(path);
            if (!Files.exists(p)) return null;
            return Files.readAllLines(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    /** 주석(#)·빈 줄을 제외하고 키워드(정규식)에 일치하는 첫 줄 반환(없으면 null). */
    protected String findActiveLine(List<String> lines, String regex) {
        if (lines == null) return null;
        for (String raw : lines) {
            String s = raw.trim();
            if (s.isEmpty() || s.startsWith("#")) continue;
            if (s.matches(regex)) return s;
        }
        return null;
    }

    /** POSIX 8진수 권한(예 "644"). 실패시 null. 외부 명령 비경유. */
    protected String octalPerm(String path) {
        try {
            Path p = Paths.get(path);
            if (!Files.exists(p)) return null;
            java.util.Set<java.nio.file.attribute.PosixFilePermission> perms =
                    Files.getPosixFilePermissions(p);
            int mode = 0;
            for (java.nio.file.attribute.PosixFilePermission perm : perms) {
                switch (perm) {
                    case OWNER_READ:    mode |= 0400; break;
                    case OWNER_WRITE:   mode |= 0200; break;
                    case OWNER_EXECUTE: mode |= 0100; break;
                    case GROUP_READ:    mode |= 0040; break;
                    case GROUP_WRITE:   mode |= 0020; break;
                    case GROUP_EXECUTE: mode |= 0010; break;
                    case OTHERS_READ:   mode |= 0004; break;
                    case OTHERS_WRITE:  mode |= 0002; break;
                    case OTHERS_EXECUTE:mode |= 0001; break;
                    default: break;
                }
            }
            return Integer.toOctalString(mode);
        } catch (UnsupportedOperationException | IOException e) {
            return null;
        }
    }

    protected List<ResultItem> newList() {
        return new ArrayList<>();
    }
}
