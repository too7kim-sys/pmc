package egovframework.let.pmc.common.security;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * UI 로그인 사용자 조회 매퍼 (comtnemplyrinfo / comtnempauthor).
 */
@Mapper
public interface UserMapper {

    Map<String, Object> selectUser(@Param("emplyrId") String emplyrId);

    List<String> selectAuthorities(@Param("emplyrId") String emplyrId);

    void updateLoginSuccess(@Param("emplyrId") String emplyrId);

    /** 로그인 실패 1회 누적, 임계치 도달 시 계정 잠금(lock_at='Y'). */
    void lockIfExceeded(@Param("emplyrId") String emplyrId, @Param("threshold") int threshold);
}
