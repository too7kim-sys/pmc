package egovframework.com.cmm.service;

import java.util.List;
import java.util.Map;

/**
 * 공통코드/사용자 관리(등록·수정·삭제). 다중 테이블 변경은 트랜잭션으로 묶는다.
 */
public interface CommonAdminService {

    /** 공통코드 상세 저장. newGroupNm 이 있으면 그룹(없을 때) 먼저 생성. isNew=등록/수정 구분. */
    void saveCode(String clCode, String newGroupNm, String code, String codeNm,
                  Integer sortOrdr, String useYn, boolean isNew);

    void deleteCode(String clCode, String code);

    /**
     * 사용자 저장(등록/수정). rawPassword 가 있으면 bcrypt 해시 후 반영(수정 시 비우면 유지),
     * roles 로 권한 매핑 동기화.
     */
    void saveUser(Map<String, Object> user, String rawPassword, List<String> roles, boolean isNew);

    /** 사용자 삭제(권한 매핑 → 사용자). */
    void deleteUser(String emplyrId);
}
