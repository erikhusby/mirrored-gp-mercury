<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
        beanclass="org.broadinstitute.gpinformatics.mercury.presentation.labevent.TransferVisualizerActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Transfer Visualizer" sectionTitle="Transfer Visualizer">

    <stripes:layout-component name="extraHead">
        <script src="${ctxpath}/resources/scripts/d3.3.5.6.min.js" type="text/javascript"></script>
        <script src="${ctxpath}/resources/scripts/dagre.min.0.7.3.js" type="text/javascript"></script>
        <script src="${ctxpath}/resources/scripts/transfer_visualizer.js" type="text/javascript"></script>

        <script type="text/javascript">
            <c:if test="${not empty actionBean.barcodes}">
            $j(document).ready(
                    function () {
                        var svg = setupSvg();
                        d3.json("${ctxpath}/labevent/transfervis.action?getJson=&barcodes=${actionBean.barcodes[0]}", function (error, json) {
                            renderJson(json, svg);
                        });
                    }
            );
            </c:if>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="eventForm">
            <div class="control-group">
                <label for="barcodes">Barcodes</label>
                <input type="text" name="barcodes" id="barcodes">
                <stripes:submit name="visualize" value="Visualize" class="btn btn-primary"/>
            </div>
        </stripes:form>
        <div id="graphDiv"></div>
    </stripes:layout-component>

</stripes:layout-render>
