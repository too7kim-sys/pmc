package egovframework.let.pmc.report.generator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 보고서 생성기에 전달하는 표준화된 점검표 데이터(생성기 비종속).
 */
public class ReportData {

    private String title;
    private final Map<String, String> header = new LinkedHashMap<>();
    private final List<Section> sections = new ArrayList<>();

    public ReportData(String title) {
        this.title = title;
    }

    public void addHeader(String k, String v) {
        header.put(k, v == null ? "" : v);
    }

    public Section addSection(String name, String... columns) {
        Section s = new Section(name, columns);
        sections.add(s);
        return s;
    }

    public String getTitle() { return title; }
    public Map<String, String> getHeader() { return header; }
    public List<Section> getSections() { return sections; }

    /** 한 분류(또는 표) 단위 */
    public static class Section {
        private final String name;
        private final String[] columns;
        private final List<String[]> rows = new ArrayList<>();

        Section(String name, String[] columns) {
            this.name = name;
            this.columns = columns;
        }

        public void addRow(String... cells) {
            rows.add(cells);
        }

        public String getName() { return name; }
        public String[] getColumns() { return columns; }
        public List<String[]> getRows() { return rows; }
    }
}
