<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%-- This page allows the user to select user defined search entity type --%>
<stripes:layout-render name="/layout.jsp" pageTitle="User-Defined Search" sectionTitle="User-Defined Search">
<stripes:layout-component name="extraHead">
</stripes:layout-component>
<stripes:layout-component name="content">

<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.ConfigurableSearchActionBean"--%>
<section>
<stripes:form action="/search/ConfigurableSearch.action">
    <h4>Choose Row Primary Entity:&nbsp;&nbsp;
                <stripes:select name="entityName" id="entityName">
                    <c:forEach items="${actionBean.availableEntityTypes}" var="entry">
                        <option value="${entry.entityName}">${entry.displayName}</option>
                    </c:forEach>
                </stripes:select>&nbsp;&nbsp;
    <stripes:submit name="queryPage" value="Next >>" class="btn btn-primary"/></h4>
    <hr />

    <h4>Or Select A Saved Search:</h4>

    <c:if test="${empty actionBean.allSearchInstances}">
        <h5>&nbsp;&nbsp;(None Available)</h5>
    </c:if>

    <%-- Structure of ConfigurableSearchActionBean.allSearchInstances:
      -- [LabVessel, LabEvent, ...]
      --                '-> [Global Type, User Type]
      --                                      '-> [Saved Search Names...]
    --%>
    <c:forEach items="${actionBean.allSearchInstances.entrySet()}" var="instanceEntrySet">
    <div class="form-horizontal span5" style="padding-bottom: 20px">
        <fieldset>
            <legend><h4>${instanceEntrySet.key.displayName}</h4></legend>
        <c:forEach items="${instanceEntrySet.value.entrySet()}" var="scopeEntrySet">
            <h5 style="margin: 0 0 0 10px">${scopeEntrySet.key.preferenceTypeName}</h5>
            <div style="margin: 0 0 0 20px">
            <c:if test="${empty scopeEntrySet.value}">
                (None Available)
            </c:if>
            <c:forEach items="${scopeEntrySet.value}" var="searchName">
                <stripes:link event="fetchSearch"
                              beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.ConfigurableSearchActionBean"
                              style="font-size: 120%">
                    <stripes:param name="entityName" value="${instanceEntrySet.key.entityName}"/>
                    <stripes:param name="selectedSearchName" value="${scopeEntrySet.key.preferenceScope}|${scopeEntrySet.key}|${searchName}"/>
                    ${searchName}
                </stripes:link><br />
            </c:forEach>
            </div>
        </c:forEach>
        </fieldset>
        </div>
    </c:forEach>

</stripes:form>
</section>
</stripes:layout-component>
</stripes:layout-render>