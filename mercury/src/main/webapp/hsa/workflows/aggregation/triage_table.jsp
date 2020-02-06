<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.hsa.AggregationTriageActionBean" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%--@elvariable id="tableId" type="java.lang.String--%>
<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.hsa.AggregationTriageActionBean"--%>
<%--@elvariable id="inSpecTable" type="java.lang.Boolean"--%>
<%--@elvariable id="dtoList" type="java.util.List<org.broadinstitute.gpinformatics.mercury.presentation.hsa.AggregationTriageActionBean.TriageDto>"--%>
<%--@elvariable id="dtoName" type="java.lang.String"--%>
<table id="${tableId}" class="table simple">
    <thead>
    <tr>
        <th width="30px">
            <input type="checkbox" class="${tableId}-checkAll" title="Check All"/>
            <span id="count" class="${tableId}-checkedCount"></span>
        </th>
        <th></th>
        <th>PDO Sample</th>
        <th>Sample Vessel</th>
        <th>PDO</th>
        <th>Cov >= 95%@20X</th>
        <th>% Contamination</th>
        <th>Aligned Q20 Bases</th>
        <th>Sex@Birth</th>
<%--        <th>Aggregated/Pending Lanes</th>--%>
<%--        <th>Aggregated/Pending Designations</th>--%>
        <th>Sex Concordant</th>
        <th>LOD</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${dtoList}" var="dto" varStatus="status">
        <tr>
            <td>
                <stripes:checkbox name="selectedSamples" class="${tableId}-checkbox"
                                  value="${dto.pdoSample}"/>
            </td>
            <td></td>
            <td>
                    ${dto.pdoSample}
                    <stripes:hidden name="${dtoName}[${status.index}].pdoSample" value="${dto.pdoSample}"/>
            </td>
            <td>
                    ${dto.sampleVessel}
                    <stripes:hidden name="${dtoName}[${status.index}].sampleVessel" value="${dto.sampleVessel}"/>
            </td>
            <td>
                    <stripes:hidden name="${dtoName}[${status.index}].library" value="${dto.library}"/>
            </td>
            <td>
                    ${dto.pdo}
                    <stripes:hidden name="${dtoName}[${status.index}].pdo" value="${dto.pdo}"/>
            </td>
            <td>
                    ${dto.coverage20x}
                    <stripes:hidden name="${dtoName}[${status.index}].coverage20x" value="${dto.coverage20x}"/>
            </td>
            <td>
                    ${dto.contaminination}
                    <stripes:hidden name="${dtoName}[${status.index}].contaminination" value="${dto.contaminination}"/>
            </td>
            <td>
                    ${dto.alignedQ20Bases}
                    <stripes:hidden name="${dtoName}[${status.index}].alignedQ20Bases" value="${dto.alignedQ20Bases}"/>
            </td>
            <td>
                    ${dto.gender}
                    <stripes:hidden name="${dtoName}[${status.index}].gender" value="${dto.gender}"/>
            </td>
            <td>
                ${dto.genderConcordance}
                    <stripes:hidden name="${dtoName}[${status.index}].genderConcordance" value="${dto.genderConcordance}"/>
            </td>
            <td>
                    ${dto.lod}
                    <stripes:hidden name="${dtoName}[${status.index}].lod" value="${dto.lod}"/>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>

<%--Build detail and hide--%>
<c:forEach items="${dtoList}" var="dto" varStatus="status">
    <table class="table borderless flowcellStatusTable" id="flowcellStatusTable-${dto.pdoSample}">
        <c:if test="${not empty dto.missingFlowcellStatuses}">
        <tr>
            <td><strong>Pending Lanes</strong></td>
        </tr>
            <c:forEach items="${dto.missingFlowcellStatuses}" var="flowcellStatus">
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
        <c:if test="${not empty dto.completedFlowcellStatuses}">
            <tr>
                <td><strong>Aggregated Lanes</strong></td>
            </tr>
            <c:forEach items="${dto.completedFlowcellStatuses}" var="flowcellStatus">
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
</c:forEach>