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
                    <stripes:label for="lcsetText" name="LCSet Name" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="lcsetText" name="lcsetName"/>
                    </div>
                </div>
                <!-- Adds the dropdowns for lab and scanner, and possibly a file chooser. -->
                <stripes:layout-render name="/vessel/rack_scanner_list_with_sim_part2.jsp" bean="${actionBean}"/>
                <div class="controls">
                    <stripes:submit value="Scan" id="scanBtn" class="btn btn-primary"
                            name="<%= LcsetActionBean.SCAN_CONTROLS_EVENT %>"/>
                </div>
            </div>

            <c:if test="${not empty actionBean.controlBarcodes}">
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
                Add the following to the LCSET:
                <c:forEach items="${actionBean.controlBarcodes}" var="controlBarcode" varStatus="loop">
                    <div>
                    ${controlBarcode}
                    <stripes:hidden name="controlBarcodes[${loop.index}]" value="${controlBarcode}"/>
                    </div>
                </c:forEach>
                <div class="controls">
                    <stripes:submit value="Confirm Controls" id="confirmBtn" class="btn btn-primary"
                            name="<%= LcsetActionBean.CONFIRM_CONTROLS_EVENT %>"/>
                </div>
            </c:if>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
