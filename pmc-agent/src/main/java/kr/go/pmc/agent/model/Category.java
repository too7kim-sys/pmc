package kr.go.pmc.agent.model;

/**
 * 점검 카테고리. (OS, WEB, WAS, DB, SW, NW, SVC, SEC)
 */
public enum Category {
    OS,   // 운영체제
    WEB,  // 웹서버
    WAS,  // 웹 애플리케이션 서버
    DB,   // 데이터베이스
    SW,   // 소프트웨어/보안
    NW,   // 네트워크
    SVC,  // 서비스(외부 URL 점검)
    SEC   // 보안 취약점/구성(config) 진단(KISA U-코드)
}
