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

            .legend { display: block; }

            .legend li { display: inline-block;}

            .legend span { margin: 10px; border: 1px solid #ccc; float: left; width: 12px; height: 12px;}
        </style>
        <script type="text/javascript">
            $j(document).ready(function () {
                <c:if test="${actionBean.labVessel != null}">
                    var json = ${actionBean.metricsTableJson};
                    console.log(json);
                    $j('#platemapme').platemap(json);
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
                    <div id="platemapme"></div>
                </c:if>
            </c:if>
        </div>
    </stripes:layout-component>
</stripes:layout-render>