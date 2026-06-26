<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">공통코드 관리</h2>

<c:if test="${not empty msg}"><div class="card"><span class="st NORMAL"><c:out value="${msg}"/></span></div></c:if>

<div class="card">
    <h3><c:choose><c:when test="${not empty editCode}">공통코드 수정</c:when><c:otherwise>공통코드 등록</c:otherwise></c:choose></h3>
    <form method="post" action="<c:url value='/cmm/cmmnCode/save.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <input type="hidden" name="isNew" value="${empty editCode ? 'true' : 'false'}"/>
        <c:choose>
            <c:when test="${not empty editCode}">
                <p>그룹코드: <b><c:out value="${editCode.clCode}"/></b>
                   &nbsp; 코드: <b><c:out value="${editCode.code}"/></b>
                   <input type="hidden" name="clCode" value="${editCode.clCode}"/>
                   <input type="hidden" name="code" value="${editCode.code}"/></p>
            </c:when>
            <c:otherwise>
                <p>그룹코드: <input name="clCode" list="grouplist" required placeholder="기존 선택 또는 새 코드"/>
                   <datalist id="grouplist">
                       <c:forEach var="g" items="${groups}"><option value="${g.clCode}"><c:out value="${g.clCodeNm}"/></option></c:forEach>
                   </datalist>
                   새 그룹명: <input name="newGroupNm" placeholder="새 그룹일 때만"/>
                   코드: <input name="code" required/></p>
            </c:otherwise>
        </c:choose>
        <p>코드명: <input name="codeNm" value="<c:out value='${editCode.codeNm}'/>" required/>
           순서: <input type="number" name="sortOrdr" value="${empty editCode ? 0 : editCode.sortOrdr}" style="width:70px"/>
           사용:
           <select name="useYn">
               <option value="Y" ${editCode.useYn=='N' ? '' : 'selected'}>Y</option>
               <option value="N" ${editCode.useYn=='N' ? 'selected' : ''}>N</option>
           </select>
           <button class="btn">저장</button>
           <c:if test="${not empty editCode}"><a class="btn" href="<c:url value='/cmm/cmmnCode/list.do'/>">취소</a></c:if>
        </p>
    </form>
</div>

<div class="card">
    <table class="tbl">
        <tr><th>그룹코드</th><th>그룹명</th><th>코드</th><th>코드명</th><th>순서</th><th>사용</th><th>관리</th></tr>
        <c:forEach var="c" items="${codes}">
            <tr>
                <td><c:out value="${c.clCode}"/></td>
                <td><c:out value="${c.clCodeNm}"/></td>
                <td><code><c:out value="${c.code}"/></code></td>
                <td><c:out value="${c.codeNm}"/></td>
                <td>${c.sortOrdr}</td>
                <td>${c.useYn}</td>
                <td>
                    <a class="btn" href="<c:url value='/cmm/cmmnCode/list.do'/>?clCode=${c.clCode}&code=${c.code}">수정</a>
                    <form class="inline" method="post" action="<c:url value='/cmm/cmmnCode/delete.do'/>" onsubmit="return confirm('삭제하시겠습니까?');">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        <input type="hidden" name="clCode" value="${c.clCode}"/>
                        <input type="hidden" name="code" value="${c.code}"/>
                        <button class="btn">삭제</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty codes}"><tr><td colspan="7" class="muted">등록된 공통코드가 없습니다.</td></tr></c:if>
    </table>
</div>
<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
