package egovframework.com.cmm.service;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 공통코드/사용자 관리(조회 + 등록/수정/삭제).
 */
@Mapper
public interface CommonMapper {

    // ===== 공통코드 =====
    List<Map<String, Object>> selectCommonCodes();
    List<Map<String, Object>> selectCodeGroups();
    /** 특정 그룹의 사용중 코드 목록(드롭다운용). */
    List<Map<String, Object>> selectCodesByGroup(@Param("clCode") String clCode);
    Map<String, Object> selectCode(@Param("clCode") String clCode, @Param("code") String code);
    int countGroup(@Param("clCode") String clCode);
    int insertGroup(@Param("clCode") String clCode, @Param("clCodeNm") String clCodeNm);
    int insertCode(Map<String, Object> p);
    int updateCode(Map<String, Object> p);
    int deleteCode(@Param("clCode") String clCode, @Param("code") String code);

    // ===== 사용자 =====
    List<Map<String, Object>> selectUsers();
    Map<String, Object> selectUserOne(@Param("emplyrId") String emplyrId);
    int countUser(@Param("emplyrId") String emplyrId);
    int insertUser(Map<String, Object> p);
    int updateUser(Map<String, Object> p);
    int updateUserPassword(@Param("emplyrId") String emplyrId, @Param("password") String password);
    int deleteUser(@Param("emplyrId") String emplyrId);

    // ===== 권한(롤) =====
    List<Map<String, Object>> selectRoles();
    List<String> selectUserRoles(@Param("emplyrId") String emplyrId);
    int deleteUserRoles(@Param("emplyrId") String emplyrId);
    int insertUserRole(@Param("emplyrId") String emplyrId, @Param("authorCode") String authorCode);

    // ===== 권한관리(롤 CRUD) =====
    Map<String, Object> selectAuthorityOne(@Param("authorCode") String authorCode);
    int countAuthority(@Param("authorCode") String authorCode);
    int insertAuthority(Map<String, Object> p);
    int updateAuthority(Map<String, Object> p);
    int deleteAuthority(@Param("authorCode") String authorCode);
    int countUsersOfAuthority(@Param("authorCode") String authorCode);

    // ===== 메뉴관리 =====
    List<Map<String, Object>> selectMenus();
    Map<String, Object> selectMenuOne(@Param("menuNo") Long menuNo);
    int countMenu(@Param("menuNo") Long menuNo);
    int insertMenu(Map<String, Object> p);
    int updateMenu(Map<String, Object> p);
    int deleteMenu(@Param("menuNo") Long menuNo);
    int countChildMenu(@Param("menuNo") Long menuNo);

    // ===== 권한-메뉴 매핑 =====
    List<Long> selectAuthorMenus(@Param("authorCode") String authorCode);
    int deleteAuthorMenus(@Param("authorCode") String authorCode);
    int deleteAuthorMenuByMenu(@Param("menuNo") Long menuNo);
    int insertAuthorMenu(@Param("authorCode") String authorCode, @Param("menuNo") Long menuNo);

    // ===== GNB(데이터 구동형 네비게이션) =====
    /** 주어진 롤들이 접근 가능한, URL 이 있는 메뉴를 정렬 순으로(중복 제거). */
    List<Map<String, Object>> selectNavMenus(@Param("roles") List<String> roles);
}
