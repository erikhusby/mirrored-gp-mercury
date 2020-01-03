<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.hsa.AggregationTriageActionBean"/>

<table class="table borderless flowcellStatusTable" id="flowcellStatusTable-${actionBean.dto.pdoSample}">
    <c:if test="${not empty actionBean.dto.missingFlowcellStatuses}">
        <tr>
            <td><strong>Pending Lanes</strong></td>
        </tr>
        <c:forEach items="${actionBean.dto.missingFlowcellStatuses}" var="flowcellStatus">
            <tr>
                <td colspan="2" class="text-right">${flowcellStatus.flowcell}</td>
                <td>${flowcellStatus.pendingLanes}</td>
            </tr>
            <tr>
                <td colspan="2"></td>
                <td>Dashboard</td>
                <td><a href="${flowcellStatus.dashboardLink}">${flowcellStatus.dashboardLink}</a></td>
            </tr>
            <c:forEach items="${flowcellStatus.jiraTickets}" var="entry">
                <tr>
                    <td colspan="2"></td>
                    <td>${entry.key}</td>
                    <td><a href="${entry.value}">${entry.value}</a></td>
                </tr>
            </c:forEach>
        </c:forEach>
    </c:if>
    <c:if test="${not empty actionBean.dto.completedFlowcellStatuses}">
        <tr>
            <td><strong>Aggregated Lanes</strong></td>
        </tr>
        <c:forEach items="${actionBean.dto.completedFlowcellStatuses}" var="flowcellStatus">
            <tr>
                <td colspan="2" class="text-right">${flowcellStatus.flowcell}</td>
                <td>${flowcellStatus.pendingLanes}</td>
            </tr>
            <tr>
                <td colspan="2"></td>
                <td>Dashboard</td>
                <td><a href="${flowcellStatus.dashboardLink}">${flowcellStatus.dashboardLink}</a></td>
            </tr>
            <c:forEach items="${flowcellStatus.jiraTickets}" var="entry">
                <tr>
                    <td colspan="2"></td>
                    <td>${entry.key}</td>
                    <td><a href="${entry.value}">${entry.value}</a></td>
                </tr>
            </c:forEach>
        </c:forEach>
    </c:if>
</table>