<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.infrastructure.search.PaginationUtil" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean"/>
<%--@elvariable id="labSearchResultList" type="org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList.ResultList"--%>
    <div class="form-inline">
        <c:if test="${fn:length(actionBean.searchValuesNotFound) > 0}">
            <h3 style="margin-top: 50px;text-align:center;">${fn:length(actionBean.searchValuesNotFound)} searched values listed below were not found in Mercury at all.</h3>
            <table id="notFound" class="table simple dataTable">
                <thead>
                    <tr><th>Search Value Not Found</th></tr>
                </thead>
                <tbody>
                    <c:forEach items="${actionBean.searchValuesNotFound}" var="notFound" varStatus="status">
                        <tr class="${status.index%2==0 ? "even" : "odd"}"><td>${notFound}</td></tr>
                    </c:forEach>
                </tbody>
            </table>
            <script type="text/javascript">

                $j('#notFound').dataTable({
                    "oTableTools": ttExportDefines,
                });
            </script>
        </c:if>

        <c:choose>
            <c:when test="${not empty actionBean.labSearchResultList.resultRows}">
                <h3 style="margin-top: 50px;text-align:center;">${fn:length(actionBean.labSearchResultList.resultRows)} samples listed below were found to not be in the ${actionBean.queueType.textName} Queue</h3>

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
                <c:if test="${empty actionBean.searchValuesNotFound}">
                    <div style="margin-top: 50px;text-align:center;">
                        <h3 style="margin-top: 50px;text-align:center;">All samples within the search are currently active in the ${actionBean.queueType.textName} queue.</h3>
                    </div>
                </c:if>
            </c:otherwise>
        </c:choose>
    </div>