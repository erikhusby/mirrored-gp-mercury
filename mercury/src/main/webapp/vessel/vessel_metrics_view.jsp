<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.MetricsViewActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Metrics View" sectionTitle="Metrics View">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {

                function applyHeatMapChange() {
                    console.log("Applying Heat Map");
                    // TODO really don't want heat map
                    $j('.heatable').heatcolor(
                            function () {
                                return $j(this).text();
                            },
                            {
                                lightness: $j("#amount").val() / 100,
                                colorstyle: 'greentored',
                                reverseOrder: true,
                                maxval: 0,
                                minval: 100 //TODO these need to be set dynamically
                            }
                    );
                }

                $j( "#heatField" ).change(function() {
                    var selectedField = $j("select#heatField option:selected").val();
                    var metricsMap =${actionBean.metricToPositionToValueJson};
                    var plotTypes =${actionBean.metricToPlotJson};
                    if (metricsMap != undefined) {
                        var positionsToValue = metricsMap[selectedField];
                        if (positionsToValue != null) {
                            for (var key in positionsToValue) {
                                if (positionsToValue.hasOwnProperty(key)) {
                                    var value = positionsToValue[key];
                                    if (value.toString().startsWith("0.")) {
                                        value = parseFloat(value * 100).toFixed(2);
                                    }
                                    $j("#" + key).text(value);
                                }
                            }
                        }
                    }
                    applyHeatMapChange();
                });

                $j('#colorSlider').slider({
                    range:"min",
                    value:45,
                    min:0,
                    max:90,
                    slide:function (event, ui) {
                        applyHeatMapChange();
                        $j('#amount').val(ui.value);
                    }

                });
                $j('#amount').val($j('#colorSlider').slider('value'));
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
        </stripes:form>
        <div id="searchResults">
            <c:if test="${actionBean.foundResults}">
                <c:if test="${actionBean.labVessel != null}">
                    <div class="form-horizontal">
                        <div class="control-group"
                            <stripes:label for="selectedHeatField" class="control-label">
                                Metric Type
                            </stripes:label>
                            <div class="controls">
                                <select name="selectedHeatField" id="heatField">
                                    <c:forEach items="${actionBean.heatMapFields}" var="field">
                                        <option value="${field}">${field}</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <stripes:label for="amount" name="Color Intensity" class="control-label"/>
                            <div class="controls">
                                <div id="colorSlider"></div>
                                <input type="hidden" id="amount" disabled="true" size="3" value=".45">
                            </div>
                        </div>
                    </div>
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
                                    <td><div class="heatable" id="${rowName}${colName}"></div></td>
                                </c:forEach>
                            </tr>
                        </c:forEach>
                    </table>
                </c:if>
            </c:if>
        </div>
    </stripes:layout-component>
</stripes:layout-render>