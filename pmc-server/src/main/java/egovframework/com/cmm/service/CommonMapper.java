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
}
