<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko"><head><meta charset="UTF-8"><title>페이지 없음</title>
<link rel="stylesheet" href="<c:url value='/css/pmc.css'/>"></head>
<body><main style="padding:40px">
    <h2 class="page">404 · 페이지를 찾을 수 없습니다</h2>
    <div class="card">
        <p>요청하신 페이지가 존재하지 않거나 이동되었습니다.</p>
        <p><a class="btn" href="<c:url value='/pmc/dashboard.do'/>">대시보드</a></p>
    </div>
</main></body></html>
