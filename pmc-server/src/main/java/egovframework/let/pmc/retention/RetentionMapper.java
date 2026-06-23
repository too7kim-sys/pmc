package egovframework.let.pmc.retention;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 데이터 보존·정리(retention) 매퍼. 보존기간 경과 데이터 삭제.
 */
@Mapper
public interface RetentionMapper {

    /** 보존기간 경과 run 삭제(계획 연계 run 은 보존). result_item 은 FK CASCADE 로 자동삭제. 삭제 건수 반환. */
    int deleteRunsBefore(@Param("days") int days);

    /** 보존기간 경과 보고서의 파일 경로 목록(파일 삭제용). */
    List<String> selectOldReportPaths(@Param("days") int days);

    /** 보존기간 경과 보고서 행 삭제. 삭제 건수 반환. */
    int deleteReportsBefore(@Param("days") int days);

    /** 보존기간 경과 알림 이력 삭제. 삭제 건수 반환. */
    int deleteAlertsBefore(@Param("days") int days);
}
