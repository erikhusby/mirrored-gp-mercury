<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.infrastructure.search.PaginationUtil" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.queueType.textName} Queue"
                       sectionTitle="${actionBean.queueType.textName} Queue" showCreate="false" dataTablesVersion="1.10">
<%--@elvariable id="labSearchResultList" type="org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList.ResultList"--%>
<stripes:layout-component name="extraHead">
</stripes:layout-component>
<stripes:layout-component name="content">
    <h1 style="margin-top: 50px;text-align:center;">Samples listed below were found to not be in the ${actionBean.queueType.textName} Queue</h1>
    <c:choose>
        <c:when test="${not empty actionBean.labSearchResultList.resultRows}">
            <div style="margin-top: 50px;text-align:center;">
                    <stripes:layout-render name="/columns/configurable_list.jsp"
                                           entityName="${actionBean.entityName}"
                                           sessionKey="${actionBean.sessionKey}"
                                           columnSetName="${actionBean.columnSetName}"
                                           downloadColumnSets="${actionBean.downloadColumnSets}"
                                           resultList="${actionBean.labSearchResultList}"
                                           action="${ctxpath}/search/ConfigurableSearch.action"
                                           downloadViewedColumns="True"
                                           isDbSortAllowed="False"
                                           dbSortPath=""
                                           dataTable="true"
                                           loadDatatable="false"
                                           showJumpToEnd="false"
                    />
            </div>
        </c:when>
        <c:otherwise>
            <div style="margin-top: 50px;text-align:center;">
                All samples within the search are currently actively in the ${actionBean.queueType.textName} queue.
            </div>
        </c:otherwise>
    </c:choose>
</stripes:layout-component>
</stripes:layout-render>