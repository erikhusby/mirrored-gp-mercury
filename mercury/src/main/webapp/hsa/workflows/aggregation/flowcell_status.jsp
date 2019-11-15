<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.hsa.AggregationTriageActionBean"--%>
<table class="table simple" id="flowcellStatusTable">
    <c:forEach items="${actionBean.flowcellStatuses}" var="flowcellStatus">
        <tr>
            <td>${flowcellStatus.flowcell}</td>
            <td>${flowcellStatus.pendingLanes}</td>
        </tr>
        <tr>
            <td>
            </td>
            <td>Dashboard</td>
            <td>${flowcellStatus.dashboardLink}</td>
        </tr>
        <c:forEach items="${flowcellStatus.jiraTickets}" var="entry">
            <tr>
                <td>
                </td>
                <td>${entry.key}</td>
                <td>${entry.value}</td>
            </tr>
        </c:forEach>
    </c:forEach>
</table>
