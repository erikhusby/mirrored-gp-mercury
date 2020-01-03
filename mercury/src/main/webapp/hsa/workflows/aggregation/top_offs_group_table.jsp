<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.hsa.AggregationTriageActionBean" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.hsa.TopOffActionBean"--%>
<stripes:form beanclass="${actionBean.class.name}" id="topOffGroupsForm" class="form-horizontal" method="POST">
    <table id="poolGroupsTable" class="table simple">
        <thead>
        <tr>
            <th>Group ID</th>
            <th>Lanes Needed</th>
            <th>Max X Needed</th>
            <th width="30px">
                <input type="checkbox" class="poolGroupsTable-checkAll" title="Check All"/>
                <span id="poolGroupsTable-count" class="poolGroupsTable-checkedCount"></span>
            </th>
            <th>Library</th>
            <th>LCSET</th>
            <th>Seq Type</th>
            <th>PDO Sample</th>
            <th>Index</th>
            <th>Is Clinical?</th>
            <th>X Needed</th>
        </tr>
        </thead>
        <tbody class="context-menu-poolGroupsTable">
        <c:forEach items="${actionBean.poolGroups}" var="poolGroup" varStatus="status">
            <c:set var="rowSpan" value="${poolGroup.count}"/>
            <c:set var="evenOrOdd" value="${status.index % 2 == 0 ? 'even' : 'odd'}"/>
            <tr class="${evenOrOdd}">
                <td rowspan="${rowSpan}">
                    ${poolGroup.groupId}
                        <stripes:hidden name="poolGroups[${status.index}].groupId" value="${poolGroup.groupId}"/>
                </td>
                <td rowspan="${rowSpan}" class="lanesNeeded" data-count="${poolGroup.count}" data-default-yield="${poolGroup.defaultExpectedYieldPerLane}" data-max-x="${poolGroup.maxXNeeded}">
                    <div>${poolGroup.lanesNeeded}</div>
                    <stripes:hidden name="poolGroups[${status.index}].lanesNeeded" value="0"/>
                    <stripes:hidden name="poolGroups[${status.index}].count" value="${poolGroup.count}"/>
                </td>
                <td rowspan="${rowSpan}">
                        ${poolGroup.maxXNeeded}
                            <stripes:hidden name="poolGroups[${status.index}].maxXNeeded" value="${poolGroup.maxXNeeded}"/>
                </td>
                <c:forEach items="${poolGroup.topOffDtos}" var="dto" varStatus="innerStatus">
                    <c:if test="${innerStatus.index > 0}">
                        <tr class="${evenOrOdd}">
                    </c:if>
                    <td>
                        <stripes:checkbox name="selectedSamples" class="poolGroupsTable-checkbox"
                                          value="${dto.pdoSample}"/>
                    </td>
                    <td>
                            ${dto.library}
                        <stripes:hidden name="mapTabToDto['Pool Groups'].[${innerStatus.index}].library" value="${dto.library}"/>
                        <stripes:hidden name="poolGroups[${status.index}].topOffDtos[${innerStatus.index}].library" value="${dto.library}"/>
                    </td>
                    <td>
                            ${dto.topOffLcset}
                        <stripes:hidden name="mapTabToDto['Pool Groups'].[${innerStatus.index}].topOffLcset" value="${dto.topOffLcset}"/>
                        <stripes:hidden name="poolGroups[${status.index}].topOffDtos[${innerStatus.index}].topOffLcset" value="${dto.topOffLcset}"/>
                    </td>
                    <td>
                        ${dto.seqType}
                        <stripes:hidden name="poolGroups[${status.index}].topOffDtos[${innerStatus.index}].seqType" value="${dto.seqType}"/>
                    </td>
                    <td>
                            ${dto.pdoSample}
                        <stripes:hidden name="mapTabToDto['Pool Groups'].[${innerStatus.index}].pdoSample" value="${dto.pdoSample}"/>
                        <stripes:hidden name="poolGroups[${status.index}].topOffDtos[${innerStatus.index}].pdoSample" value="${dto.pdoSample}"/>
                    </td>
                    <td class="myIndex">
                            ${dto.index}
                        <stripes:hidden name="mapTabToDto['Pool Groups'].[${innerStatus.index}].index" value="${dto.index}"/>
                        <stripes:hidden name="poolGroups[${status.index}].topOffDtos[${innerStatus.index}].index" value="${dto.index}"/>
                    </td>
                    <td>
                            ${dto.clinical}
                        <stripes:hidden name="mapTabToDto['Pool Groups'].[${innerStatus.index}].clinical" value="${dto.clinical}"/>
                        <stripes:hidden name="poolGroups[${status.index}].topOffDtos[${innerStatus.index}].clinical" value="${dto.clinical}"/>
                    </td>
                    <td>
                            ${dto.xNeeded}
                        <stripes:hidden name="mapTabToDto['Pool Groups'].[${innerStatus.index}].xNeeded" value="${dto.xNeeded}"/>
                        <stripes:hidden name="poolGroups[${status.index}].topOffDtos[${innerStatus.index}].xNeeded" value="${dto.xNeeded}"/>
                    </td>
                    <c:if test="${innerStatus.index < innerStatus.end}">
                        </tr>
                    </c:if>
                </c:forEach>
            </tr>
        </c:forEach>
        </tbody>
    </table>
    <div class="control-group">
        <label class="control-label" for="poolingPenalty">Pooling Penalty</label>
        <div class="controls">
            <input type="text" class="number" id="poolingPenalty" placeholder="defaults to 1" value="1">
        </div>
    </div>
    <div class="control-group">
        <label class="control-label" for="expectedYield">Expected Yield</label>
        <div class="controls">
            <input type="text" class="number" id="expectedYield" placeholder="overrides seq type defaults">
        </div>
    </div>
    <div class="control-group">
        <div class="controls">
            <stripes:hidden name="sequencingType" value="PoolGroups"/>
            <stripes:submit name="removeFromPoolGroup" id="removeFromPoolGroup" value="Remove From Pool Group" class="btn btn-primary"/>
            <stripes:submit name="markComplete" id="markComplete"  value="Mark Complete" class="btn btn-primary"/>
            <stripes:submit name="downloadPoolGroups" id="downloadPoolGroups" value="Download" class="btn"/>
        </div>
    </div>
</stripes:form>