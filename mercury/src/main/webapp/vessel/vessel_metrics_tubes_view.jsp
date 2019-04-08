<%@ page import="org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry" %>
<%--
This fragment is used by vessel_metrics_view.jsp to re-use code when displaying a rack scan
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.vessel.MetricsViewActionBean"--%>

<c:set var="vesselGeometry" value="<%=VesselGeometry.G12x8%>"/>
<div id="platemap[0]" class="platemapcontainer">
    <div class="row">
        <div class="form-horizontal">
            <div class="control-group">
                <stripes:label for="visualizations" class="control-label">
                    Visualization(s)
                </stripes:label>
                <div class="controls">
                    <select class='metricsList'></select>
                </div>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="span3">
            <div class="legend">
            </div>
        </div>
        <div class="span5">
            <?xml version="1.0" encoding="UTF-8"?>
            <table class="platemap table table-bordered table-condensed">
                <tbody>
                <tr>
                    <th colspan="${vesselGeometry.columnCount + 1}"
                        style="text-align: center">Rack</th>
                </tr>
                <tr>
                    <th class="fit" ></th>
                    <c:forEach items="${vesselGeometry.columnNames}" var="colName">
                        <th class="fit">${colName}</th>
                    </c:forEach>
                </tr>
                <c:forEach items="${vesselGeometry.rowNames}" var="rowName" varStatus="rowStatus">
                        <th>${rowName}</th>
                        <c:forEach items="${vesselGeometry.columnNames}" var="colName" varStatus="columnStatus">
                            <c:set var="receptacleIndex"
                                   value="${rowStatus.index * vesselGeometry.columnCount + columnStatus.index}"/>
                            <c:set var="well" value="${vesselGeometry.vesselPositions[receptacleIndex]}"/>
                            <c:set var="labVessel" value="${actionBean.mapPositionToVessel.get(well)}"/>
                            <c:choose>
                                <c:when test="${labVessel != null}">
                                    <td id="${well}" class="metricCell"></td>
                                </c:when>
                                <c:otherwise>
                                    <td id="${rowName}${colName}" class="emptyCell"></td>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
        <div class="span4">
            <div class="well" style="height: 335px">
                Well Metadata
                <dl class="metadataDefinitionList dl-horizontal">
                </dl>
            </div>
        </div>
    </div>
</div>