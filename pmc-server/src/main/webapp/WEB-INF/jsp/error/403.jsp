<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko"><head><meta charset="UTF-8"><title>접근 거부</title>
<link rel="stylesheet" href="<c:url value='/css/pmc.css'/>"></head>
<body><main style="padding:40px">
    <h2 class="page">403 · 접근 권한이 없습니다</h2>
    <div class="card">
        <p>요청하신 페이지에 접근할 권한이 없습니다. 필요한 권한은 관리자에게 문의하세요.</p>
        <p><a class="btn" href="<c:url value='/pmc/dashboard.do'/>">대시보드</a>
           <a class="btn" href="<c:url value='/login.do'/>">로그인</a></p>
    </div>
</main></body></html>
