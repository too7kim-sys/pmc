<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">사용자 관리</h2>

<c:if test="${not empty msg}"><div class="card"><span class="st NORMAL"><c:out value="${msg}"/></span></div></c:if>

<div class="card">
    <h3><c:choose><c:when test="${not empty editUser}">사용자 수정</c:when><c:otherwise>사용자 등록</c:otherwise></c:choose></h3>
    <form method="post" action="<c:url value='/pmc/admin/user/save.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <input type="hidden" name="isNew" value="${empty editUser ? 'true' : 'false'}"/>
        <p>ID:
            <c:choose>
                <c:when test="${not empty editUser}">
                    <b><c:out value="${editUser.emplyrId}"/></b><input type="hidden" name="emplyrId" value="${editUser.emplyrId}"/>
                </c:when>
                <c:otherwise><input name="emplyrId" required/></c:otherwise>
            </c:choose>
            &nbsp; 성명: <input name="userNm" value="<c:out value='${editUser.userNm}'/>" required/>
            &nbsp; 비밀번호: <input type="password" name="password" placeholder="${empty editUser ? '필수' : '변경 시에만 입력'}"/>
        </p>
        <p>이메일: <input name="emailAdres" value="<c:out value='${editUser.emailAdres}'/>"/>
           직위: <input name="ofcpsNm" value="<c:out value='${editUser.ofcpsNm}'/>" style="width:90px"/>
           부서: <input name="deptCode" value="<c:out value='${editUser.deptCode}'/>" style="width:90px"/>
        </p>
        <p>상태:
            <select name="emplyrSttus">
                <option value="P" ${editUser.emplyrSttus=='D' ? '' : 'selected'}>정상(P)</option>
                <option value="D" ${editUser.emplyrSttus=='D' ? 'selected' : ''}>정지(D)</option>
            </select>
            잠금:
            <select name="lockAt">
                <option value="N" ${editUser.lockAt=='Y' ? '' : 'selected'}>N</option>
                <option value="Y" ${editUser.lockAt=='Y' ? 'selected' : ''}>Y</option>
            </select>
            권한:
            <c:forEach var="r" items="${roles}">
                <c:set var="chk" value=""/>
                <c:forEach var="ur" items="${editUserRoles}"><c:if test="${ur == r.authorCode}"><c:set var="chk" value="checked"/></c:if></c:forEach>
                <label style="margin-right:8px"><input type="checkbox" name="roles" value="${r.authorCode}" ${chk}/> <c:out value="${r.authorCode}"/></label>
            </c:forEach>
            <button class="btn">저장</button>
            <c:if test="${not empty editUser}"><a class="btn" href="<c:url value='/pmc/admin/user.do'/>">취소</a></c:if>
        </p>
    </form>
    <p class="muted">상태 '정상(P)'만 로그인 가능. 잠금 'Y'면 차단. 비밀번호는 bcrypt 로 저장됩니다.
       사용자정보 연계(LDAP/SSO/조직도)는 com/integration 어댑터로 동기화됩니다(설정 시).</p>
</div>

<div class="card">
    <table class="tbl">
        <tr><th>ID</th><th>성명</th><th>이메일</th><th>직위</th><th>부서</th><th>권한</th><th>상태</th><th>잠금</th><th>최근로그인</th><th>관리</th></tr>
        <c:forEach var="u" items="${users}">
            <tr>
                <td><c:out value="${u.emplyrId}"/></td>
                <td><c:out value="${u.userNm}"/></td>
                <td><c:out value="${u.emailAdres}"/></td>
                <td><c:out value="${u.ofcpsNm}"/></td>
                <td><c:out value="${u.deptCode}"/></td>
                <td><c:out value="${u.roles}"/></td>
                <td>${u.emplyrSttus}</td>
                <td>${u.lockAt}</td>
                <td>${u.lastLoginDt}</td>
                <td>
                    <a class="btn" href="<c:url value='/pmc/admin/user.do'/>?emplyrId=${u.emplyrId}">수정</a>
                    <form class="inline" method="post" action="<c:url value='/pmc/admin/user/delete.do'/>" onsubmit="return confirm('삭제하시겠습니까?');">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        <input type="hidden" name="emplyrId" value="${u.emplyrId}"/>
                        <button class="btn">삭제</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty users}"><tr><td colspan="10" class="muted">등록된 사용자가 없습니다.</td></tr></c:if>
    </table>
</div>
<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
