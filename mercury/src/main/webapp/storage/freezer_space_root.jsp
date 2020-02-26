<%@ page contentType="text/html;charset=UTF-8"%><%@ include file="/resources/layout/taglibs.jsp" %><%--
***** This is the starting point of recursive JSP snippets that drill down into storage tiers **** --%>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.storage.StorageAllocationActionBean"/>
<c:set var="nodeList" scope="request" value="${actionBean.freezerChildMap}"/>
<jsp:include page="freezer_space_nodes.jsp"/>