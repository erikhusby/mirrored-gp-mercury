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
            <c:if test="${not empty actionBean.barcodes}">
            $j(document).ready(
                    function () {
                        d3.json("${ctxpath}/labevent/transfervis.action?getJson=&barcodes=${actionBean.barcodes[0]}")
                                .on("progress", function() {
                                    d3.select("#graphDiv").html("Bytes loaded: " + d3.event.loaded);
                                })
                                .get(function (error, json) {
                                    renderJson(json);
                                });
                    }
            );
            </c:if>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="eventForm">
            <div class="form-horizontal">
                <div class="control-group">
                    <label for="barcodes" class="control-label">Barcodes</label>
                    <div class="controls">
                        <input type="text" name="barcodes" id="barcodes">
                        <stripes:submit name="visualize" value="Visualize" class="btn btn-primary"/>
                        <div id="progress"></div>
                    </div>
                </div>
            </div>
        </stripes:form>
        <div id="graphDiv"></div>
    </stripes:layout-component>

</stripes:layout-render>
