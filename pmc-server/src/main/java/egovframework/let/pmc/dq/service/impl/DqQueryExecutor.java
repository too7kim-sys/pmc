package egovframework.let.pmc.dq.service.impl;

import egovframework.let.pmc.dq.service.DqMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 데이터품질 룰의 위반 건수 SELECT 를 <b>읽기전용 트랜잭션</b>에서 실행한다.
 * txManager 의 enforceReadOnly=true 와 결합되어 'SET TRANSACTION READ ONLY' 가 적용되므로,
 * 룰 SQL 이 DML/DDL 을 시도해도 DB 가 차단한다(문자열 검사 {@code isSafeSelect} 는 보강 수단).
 * 별도 빈으로 분리해 결과 insert(쓰기) 와 트랜잭션 경계를 분리한다.
 */
@Component
public class DqQueryExecutor {

    private final DqMapper dqMapper;

    @Autowired
    public DqQueryExecutor(DqMapper dqMapper) {
        this.dqMapper = dqMapper;
    }

    @Transactional(readOnly = true)
    public long countViolations(String sql) {
        Long v = dqMapper.executeCount(sql);
        return v == null ? 0L : v;
    }
}
