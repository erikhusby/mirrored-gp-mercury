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
        <div class="row">
            <div class="span6">
                <stripes:form beanclass="${actionBean.class.name}" id="selectionForm" class="form-horizontal">
                    <div class="form-horizontal">
                        <fieldset>
                            <legend><h5>Use the form below for batches that can be updated via rack scan.</h5></legend>
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
                        </fieldset>
                    </div>

                    <c:if test="${ not empty actionBean.rackScan and (not empty actionBean.controlBarcodes or not empty actionBean.addBarcodes or not empty actionBean.removeBarcodes)}">
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
                        <c:set var="batchName" value="${actionBean.lcsetName}" scope="request"/>
                        <jsp:include page="lcset_control_confirm.jsp"/>
                        <div class="controls">
                            <stripes:submit value="Confirm" id="confirmBtn" class="btn btn-primary"
                                            name="<%= LcsetActionBean.CONFIRM_CONTROLS_EVENT %>"/>
                        </div>
                        <c:forEach items="${actionBean.rackScan}" var="entry">
                            <input type="hidden" name="rackScan['${entry.key}']" value="${entry.value}">
                        </c:forEach>
                    </c:if>
                </stripes:form>
            </div>
            <div class="span6">
                <stripes:form beanclass="${actionBean.class.name}" id="controlInputForm" class="form-horizontal">
                    <div class="form-horizontal">
                        <fieldset>
                            <legend><h5>Otherwise, scan control barcodes manually</h5></legend>
                            <div class="control-group">
                                <label for="labBatchText" class="control-label">Lab Batch Name</label>
                                <div class="controls">
                                    <input type="text" id="labBatchText" name="labBatchName" value="${actionBean.labBatchName}"/>
                                </div>
                            </div>
                            <div class="control-group">
                                <label for="controlsTextArea" class="control-label">Lab Vessel Barcodes To Add</label>
                                <div class="controls">
                                    <textarea id="controlsTextArea" name="controls" cols="80" rows="4">${actionBean.controls}</textarea>
                                </div>
                            </div>
                            <div class="controls">
                                <stripes:submit value="Submit" id="typeSubmitBtn" class="btn btn-primary"
                                                name="<%= LcsetActionBean.TYPE_CONTROLS_EVENT %>"/>
                            </div>
                            <c:if test="${empty actionBean.rackScan and (not empty actionBean.controlBarcodes or not empty actionBean.addBarcodes or not empty actionBean.removeBarcodes)}">
                                <c:set var="batchName" value="${actionBean.labBatchName}" scope="request"/>
                                <jsp:include page="lcset_control_confirm.jsp"/>
                                <div class="controls">
                                    <stripes:submit value="Confirm" id="typeConfirmBtn" class="btn btn-primary"
                                                    name="<%= LcsetActionBean.CONFIRM_TYPED_CONTROLS_EVENT %>"/>
                                </div>
                            </c:if>
                        </fieldset>
                    </div>
                </stripes:form>
            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
