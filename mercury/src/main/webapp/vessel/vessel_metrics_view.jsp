<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.MetricsViewActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Metrics View" sectionTitle="Metrics View">
    <stripes:layout-component name="extraHead">
        <style type="text/css">
            .platemap {
                width: auto;
            }

            .platemap td {
                width: 35px;
            }

            .platemapcontainer {
                border-top: 1px solid #EEEEEE;
                padding-top: 15px;
            }

            .legend li {
                display: block;
                float: right;
                clear: right;
            }

            .legend span { margin: 5px; border: 1px solid #ccc; float: right; width: 12px; height: 12px;}
        </style>
        <script src="${ctxpath}/resources/scripts/plateMap.js"></script>
        <script type="text/javascript">
            $j(document).ready(function () {
                <c:if test="${actionBean.foundResults}">
                    var platemaps = ${actionBean.metricsTableJson};
                    var json = {};
                    json.platemaps = platemaps;
                    console.log(json);
                    $j('#platemaps').plateMap(json);
                </c:if>
            });
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="scanForm" class="form-horizontal">
            <div class="form-horizontal">
                <div class="control-group">
                    <stripes:label for="lvIdentifiersText" name="Vessel Barcodes" class="control-label"/>
                    <div class="controls">
                        <stripes:textarea id="lvIdentifierTexts" name="barcodes"/>
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
                <div id="platemaps">
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
                                        <div class="well">
                                            Well Metadata
                                            <dl class="metadataDefinitionList dl-horizontal">
                                            </dl>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </c:if>
                    </c:forEach>
                </div>
            </c:if>
        </div>
    </stripes:layout-component>
</stripes:layout-render>