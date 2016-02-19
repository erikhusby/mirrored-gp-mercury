<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%-- This page renders an HTML fragment for a new search term in a user-defined search, it is
 the result of an AJAX call --%>
<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.ConfigurableSearchActionBean"--%>

<c:set var="searchValues" value="${actionBean.searchValueList}" scope="request"/>
<jsp:include page="recurse_search_terms.jsp"/>

