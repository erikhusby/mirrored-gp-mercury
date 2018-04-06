<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.workflow.LcsetActionBean" %>
<%--
  This page allows a user to declare the positive and negative controls in an LCSET.
--%>
<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
        beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.LcsetActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="LCSET" sectionTitle="LCSET">
    <stripes:layout-component name="extraHead">
        <%@ include file="/vessel/rack_scanner_list_with_sim_part1.jsp" %>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="selectionForm" class="form-horizontal">
            <div class="form-horizontal">
                <div class="control-group">
                    <label for="lcsetText" class="control-label">LCSet Name</label>
                    <div class="controls">
                        <input type="text" id="lcsetText" name="lcsetName" value="${actionBean.lcsetName}"/>
                    </div>
                </div>
                <div class="control-group">
                    <label for="rackBarcode" class="control-label">Rack Barcode</label>
                    <div class="controls">
                        <input type="text" id="rackBarcode" name="rackBarcode" value="${actionBean.rackBarcode}"/>
                    </div>
                </div>
                <!-- Adds the dropdowns for lab and scanner, and possibly a file chooser. -->
                <stripes:layout-render name="/vessel/rack_scanner_list_with_sim_part2.jsp" bean="${actionBean}"/>
                <div class="controls">
                    <stripes:submit value="Scan" id="scanBtn" class="btn btn-primary"
                            name="<%= LcsetActionBean.SCAN_CONTROLS_EVENT %>"/>
                </div>
            </div>

            <c:if test="${not empty actionBean.controlBarcodes or not empty actionBean.addBarcodes or not empty actionBean.removeBarcodes}">
                <table class="table simple">
                    <tr>
                        <th></th>
                    <c:forEach items="${actionBean.vesselGeometry.columnNames}" var="columnName">
                        <th>${columnName}</th>
                    </c:forEach>
                    </tr>
                    <c:forEach items="${actionBean.vesselGeometry.vesselPositions}" var="vesselPosition" varStatus="status">
                        <%--@elvariable id="vesselPosition" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition"--%>
                        <c:if test="${status.index % actionBean.vesselGeometry.columnCount == 0}">
                            <tr>
                            <td>${fn:substring(vesselPosition, 0, 1)}</td>
                        </c:if>
                        <td style='${actionBean.controlBarcodes.contains(actionBean.rackScan[vesselPosition.name()]) ? "background-color:yellow" : ""}'>
                            ${actionBean.rackScan[vesselPosition.name()]}&nbsp;
                        </td>
                        <c:if test="${status.index % actionBean.vesselGeometry.columnCount == actionBean.vesselGeometry.columnCount - 1}">
                            </tr>
                        </c:if>
                    </c:forEach>
                </table>
                Click Confirm to make the following changes:<br/>
                <c:if test="${not empty actionBean.controlBarcodes}">
                    Add the following controls to the LCSET:
                    <c:forEach items="${actionBean.controlBarcodes}" var="controlBarcode" varStatus="loop">
                        <div>
                            ${controlBarcode}
                            <input type="hidden" name="controlBarcodes[${loop.index}]" value="${controlBarcode}"/>
                        </div>
                    </c:forEach>
                </c:if>
                <c:if test="${not empty actionBean.addBarcodes}">
                    Add the following samples to the LCSET:
                    <c:forEach items="${actionBean.addBarcodes}" var="addBarcode" varStatus="loop">
                        <div>
                                ${addBarcode}
                            <input type="hidden" name="addBarcodes[${loop.index}]" value="${addBarcode}"/>
                        </div>
                    </c:forEach>
                </c:if>
                <c:if test="${not empty actionBean.removeBarcodes}">
                    Remove the following samples from the LCSET, and place them in the bucket:
                    <c:forEach items="${actionBean.removeBarcodes}" var="removeBarcode" varStatus="loop">
                        <div>
                                ${removeBarcode}
                            <input type="hidden" name="removeBarcodes[${loop.index}]" value="${removeBarcode}"/>
                        </div>
                    </c:forEach>
                </c:if>
                <div class="controls">
                    <stripes:submit value="Confirm" id="confirmBtn" class="btn btn-primary"
                            name="<%= LcsetActionBean.CONFIRM_CONTROLS_EVENT %>"/>
                </div>
                <c:forEach items="${actionBean.rackScan}" var="entry">
                    <input type="hidden" name="rackScan['${entry.key}']" value="${entry.value}">
                </c:forEach>
            </c:if>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
