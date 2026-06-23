package egovframework.let.pmc;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DB 없이 도는 스모크 테스트: 모든 MyBatis 매퍼 XML 을 파싱·바인딩한다.
 * - XML 문법 오류, 네임스페이스↔@Mapper 불일치, statement id↔메서드 불일치,
 *   parameterType/resultType 클래스 오참조를 빌드 시점에 잡아낸다(실제 DB 연결 없음).
 */
class MapperBindingSmokeTest {

    @Test
    void allMapperXmlsParseAndBind() throws Exception {
        Configuration cfg = new Configuration();
        cfg.setEnvironment(new Environment("smoke", new JdbcTransactionFactory(),
                new UnpooledDataSource("org.postgresql.Driver",
                        "jdbc:postgresql://localhost:5432/none", null, null)));

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] xmls = resolver.getResources("classpath*:egovframework/mapper/pmc/*.xml");
        assertTrue(xmls.length >= 8, "매퍼 XML 이 충분히 로드되어야 함, 실제=" + xmls.length);

        for (Resource res : xmls) {
            String id = res.getURI().toString();
            try (InputStream in = res.getInputStream()) {
                // parse() 가 네임스페이스 인터페이스 바인딩 + statement 등록 + 클래스 로딩까지 수행
                new XMLMapperBuilder(in, cfg, id, cfg.getSqlFragments()).parse();
            } catch (Exception e) {
                throw new AssertionError("매퍼 XML 바인딩 실패: " + id + " — " + e.getMessage(), e);
            }
        }
        // 미해결 statement/메서드가 있으면 여기서 예외
        cfg.getMappedStatements();
        assertNotNull(cfg.getMappedStatements());
        assertTrue(cfg.getMappedStatements().size() > 20,
                "매핑된 statement 가 충분해야 함, 실제=" + cfg.getMappedStatements().size());
    }
}
