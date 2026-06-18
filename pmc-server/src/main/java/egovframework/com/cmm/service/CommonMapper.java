package egovframework.com.cmm.service;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 공통코드/사용자 조회(관리 화면).
 */
@Mapper
public interface CommonMapper {
    List<Map<String, Object>> selectCommonCodes();
    List<Map<String, Object>> selectUsers();
}
