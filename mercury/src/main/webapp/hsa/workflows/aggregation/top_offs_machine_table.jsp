<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.hsa.AggregationTriageActionBean" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%--@elvariable id="tableId" type="java.lang.String--%>
<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.hsa.TopOffActionBean"--%>
<%--@elvariable id="dtoList" type="java.util.List<org.broadinstitute.gpinformatics.mercury.presentation.hsa.TopOffActionBean.HoldForTopoffDto>"--%>
<%--@elvariable id="machineType" type="java.lang.String"--%>
<%--@elvariable id="includeClear" type="java.lang.Boolean"--%>
<stripes:form beanclass="${actionBean.class.name}" id="topOffForm-${tableId}" class="form-horizontal" method="POST">
    <table id="${tableId}" class="table simple">
        <thead>
        <tr>
            <th width="30px">
                <input type="checkbox" class="${tableId}-checkAll" title="Check All"/>
                <span id="hiseq-count" class="${tableId}-checkedCount"></span>
            </th>
            <th>Library</th>
            <th>PDO Sample</th>
            <th>Index</th>
            <th>X Needed</th>
            <th>Is Clinical?</th>
            <th>Volume</th>
            <th>Storage</th>
        </tr>
        </thead>
        <tbody class="context-menu-${tableId}">
        <c:forEach items="${dtoList}" var="dto" varStatus="status">
            <tr>
                <stripes:hidden name="mapTabToDto['${machineType}'].[${status.index}].seqType" value="${dto.seqType}"/>
                <stripes:hidden name="mapTabToDto['${machineType}'].[${status.index}].lcset" value="${dto.lcset}"/>
                <stripes:hidden name="mapTabToDto['${machineType}'].[${status.index}].topOffLcset" value="${dto.topOffLcset}"/>
                <stripes:hidden name="mapTabToDto['${machineType}'].[${status.index}].pdo" value="${dto.pdo}"/>
                <td>
                    <stripes:checkbox name="selectedSamples" class="${tableId}-checkbox"
                                      value="${dto.pdoSample}"/>
                </td>
                <td>
                        ${dto.library}
                    <stripes:hidden name="mapTabToDto['${machineType}'].[${status.index}].library" value="${dto.library}"/>
                </td>
                <td>
                        ${dto.pdoSample}
                    <stripes:hidden name="mapTabToDto['${machineType}'].[${status.index}].pdoSample" value="${dto.pdoSample}"/>
                </td>
                <td class="myIndex">
                        ${dto.index}
                    <stripes:hidden name="mapTabToDto['${machineType}'].[${status.index}].index" value="${dto.index}"/>
                </td>
                <td>
                        ${dto.xNeeded}
                    <stripes:hidden name="mapTabToDto['${machineType}'].[${status.index}].xNeeded" value="${dto.xNeeded}"/>
                </td>
                <td>
                        ${dto.clinical}
                    <stripes:hidden name="mapTabToDto['${machineType}'].[${status.index}].clinical" value="${dto.clinical}"/>
                </td>
                <td>
                        ${dto.volume}
                    <stripes:hidden name="mapTabToDto['${machineType}'].[${status.index}].volume" value="${dto.volume}"/>
                </td>
                <td>
                        ${dto.storage}
                    <stripes:hidden name="mapTabToDto['${machineType}'].[${status.index}].storage" value="${dto.storage}"/>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
    <c:forEach items="${actionBean.mapSampleToDto.values()}" var="dto">
        <stripes:hidden name="mapSampleToDto['${dto.pdoSample}'].seqType" value="${dto.seqType}"/>
        <stripes:hidden name="mapSampleToDto['${dto.pdoSample}'].lcset" value="${dto.lcset}"/>
        <stripes:hidden name="mapSampleToDto['${dto.pdoSample}'].topOffLcset" value="${dto.topOffLcset}"/>
        <stripes:hidden name="mapSampleToDto['${dto.pdoSample}'].pdo" value="${dto.pdo}"/>
        <stripes:hidden name="mapSampleToDto['${dto.pdoSample}'].pdoSample" value="${dto.pdoSample}"/>
        <stripes:hidden name="mapSampleToDto['${dto.pdoSample}'].library" value="${dto.library}"/>
        <stripes:hidden name="mapSampleToDto['${dto.pdoSample}'].index" value="${dto.index}"/>
        <stripes:hidden name="mapSampleToDto['${dto.pdoSample}'].xNeeded" value="${dto.xNeeded}"/>
        <stripes:hidden name="mapSampleToDto['${dto.pdoSample}'].clinical" value="${dto.clinical}"/>
        <stripes:hidden name="mapSampleToDto['${dto.pdoSample}'].volume" value="${dto.volume}"/>
        <stripes:hidden name="mapSampleToDto['${dto.pdoSample}'].storage" value="${dto.storage}"/>
    </c:forEach>
    <div class="control-group">
        <div class="controls">
            <stripes:hidden name="sequencingType" value="${machineType}"/>
            <c:choose>
                <c:when test="${machineType != 'Sent To Rework'}">
                    <stripes:submit name="createTopOffGroup" value="Create Top Off Group" id="${tableId}-createTopOffGroup" class="btn btn-primary ajaxSubmit"/>
                    <stripes:submit name="sendToRework" value="Send To Rework" id="${tableId}-sendToRework" class="btn btn-primary ajaxSubmit"/>
                    <stripes:submit name="sendToHolding" value="Send Back To Holding" id="${tableId}-sendToHolding" class="btn btn-primary ajaxSubmit"/>
                    <stripes:submit name="downloadPickList" value="Download Picklist" id="${tableId}-downloadPickList" class="btn ajaxSubmit"/>
                </c:when>
                <c:otherwise>
                    <stripes:submit name="sendBackToSeqQueue" value="Back To Sequencing Queue" class="btn btn-primary ajaxSubmit"/>
                    <stripes:submit name="clearRework" value="Clear" class="btn btn-primary ajaxSubmit"/>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</stripes:form>