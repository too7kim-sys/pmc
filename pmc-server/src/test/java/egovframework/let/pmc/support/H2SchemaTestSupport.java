package egovframework.let.pmc.support;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 인메모리 H2(PostgreSQL 호환모드) 통합테스트 하니스.
 * <p>
 * 운영은 PostgreSQL 이지만, Docker 없는 환경에서도 DDL/매퍼 정합성을 실제 실행으로 검증하기 위해
 * V1~V10 전체 스키마 + 시드(S2,S1,S3)를 H2 에 적재하고, 운영과 동일한 매퍼 XML 로
 * {@link SqlSessionFactory} 를 구성한다. PG↔H2 DDL 차이는 {@link #toH2(String)} 에서 최소 변환한다.
 * <p>
 * 운영 매퍼/DDL 원본은 일절 수정하지 않는다(테스트 전용 변환).
 */
public final class H2SchemaTestSupport {

    private final DataSource dataSource;
    private final SqlSessionFactory sqlSessionFactory;

    private H2SchemaTestSupport(DataSource ds, SqlSessionFactory sf) {
        this.dataSource = ds;
        this.sqlSessionFactory = sf;
    }

    public DataSource dataSource() { return dataSource; }
    public SqlSessionFactory sqlSessionFactory() { return sqlSessionFactory; }

    /** 전체 스키마+시드를 적재한 H2 하니스 생성. dbName 으로 인메모리 DB 를 격리한다. */
    public static H2SchemaTestSupport create(String dbName) throws Exception {
        String url = "jdbc:h2:mem:" + dbName
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
                + ";NON_KEYWORDS=VALUE,YEAR,MONTH,DAY,HOUR,MINUTE,SECOND";
        DataSource ds = new UnpooledDataSource("org.h2.Driver", url, "sa", "");

        List<File> scripts = new ArrayList<>();
        scripts.addAll(sortedDdl(new File("../db/ddl")));   // V1..V10 (버전 숫자 오름차순)
        scripts.addAll(filterSeed(new File("../db/seed"), "S2"));
        scripts.addAll(filterSeed(new File("../db/seed"), "S1"));
        scripts.addAll(filterSeed(new File("../db/seed"), "S3"));

        try (Connection c = ds.getConnection()) {
            for (File f : scripts) {
                String sql = toH2(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
                try (Statement st = c.createStatement()) {
                    st.execute(sql);
                }
            }
        }
        return new H2SchemaTestSupport(ds, buildSessionFactory(ds));
    }

    /** PostgreSQL DDL/시드를 H2(PostgreSQL 모드) 호환으로 최소 변환(테스트 전용). */
    static String toH2(String sql) {
        // TIMESTAMPTZ 별칭 미지원 → 표준 표기
        sql = sql.replaceAll("(?i)TIMESTAMPTZ", "TIMESTAMP WITH TIME ZONE");
        // H2 는 ON CONFLICT 의 충돌 대상 컬럼 목록을 받지 않음 → 대상 없는 형태로
        sql = sql.replaceAll("(?i)ON\\s+CONFLICT\\s*\\([^)]*\\)\\s+DO\\s+NOTHING", "ON CONFLICT DO NOTHING");
        return sql;
    }

    /** 버전 숫자(V1<V2<…<V10) 오름차순. 문자열 정렬은 V10 을 V1 앞에 두므로 사용 금지. */
    public static List<File> sortedDdl(File dir) {
        File[] arr = dir.listFiles((d, n) -> n.endsWith(".sql"));
        List<File> list = arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr));
        list.sort(Comparator.comparingInt(H2SchemaTestSupport::version));
        return list;
    }

    public static int version(File f) {
        Matcher m = Pattern.compile("V(\\d+)").matcher(f.getName());
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    public static List<File> filterSeed(File dir, String prefix) {
        File[] arr = dir.listFiles((d, n) -> n.startsWith(prefix) && n.endsWith(".sql"));
        List<File> list = arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr));
        list.sort(Comparator.comparing(File::getName));
        return list;
    }

    private static SqlSessionFactory buildSessionFactory(DataSource ds) throws Exception {
        Configuration cfg = new Configuration();
        cfg.setEnvironment(new Environment("h2", new JdbcTransactionFactory(), ds));
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
