<%@ page import="org.broadinstitute.gpinformatics.mercury.boundary.transfervis.TransferVisualizerV2" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
        beanclass="org.broadinstitute.gpinformatics.mercury.presentation.labevent.TransferVisualizerActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Transfer Visualizer" sectionTitle="Transfer Visualizer">

    <stripes:layout-component name="extraHead">
        <link rel="stylesheet"  href="${ctxpath}/resources/css/d3-context-menu.css"/>
        <script src="${ctxpath}/resources/scripts/D3/d3.3.5.6.min.js" type="text/javascript"></script>
        <script src="${ctxpath}/resources/scripts/D3/dagre.min.0.7.3.js" type="text/javascript"></script>
        <script src="${ctxpath}/resources/scripts/D3/d3-context-menu.js"></script>
        <script src="${ctxpath}/resources/scripts/D3/transfer_visualizer.js" type="text/javascript"></script>

        <script type="text/javascript">
            <c:if test="${not empty actionBean.jsonUrl}">
            $j(document).ready(
                    function () {
                        d3.json(<enhance:out escapeXml="false">"${ctxpath}${actionBean.jsonUrl}"</enhance:out>)
                                .on("progress", function() {
                                    d3.select("#progress").html("Bytes loaded: " + d3.event.loaded);
                                })
                                .get(function (error, json) {
                                    if (json.error) {
                                        alert(json.error);
                                    } else {
                                        renderJson(json);
                                    }
                                });
                    }
            );
            </c:if>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="eventForm">
            <div class="form-inline">
                <label for="barcodes" class="control-label">Barcodes</label>
                <textarea name="barcodes" id="barcodes">${actionBean.barcodes}</textarea>
                <c:forEach items="<%=TransferVisualizerV2.AlternativeIds.values()%>" var="alternateId">
                    <label class="checkbox">
                        <input type="checkbox" name="alternativeIds" value="${alternateId.toString()}"
                            ${actionBean.alternativeIds.contains(alternateId) ? 'checked="true"' : ''}>
                            ${alternateId.displayName}
                    </label>
                </c:forEach>
                <stripes:submit name="visualize" value="Visualize" class="btn btn-primary"/>
                <label id="progress"></label>
            </div>
        </stripes:form>
        <div id="graphDiv"></div>
    </stripes:layout-component>

</stripes:layout-render>
