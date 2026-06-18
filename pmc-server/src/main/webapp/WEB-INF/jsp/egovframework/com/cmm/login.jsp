<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>PMC 로그인</title>
    <link rel="stylesheet" href="<c:url value='/css/pmc.css'/>">
    <style>
        .login-wrap { max-width: 360px; margin: 80px auto; }
        .login-wrap .card { padding: 28px; }
        .login-wrap h1 { font-size: 20px; color:#1b3a6b; text-align:center; }
        .login-wrap input { width: 100%; margin-bottom: 10px; padding: 10px; }
        .login-wrap .btn { width: 100%; padding: 10px; }
        .err { color:#c0392b; font-size:13px; margin-bottom:10px; }
    </style>
</head>
<body>
<div class="login-wrap">
    <div class="card">
        <h1>🛡 PMC 장애예방점검</h1>
        <p class="muted" style="text-align:center;">정보시스템 장애예방 점검 자동화</p>
        <c:if test="${param.error != null}"><div class="err">아이디 또는 비밀번호가 올바르지 않습니다.</div></c:if>
        <form method="post" action="<c:url value='/login-process'/>">
            <input type="text" name="username" placeholder="아이디" autofocus required>
            <input type="password" name="password" placeholder="비밀번호" required>
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <button type="submit" class="btn">로그인</button>
        </form>
        <p class="muted" style="text-align:center; margin-top:14px;">기본 관리자 : admin / pmc1234!</p>
    </div>
</div>
</body>
</html>
