<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoReceivingActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Mayo Sample Receipt" sectionTitle="Mayo Sample Receipt Rack Scan">
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="saveForm" class="form-horizontal">
            <!-- The hidden variables to pass back to the action bean. -->
            <stripes:hidden name="rackBarcode" value="${actionBean.rackBarcode}"/>
            <stripes:hidden name="vesselGeometry" value="${actionBean.vesselGeometry}"/>
            <c:forEach items="${actionBean.rackScanEntries}" var="mapEntry" varStatus="item">
                <stripes:hidden name="rackScanEntries[${item.index}]" value="${mapEntry}"/>
            </c:forEach>

            <!-- Displays the scan result as a table of vessel positions and corresponding samples. -->
            <p>The tubes in rack ${actionBean.rackBarcode}</p>

            <table id="samplesTable" border="2">
                <thead>
                <tr>
                    <th width="20"></th>
                    <c:forEach items="${actionBean.rackColumns}" var="rackColumn">
                        <th width="180">${rackColumn}</th>
                    </c:forEach>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.rackRows}" var="rackRow">
                    <tr>
                        <td><b>${rackRow}</b></td>
                        <c:forEach items="${actionBean.rackColumns}" var="rackColumn">
                            <td>${actionBean.getSampleAt(rackRow, rackColumn)}</td>
                        </c:forEach>
                    </tr>
                </c:forEach>
                </tbody>
            </table>

            <!-- Button to lookup the manifest file for the rack and display contents. -->
            <c:if test="${actionBean.getManifestCellGrid().isEmpty()}">
                <div style="padding-top: 20px;">
                    <stripes:submit id="viewManifestBtn" name="viewManifestBtn" value="View The Manifest File"
                                    class="btn btn-primary"
                                    title="Click for a display of the manifest file that will be used for the rack."/>
                </div>
            </c:if>

            <!-- Manifest file contents. -->
            <c:if test="${!actionBean.getManifestCellGrid().isEmpty()}">
                <p>Filename: ${actionBean.filename}</p>
                <div style="padding-top: 10px;">
                    <table id="manifestCellGrid" border="2">
                        <tbody>
                        <c:forEach items="${actionBean.getManifestCellGrid()}" var="manifestRow">
                            <tr>
                                <c:forEach items="${manifestRow}" var="manifestColumn">
                                    <td align="center">${manifestColumn}</td>
                                </c:forEach>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:if>
            <!-- User entered values that go into the RCT ticket. -->
            <table style="padding-top: 20px;" cellpadding="5">
                <tr>
                    <td>Shipment Condition</td>
                    <td><stripes:textarea style="min-width: 50em" rows="2" id="shipmentCondition" name="shipmentCondition"/></td>
                </tr>
                <tr>
                    <td>Quarantine due to condition</td>
                    <td><stripes:checkbox name="forcedQuarantine" id="forcedQuarantine"
                    title="Mark the rack Quarantined because of the shipment condition."/></td>
                </tr>
                <tr>
                    <td>Shipping Acknowledgement</td>
                    <td><stripes:text style="min-width: 40em" id="shippingAcknowledgement" name="shippingAcknowledgement"/></td>
                </tr>
                <tr>
                    <td>Delivery Method</td>
                    <td><stripes:select id="deliveryMethod" class="multiEditSelect" name="deliveryMethod">
                        <stripes:option value="None" label="None"/>
                        <stripes:option value="FedEx" label="FedEx"/>
                        <stripes:option value="Local Courier" label="Local Courier"/>
                    </stripes:select>
                    </td>
                </tr>
                <tr>
                    <td>Receipt Type</td>
                    <td><stripes:select id="receiptType" class="multiEditSelect" name="receiptType">
                        <stripes:option value="None" label="None"/>
                        <stripes:option value="Clinical Genomes" label="Clinical Genomes"/>
                        <stripes:option value="Clinical Exomes" label="Clinical Exomes"/>
                        <!-- Multiple values must be comma-space separated. -->
                        <stripes:option value="Clinical Exomes, Clinical Genomes" label="Clinical Exomes, Clinical Genomes"/>
                    </stripes:select>
                    </td>
                </tr>
            </table>

            <div style="padding-top: 20px;">
                <span>
                <stripes:submit id="saveBtn" name="saveBtn" value="Receive and Accession" class="btn btn-primary"
                                title="Receives the rack and tubes, and attempts to accession the samples."/>
                    <span style="margin-left: 20px;">
                <stripes:submit id="cancelBtn" name="cancelBtn" value="Cancel" class="btn btn-primary"/>
                    </span>
                </span>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>