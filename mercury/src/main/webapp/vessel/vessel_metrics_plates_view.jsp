<%--
This fragment is used by vessel_metrics_view.jsp to re-use code when displaying plates
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.vessel.MetricsViewActionBean"--%>

<c:forEach items="${actionBean.staticPlates}" var="labVessel" varStatus="loop">
    <c:if test="${labVessel != null}">
        <div id="platemap[${loop.index}]" class="platemapcontainer">
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
                <div class="span2">
                    <div class="legend">
                    </div>
                </div>
                <div class="span5">
                    <?xml version="1.0" encoding="UTF-8"?>
                    <table class="platemap table table-bordered table-condensed">
                        <tbody>
                        <tr>
                            <th colspan="${labVessel.vesselGeometry.columnCount + 1}"
                                style="text-align: center">${labVessel.label}</th>
                        </tr>
                        <tr>
                            <th class="fit" ></th>
                            <c:forEach items="${labVessel.vesselGeometry.columnNames}"
                                       var="colName">
                                <th class="fit">${colName}</th>
                            </c:forEach>
                        </tr>
                        <c:forEach items="${labVessel.vesselGeometry.rowNames}"
                                   var="rowName">
                            <tr>
                                <th>${rowName}</th>
                                <c:forEach items="${labVessel.vesselGeometry.columnNames}"
                                           var="colName">
                                    <td id="${labVessel.label}_${rowName}${colName}"
                                        class="metricCell"></td>
                                </c:forEach>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <div class="span4">
                    <div class="well" style="height: 335px">
                        Vessel Metadata
                        <dl class="metadataDefinitionPlateList dl-horizontal">
                        </dl>
                        Well Metadata
                        <dl class="metadataDefinitionList dl-horizontal">
                        </dl>
                    </div>
                </div>
            </div>
        </div>
    </c:if>
</c:forEach>