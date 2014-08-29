<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%-- This page allows the user to select user defined search entity type --%>

<stripes:layout-render name="/layout.jsp" pageTitle="User-Defined Search" sectionTitle="User-Defined Search">

<stripes:layout-component name="extraHead">

</stripes:layout-component>

<stripes:layout-component name="content">

<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.ConfigurableSearchActionBean"--%>
<stripes:form action="/search/ConfigurableSearch.action">
    <h4>Search For Entity Type: </h4>
                <stripes:select name="entityName" id="entityName">
                    <c:forEach items="${actionBean.availableEntityTypes}" var="entry">
                        <option value="${entry.entityName}">${entry.displayName}</option>
                    </c:forEach>
                </stripes:select>
    <stripes:submit name="queryPage" value="Search" class="btn btn-primary"/>
                
</stripes:form>

</stripes:layout-component>
</stripes:layout-render>