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
                vertical-align: middle;
                padding-top: 20px;
                padding-right: 20px;
            }
            div.inputGroup > div.control-group > div.controls {
                display: table-cell;
                vertical-align: middle;
                padding-top: 20px;
            }
            text, textarea, .control-label, .controls, select, option, option-collection {
                font-size: 12px;
                font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
            }
        </style>

        <%@ include file="/vessel/rack_scanner_list_with_sim_part1.jsp" %>

        <stripes:form beanclass="${actionBean.class.name}" id="rackScanForm" class="form-horizontal">
            <div class="inputGroup">
                <!-- Captures the rack barcode. -->
                <div class="control-group">
                    <div class="control-label">Rack Barcode</div>
                    <div class="controls">
                        <stripes:text id="rackBarcode" name="rackBarcode"/>
                    </div>
                </div>

                <!-- Selectors for the rack scan lab & scanner, and the Scan button. -->
                <stripes:layout-render name="/vessel/rack_scanner_list_with_sim_part2.jsp" bean="${actionBean}"/>

                <div class="control-group">
                    <div class="control-label"/>
                    <div class="controls">
                        <stripes:submit value="Rack Scan" id="scanBtn" name="scanBtn" class="btn btn-primary"/>
                    </div>
                </div>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>