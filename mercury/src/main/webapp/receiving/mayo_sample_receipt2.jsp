<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoReceivingActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Mayo Sample Receipt" sectionTitle="Mayo Sample Receipt Rack Scan">
    <stripes:layout-component name="content">

        <p>Package ${actionBean.packageBarcode}</p>
        <p>Rack or Box ${actionBean.rackBarcode}</p>
        <p>Tubes in Rack Scan:</p>
        <!-- Displays the scan result as a table of vessel positions and corresponding samples. -->
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

        <stripes:form beanclass="${actionBean.class.name}" id="saveForm" class="form-horizontal">

            <!-- The hidden variables to pass back to the action bean. -->
            <stripes:hidden name="packageBarcode" value="${actionBean.packageBarcode}"/>
            <stripes:hidden name="rackBarcode" value="${actionBean.rackBarcode}"/>
            <stripes:hidden name="vesselGeometry" value="${actionBean.vesselGeometry}"/>
            <c:forEach items="${actionBean.rackScanEntries}" var="mapEntry" varStatus="item">
                <stripes:hidden name="rackScanEntries[${item.index}]" value="${mapEntry}"/>
            </c:forEach>

            <div style="margin-top: 20px;">
                <!-- The Overwrite checkbox. -->
                <c:if test="${actionBean.previousMayoRack}">
                    <span>
                        <stripes:checkbox id="overwriteFlag" name="overwriteFlag" style="align-vertical: center"/>
                        A Mayo rack having this barcode was already uploaded. Check to overwrite it.
                    </span>
                </c:if>
            </div>
            <div style="margin-top: 10px;">
                <!-- The Save and Cancel buttons. -->
                <stripes:submit id="saveBtn" name="saveBtn" value="Save" class="btn btn-primary"
                                title="Save the rack, tubes, and samples in Mercury."/>
                <span style="margin-left: 20px;">
                    <stripes:submit id="cancelBtn" name="cancelBtn" value="Cancel" class="btn btn-primary"/>
                </span>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>