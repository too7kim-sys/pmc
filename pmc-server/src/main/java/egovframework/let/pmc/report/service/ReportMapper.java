package egovframework.let.pmc.report.service;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportMapper {
    void insertReport(ReportVO vo);
    List<ReportVO> selectReportList();
    ReportVO selectReport(@Param("reportId") Long reportId);
}
