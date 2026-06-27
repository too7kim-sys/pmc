package egovframework.com.cmm.service;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 관리자 작업/로그인 감사 로그(pmc_audit_log).
 */
@Mapper
public interface AuditMapper {
    int insertAudit(Map<String, Object> p);
    List<Map<String, Object>> selectAuditLog(@Param("limit") int limit);
}
