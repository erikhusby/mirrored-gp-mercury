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
        <th>PDO Sample</th>
        <th>Library</th>
        <th>PDO</th>
        <th>Cov >= 95%@20X</th>
        <th>% Contamination</th>
        <th>Aligned Q20 Bases</th>
        <th>Sex@Birth</th>
        <th>Finished Read Groups/Pending</th>
        <th>Processing Status</th>
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
            <td>
                    ${dto.pdoSample}
                    <stripes:hidden name="${dtoName}[${status.index}].pdoSample" value="${dto.pdoSample}"/>
            </td>
            <td>
                    ${dto.library}
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
                    ${dto.processingStatus}
                    <stripes:hidden name="${dtoName}[${status.index}].processingStatus" value="${dto.processingStatus}"/>
            </td>
            <td>
                    ${dto.lod}
                    <stripes:hidden name="${dtoName}[${status.index}].lod" value="${dto.lod}"/>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>