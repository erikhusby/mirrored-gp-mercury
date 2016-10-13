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

            .legend li {
                display: block;
                float: right;
                clear: right;
            }

            .legend span { margin: 5px; border: 1px solid #ccc; float: right; width: 12px; height: 12px;}
        </style>
        <script type="text/javascript">
            $j(document).ready(function () {
                <c:if test="${actionBean.labVessel != null}">
                    var json = ${actionBean.metricsTableJson};
                    $j('#platemapme').plateMap(json);
                </c:if>
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
                    <div id="platemapme">
                        <div class="row">
                            <div class="form-horizontal">
                                <div class="control-group">
                                    <stripes:label for="visualizations" class="control-label">Visualization(s)</stripes:label>
                                    <div class="controls">
                                        <select id='metricsList'></select>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="row">
                            <div class="span2">
                                <div id="legend">
                                </div>
                            </div>
                            <div class="span5">
                                <?xml version="1.0" encoding="UTF-8"?>
                                <table id="platemap" class="platemap table table-bordered table-condensed">
                                    <tbody>
                                        <tr>
                                            <th class="fit" ></th>
                                            <c:forEach items="${actionBean.labVessel.vesselGeometry.columnNames}" var="colName">
                                                <th class="fit">${colName}</th>
                                            </c:forEach>
                                        </tr>
                                        <c:forEach items="${actionBean.labVessel.vesselGeometry.rowNames}" var="rowName">
                                            <tr>
                                                <th>${rowName}</th>
                                                <c:forEach items="${actionBean.labVessel.vesselGeometry.columnNames}" var="colName">
                                                    <td id="${rowName}${colName}" class="metricCell"></td>
                                                </c:forEach>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                            <div class="span4">
                                <div class="well">
                                    Well Metadata
                                    <dl class="dl-horizontal" id="metadataDefinitionList">
                                    </dl>
                                </div>
                            </div>
                        </div>
                    </div>
                </c:if>
            </c:if>
        </div>
    </stripes:layout-component>
</stripes:layout-render>