package egovframework.let.pmc;

import egovframework.let.pmc.dashboard.service.DashboardMapper;
import egovframework.let.pmc.ingest.service.InspectionMapper;
import egovframework.let.pmc.monitoring.service.MonitoringMapper;
import egovframework.let.pmc.notify.service.AlertChannelMapper;
import egovframework.let.pmc.notify.service.AlertMapper;
import egovframework.let.pmc.risk.service.RiskMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 실 PostgreSQL 통합 스모크 테스트(Testcontainers).
 * DDL(V1~V9)+시드 적용 후 PG 전용 문법(DISTINCT ON·make_interval·regexp_replace·ON CONFLICT·
 * ::date·CAST uuid 등)을 쓰는 대표 매퍼 쿼리를 빈/시드 테이블에서 실행해 문법 정합성을 검증한다.
 * <b>Docker 미가용 환경에서는 전체 skip</b>(빌드 영향 없음). Docker 가용 CI 에서만 실제 실행.
 */
class PgSchemaIntegrationTest {

    private static PostgreSQLContainer<?> pg;
    private static SqlSessionFactory sqlSessionFactory;

    @BeforeAll
    static void setUp() throws Exception {
        assumeTrue(isDockerAvailable(), "Docker 미가용 — PG 통합테스트 skip");

        pg = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("pmc").withUsername("pmc").withPassword("pmc");
        pg.start();

        // DDL → 시드 순서대로 적용(README 순서: V*, S2*, S1*)
        List<File> scripts = new ArrayList<>();
        scripts.addAll(sortedSql(new File("../db/ddl")));
        scripts.addAll(filterSql(new File("../db/seed"), "S2"));
        scripts.addAll(filterSql(new File("../db/seed"), "S1"));
        try (Connection c = java.sql.DriverManager.getConnection(
                pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())) {
            for (File f : scripts) {
                String sql = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                try (Statement st = c.createStatement()) {
                    st.execute(sql); // PG JDBC: 세미콜론 구분 다중 구문 일괄 실행
                }
            }
        }
        sqlSessionFactory = buildSessionFactory(new UnpooledDataSource(
                "org.postgresql.Driver", pg.getJdbcUrl(), pg.getUsername(), pg.getPassword()));
    }

    @AfterAll
    static void tearDown() {
        if (pg != null) pg.stop();
    }

    @Test
    void dashboardQueriesRun() {
        try (SqlSession s = sqlSessionFactory.openSession()) {
            DashboardMapper m = s.getMapper(DashboardMapper.class);
            assertNotNull(m.selectCounters());
            m.selectStatusByCategory();
            m.selectRiskTopServers();
            m.selectRecentRuns();
            m.selectSvcAvailability();
            assertNotNull(m.selectPlanCompliance());
            assertNotNull(m.selectDqSummary());
            m.selectDailyTrend();
        }
    }

    @Test
    void monitoringAndRiskQueriesRun() {
        try (SqlSession s = sqlSessionFactory.openSession()) {
            MonitoringMapper mm = s.getMapper(MonitoringMapper.class);
            mm.selectSvcLatest(7);
            mm.selectSvcFailCount(7);
            mm.selectSvcUrlMap();
            mm.selectDailySummary(7);
            mm.selectRunsByDate("2026-01-01");
            mm.selectRealtimeServers();
            assertNotNull(mm.selectCountersToday());
            mm.selectCapacityServers(7);
            mm.selectCapacitySeries(1L, 7);

            s.getMapper(RiskMapper.class).selectServerRiskFacts(7);
        }
    }

    @Test
    void inspectionAndAlertQueriesRun() {
        try (SqlSession s = sqlSessionFactory.openSession()) {
            InspectionMapper im = s.getMapper(InspectionMapper.class);
            im.selectRunList(null);
            im.selectStatusSummary();

            AlertMapper am = s.getMapper(AlertMapper.class);
            am.selectStaleHeartbeats(24);
            am.selectAlertLog(10);

            s.getMapper(AlertChannelMapper.class).selectEnabledChannels();
        }
    }

    // ---- helpers ----

    private static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    private static List<File> sortedSql(File dir) {
        File[] arr = dir.listFiles((d, n) -> n.endsWith(".sql"));
        List<File> list = arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr));
        list.sort((a, b) -> a.getName().compareTo(b.getName()));
        return list;
    }

    private static List<File> filterSql(File dir, String prefix) {
        File[] arr = dir.listFiles((d, n) -> n.startsWith(prefix) && n.endsWith(".sql"));
        List<File> list = arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr));
        list.sort((a, b) -> a.getName().compareTo(b.getName()));
        return list;
    }

    private static SqlSessionFactory buildSessionFactory(DataSource ds) throws Exception {
        Configuration cfg = new Configuration();
        cfg.setEnvironment(new Environment("it", new JdbcTransactionFactory(), ds));
        cfg.setMapUnderscoreToCamelCase(true);
        cfg.setCallSettersOnNulls(true);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (Resource res : resolver.getResources("classpath*:egovframework/mapper/pmc/*.xml")) {
            try (InputStream in = res.getInputStream()) {
                new XMLMapperBuilder(in, cfg, res.getURI().toString(), cfg.getSqlFragments()).parse();
            }
        }
        return new SqlSessionFactoryBuilder().build(cfg);
    }
}
