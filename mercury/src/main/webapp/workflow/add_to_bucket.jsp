<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.AddReworkActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Add Sample(s) To Bucket" sectionTitle="Add To Bucket">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {

                $j("#rework-reason-label").hide();
                $j("#rework-reason-value").hide();
                $j("#rework-comment-label").hide();
                $j("#commentText").hide();
                $j("#rework-reason-user-value").hide();

                $j('#vesselBarcode').change(function () {
                    var barcode = $j("#vesselBarcode").val();
                    if (barcode) {
                        $j('#vesselInfo').html("<img src=\"${ctxpath}/images/spinner.gif\"/>");
                        $j.ajax({
                            type: "POST",
                            url: "${ctxpath}/workflow/AddToBucket.action?vesselInfo=",
                            data: { vesselLabel: barcode },
                            dataType: 'html',
                            success: updateDetails
                        });
                    }
                });

                // Invoke ajax request in the case that this is a redisplay of the page because of an error
                $j('#vesselBarcode').change();


                $j("#rework-reason-value").change(function () {
                    if (this.value == 'Other...') {
                        $j("#rework-reason-user-value").show();
                    } else {
                        $j("#rework-reason-user-value").hide();
                    }
                });
            });

            function toggleReworkComponents() {
                if ($j('.rework-checkbox:checked').length) {
                    $j("#rework-reason-label").show();
                    $j("#rework-reason-value").show();
                    $j("#rework-comment-label").show();
                    $j("#commentText").show();
                    if ($j("#rework-reason-value").val() == -1) {
                        $j("#rework-reason-user-value").show();
                    } else {
                        $j("#rework-reason-user-value").hide();
                    }
                } else {
                    $j("#rework-reason-label").hide();
                    $j("#rework-reason-value").hide();
                    $j("#rework-comment-label").hide();
                    $j("#commentText").hide();
                }
            }

            function updateDetails(data) {
                $j("#vesselInfo").html(data);
            }
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" class="form-horizontal" id="addToBucketEntryForm">
            <div class="control-group">
                <stripes:label for="vesselLabel" class="control-label">
                    Barcodes or Sample IDs
                </stripes:label>
                <div id="barcodeDiv" class="controls">
                    <stripes:textarea id="vesselBarcode" name="vesselLabel" rows="3" cols="16"/>
                </div>
            </div>
            <!-- FIXME SGM: Move the definition of the vessel_info JSP to here instead of making it an Ajax insert -->
            <div id="vesselInfo"></div>
            <div class="control-group">
                <stripes:label for="reworkReason" class="control-label" id="rework-reason-label">
                    Reason for Rework
                </stripes:label>
                <div class="controls">
                    <stripes:select name="reworkReason" id="rework-reason-value">
                        <stripes:options-collection collection="${actionBean.getAllReworkReasons()}"
                                                    value="reason" label="reason"/>
                        <stripes:option label="Other..." value="Other..."/>
                    </stripes:select>
                    <stripes:text name="userReworkReason" id="rework-reason-user-value"/>
                </div>
            </div>
            <div class="control-group">
                <stripes:label for="commentText" class="control-label" id="rework-comment-label">
                    Comments
                </stripes:label>
                <div class="controls">
                    <stripes:textarea name="commentText" id="commentText"/>
                </div>
            </div>
            <div class="control-group">
                <div class="controls">
                    <stripes:submit name="addSample" value="Add Sample To Bucket" class="btn btn-primary"/>
                </div>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
