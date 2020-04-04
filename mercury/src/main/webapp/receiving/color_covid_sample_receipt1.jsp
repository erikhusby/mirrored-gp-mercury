<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.ColorCovidReceiptActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Color Covid Sample Receipt" sectionTitle="Color Covid Sample Receipt">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                if (${actionBean.clearFields}) {
                    $j("#rackBarcode").val('');
                }
            });
        </script>
    </stripes:layout-component>

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

        <c:if test="${empty actionBean.dtoString}">
            <stripes:form beanclass="${actionBean.class.name}" id="rackScanForm" class="form-horizontal">
                <div class="inputGroup">
                    <!-- Captures the rack barcode. -->
                    <div class="control-group">
                        <div class="control-label">Rack Barcode</div>
                        <div class="controls">
                            <stripes:textarea rows="1" id="rackBarcode" name="rackBarcode"/>
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
        </c:if>

        <c:if test="${not empty actionBean.dtoString}">
            <stripes:form beanclass="${actionBean.class.name}" id="saveForm" class="form-horizontal">
                <!-- The hidden variables to pass back to the action bean. -->
                <input type="hidden" name="rackBarcode" value="${actionBean.rackBarcode}"/>
                <input type="hidden" name="dtoString" value="${actionBean.dtoString}"/>
                <input type="hidden" name="filename" value="${actionBean.filename}"/>
                <input type="hidden" name="manifestContent" value="${actionBean.manifestContent}"/>

                <div style="padding-top: 20px;">
                <span>
                    <c:if test="${!actionBean.messageCollection.hasErrors()}">
                        <stripes:submit id="saveBtn" name="saveBtn" value="Save" class="btn btn-primary"
                                        title="Accessions the rack, tubes, and samples."/>
                    </c:if>
                    <span style="margin-left: 20px;">
                        <stripes:submit id="cancelBtn" name="cancelBtn" value="Cancel" class="btn btn-primary"/>
                    </span>
                </span>
                </div>
            </stripes:form>
        </c:if>
    </stripes:layout-component>
</stripes:layout-render>