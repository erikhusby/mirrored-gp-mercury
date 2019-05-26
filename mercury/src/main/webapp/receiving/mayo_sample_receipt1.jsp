<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoSampleReceiptActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Mayo Sample Receipt" sectionTitle="Mayo Sample Receipt">
    <stripes:layout-component name="content">
        <style type="text/css">
            div.inputGroup {
                display: table;
            }
            div.inputGroup > div.control-group {
                display: table-row;
            }
            div.inputGroup > div.control-group > .control-label {
                display: table-cell;
            }
            div.inputGroup > div.control-group > div.controls {
                display: table-cell;
                padding-left: 10px;
                padding-top: 10px;
            }
        </style>

        <%@ include file="/vessel/rack_scanner_list_with_sim_part1.jsp" %>

        <stripes:form beanclass="${actionBean.class.name}" id="rackScanForm" class="form-horizontal">
            <!-- Captures the rack barcode. -->
            <div class="control-group">
                Rack Barcode:
                <span style="padding-left: 10px">
                    <stripes:text id="rackBarcode" name="rackBarcode"/>
                </span>
            </div>

            <!-- Selectors for the rack scan lab & scanner, and the Scan button. -->
            <stripes:layout-render name="/vessel/rack_scanner_list_with_sim_part2.jsp" bean="${actionBean}"/>

            <div class="control-group">
                <stripes:submit value="Rack Scan" id="scanBtn" name="scanBtn" class="btn btn-primary"/>
            </div>

        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>