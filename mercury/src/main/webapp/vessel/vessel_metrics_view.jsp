<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.vessel.MetricsViewActionBean" %>
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

            .noSample {
                background-color: lightgray;
                background-image: repeating-linear-gradient(45deg, transparent, transparent 5px, rgba(255,255,255,.5) 5px,
                                                            rgba(255,255,255,.5) 10px);
            }

            .blacklisted {
                background-image:url("../images/blacklist.png");
                background-position: right center;
                background-repeat: no-repeat;
            }

            .abandoned {
                background-image:url("../images/abandon.png");
                background-position: right center;
                background-repeat: no-repeat;
            }
        </style>
        <script src="${ctxpath}/resources/scripts/plateMap.js"></script>
        <script type="text/javascript">
            $j(document).ready(function () {
                <c:if test="${actionBean.foundResults}">
                    <enhance:out escapeXml="false">
                    var platemaps = ${actionBean.metricsTableJson};
                    </enhance:out>
                    var json = {};
                    json.platemaps = platemaps;
                    console.log(json);
                    $j('#platemaps').plateMap(json);
                </c:if>
            });
        </script>
        <%@ include file="/vessel/rack_scanner_list_with_sim_part1.jsp" %>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <div class="row">
            <div class="span6">
                <stripes:form beanclass="${actionBean.class.name}" id="scanForm" class="form-horizontal">
                    <div class="form-horizontal">
                        <fieldset>
                            <legend><h5>Enter plate or chip barcodes below for infinium metrics</h5></legend>
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
                        </fieldset>
                    </div>
                </stripes:form>
            </div>
            <div class="span6">
                <stripes:form beanclass="${actionBean.class.name}" id="scanForm" class="form-horizontal">
                    <div class="form-horizontal">
                        <fieldset>
                            <legend><h5>Use the form below for metrics from a rack scan</h5></legend>
                            <stripes:layout-render name="/vessel/rack_scanner_list_with_sim_part2.jsp" bean="${actionBean}"/>
                            <div class="controls">
                                <stripes:submit value="Scan" id="scanBtn" class="btn btn-primary"
                                                name="<%= MetricsViewActionBean.RACK_SCAN_EVENT %>"/>
                            </div>
                        </fieldset>
                    </div>
                </stripes:form>
            </div>
        </div>
        <div class="row">
            <div id="searchResults">
                <c:if test="${actionBean.foundResults}">
                    <c:set var="staticPlates" value="${actionBean.staticPlates}"/>
                    <div id="platemaps">
                        <c:choose>
                            <c:when test="${empty staticPlates}">
                                <jsp:include page="vessel_metrics_tubes_view.jsp"/>
                            </c:when>
                            <c:otherwise>
                                <jsp:include page="vessel_metrics_plates_view.jsp"/>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </c:if>
            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>