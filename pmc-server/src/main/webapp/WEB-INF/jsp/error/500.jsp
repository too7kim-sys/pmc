<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko"><head><meta charset="UTF-8"><title>오류</title>
<link rel="stylesheet" href="<c:url value='/css/pmc.css'/>"></head>
<body><main style="padding:40px">
    <h2 class="page">처리 중 오류가 발생했습니다</h2>
    <div class="card">
        <p>일시적인 오류가 발생했습니다. 잠시 후 다시 시도하시고, 계속되면 관리자에게 문의하세요.</p>
        <p><a class="btn" href="<c:url value='/pmc/dashboard.do'/>">대시보드</a></p>
    </div>
</main></body></html>
