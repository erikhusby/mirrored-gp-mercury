<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.MetricsViewActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Metrics View" sectionTitle="Metrics View">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j( "#heatField" ).change(function() {
                    var selectedField = $j("select#heatField option:selected").val();
                    console.log(selectedField);
                    var metricsMap = $j("#metrics").val();
                    console.log("MetricsMap= " + metricsMap);
                    var positionsToValue = metricsMap[selectedField];
                    if (positionsToValue != null) {
                        for (var key in positionsToValue) {
                            if (positionsToValue.hasOwnProperty(key)) {
                                var value = positionsToValue[key];
                                $j("#" + key).text(value);
                            }
                        }
                    }
                });
            });
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="scanForm" class="form-horizontal">
            <div class="form-horizontal">
                <div class="control-group">
                    <stripes:label for="lvIdentifierText" name="Vessel Barcode" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="lvIdentifierText" name="labVesselIdentifier"/>
                    </div>
                </div>
                <div class="control-group">
                    <div class="controls">
                        <stripes:submit name="search" value="Search" class="btn btn-primary"/>
                    </div>
                </div>
            </div>
            <stripes:hidden id="metrics"  name="metricToPositionToValue"
                            value="${actionBean.metricToPositionToValue}"/>
        </stripes:form>
        <div id="searchResults">
            <c:if test="${actionBean.foundResults}">
                <c:if test="${actionBean.labVessel != null}">
                    <table class="platemap">
                        <thead>
                            <tr>
                                <th></th>
                                <c:forEach items="${actionBean.labVessel.vesselGeometry.columnNames}" var="colName">
                                    <th>${colName}</th>
                                </c:forEach>
                            </tr>
                        </thead>
                        <c:forEach items="${actionBean.labVessel.vesselGeometry.rowNames}" var="rowName">
                            <tr>
                                <th>${rowName}</th>
                                <c:forEach items="${actionBean.labVessel.vesselGeometry.columnNames}" var="colName">
                                    <%--<td>${actionBean.positionToQc[rowName.concat(colName)].callRate}</td>--%>
                                    <td><span id="${rowName}${colName}"/></td>
                                </c:forEach>
                            </tr>
                        </c:forEach>
                    </table>
                    <stripes:layout-render name="/container/heat_map.jsp" actionBean="${actionBean}"/>
                </c:if>
            </c:if>
        </div>
    </stripes:layout-component>
</stripes:layout-render>