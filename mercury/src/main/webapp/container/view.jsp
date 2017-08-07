<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.container.ContainerActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="View Container" sectionTitle="View Container: ${actionBean.viewVessel.label}"
                       businessKeyValue="${actionBean.containerBarcode}">

    <stripes:layout-component name="extraHead">
        <%@ include file="/vessel/rack_scanner_list_with_sim_part1.jsp" %>
        <link rel="stylesheet"
              href="${ctxpath}/resources/scripts/jsTree/themes/default/style.min.css"/>
        <script src="${ctxpath}/resources/scripts/jsTree/jstree.min.js"></script>
        <style type="text/css">
            .platemap {
                width: auto;
            }

            .platemap td {
                width: 35px;
            }

            .platemapcontainer {
                padding-top: 15px;
            }

            .sample {
                background-color: #d9edf7
            }

            label {
                display: inline;
                font-weight: bold;
            }
            input[type="text"].smalltext {
                width: 70px;
                font-size: 12px;
                padding: 2px 2px;
            }
            input[type='text'].barcode {
                width: 100px;
                font-size: 12px;
            }

            .top-buffer { margin-top:20px; }
        </style>
        <script src="${ctxpath}/resources/scripts/storage-location-ajax.js"></script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" method="GET"
                      id="createContainerForm" class="form-horizontal">
            <fieldset>
                <legend>Find Container By Barcode</legend>
                <div class="control-group">
                    <stripes:label for="containerBarcode" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="containerBarcode" name="containerBarcode"/>
                        <stripes:submit id="searchForContainer" name="viewContainer" value="Find" class="btn btn-primary"/>
                    </div>
                </div>
            </fieldset>
        </stripes:form>
        <c:if test="${not empty actionBean.viewVessel}">
            <c:set var="geometry" value="${actionBean.viewVessel.vesselGeometry}" scope="request"/>
            <jsp:include page="container_view.jsp"/>
        </c:if>
    </stripes:layout-component>
</stripes:layout-render>