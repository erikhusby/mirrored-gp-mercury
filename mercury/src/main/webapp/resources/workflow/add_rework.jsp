<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.AddReworkActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Add Rework" sectionTitle="Add Rework">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $(document).ready(function () {
                $j('#vesselBarcode').change(function () {
                    var barcode = $j("#vesselBarcode").val();
                    if (barcode) {
                        $j('#vesselInfo').html("<img src=\"${ctxpath}/images/spinner.gif\"/>");
                        $j.ajax({
                            url: "${ctxpath}/workflow/AddRework.action?vesselInfo=&vesselLabel=" + barcode,
                            dataType: 'html',
                            success: updateDetails
                        });
                    }
                })
            });
            function updateDetails(data) {
                $j("#vesselInfo").html(data);
            }

        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" class="form-horizontal" id="reworkEntryForm">
            <div class="control-group">
                <stripes:label for="vesselLabel" class="control-label">
                    Barcode:
                </stripes:label>
                <div id="barcodeDiv" class="controls">
                    <stripes:text id="vesselBarcode" name="vesselLabel"/>
                </div>
            </div>
            <div id="vesselInfo"></div>
            <div class="control-group">
                <stripes:label for="reworkReason" class="control-label">
                    Reason for Rework:
                </stripes:label>
                <div class="controls">
                    <stripes:select name="reworkReason">
                        <stripes:options-enumeration
                                enum="org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry.ReworkReason"
                                label="value"/>
                    </stripes:select>
                </div>
            </div>
            <div class="control-group">
                <stripes:label for="commentText" class="control-label">
                    Comments:
                </stripes:label>
                <div class="controls">
                    <stripes:textarea name="commentText" id="commentText"/>
                </div>
            </div>
            <%--<stripes:hidden name="bucketName"  />--%>
            <div class="control-group">
                <div class="controls">
                    <stripes:submit name="reworkSample" value="Rework Sample"/>
                </div>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
