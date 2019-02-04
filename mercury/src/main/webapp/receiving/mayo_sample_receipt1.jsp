<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoReceivingActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Mayo Sample Receipt" sectionTitle="Mayo Sample Receipt">
    <stripes:layout-component name="content">

        <%@ include file="/vessel/rack_scanner_list_with_sim_part1.jsp" %>

        <stripes:form beanclass="${actionBean.class.name}" id="rackScanForm" class="form-horizontal">

            <!-- The hidden variables to pass back to the action bean. -->
            <c:if test="${actionBean.vesselGeometry != null}">
                <stripes:hidden name="vesselGeometry" value="${actionBean.vesselGeometry}"/>
            </c:if>

            <!-- Input for package barcode, and next to it a button to show the manifest for that package. -->
            <div class="control-group">
                <stripes:label for="packageBarcode" name="Package Barcode" class="control-label"/>
                <div class="controls">
                    <span style="width: 25%; border-right-width: 100px;">
                        <input type="text" id="packageBarcode" name="packageBarcode" value="${actionBean.packageBarcode}"
                               autocomplete="off" class="clearable barcode unique">
                    </span>
                    <span>
                        <stripes:submit value="Show The Manifest" id="showManifestBtn" name="showManifestBtn"
                                        class="btn btn-primary"
                                        title="Click to see a display of the manifest document corresponding to the package barcode."/>
                    </span>
                </div>
            </div>
            <!-- Input for rack/box barcode. -->
            <div class="control-group">
                <stripes:label for="rackBarcode" name="Tube Rack or Box Barcode" class="control-label"/>
                <div class="controls">
                    <input type="text" id="rackBarcode" name="rackBarcode" value="${actionBean.rackBarcode}"
                           autocomplete="off" class="clearable barcode unique">
                </div>
            </div>

            <!-- Selectors for the rack scan lab & scanner, and the Scan button. -->
            <stripes:layout-render name="/vessel/rack_scanner_list_with_sim_part2.jsp" bean="${actionBean}"/>
            <div class="control-group">
                <div class="controls">
                    <stripes:submit value="Rack Scan" id="scanBtn" name="scanBtn" class="btn btn-primary"/>
                </div>
            </div>

        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>