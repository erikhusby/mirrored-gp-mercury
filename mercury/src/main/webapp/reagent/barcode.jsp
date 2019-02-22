<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.reagent.ReagentDesignActionBean"/>

<stripes:layout-render name="/layout.jsp"
                       pageTitle="Assign Barcode"
                       sectionTitle="Assign Barcode">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j("#reagentDesign").tokenInput(
                        "${ctxpath}/reagent/design.action?reagentListAutocomplete=&reagentDesign=${actionBean.editReagentDesign.businessKey}", {
                            <enhance:out escapeXml="false">
                            prePopulate: ${actionBean.ensureStringResult(actionBean.reagentDesignTokenInput.completeData)},
                            </enhance:out>
                            resultsFormatter: formatInput,
                            tokenLimit: 1,
                            tokenDelimiter: "${actionBean.reagentDesignTokenInput.separator}",
                            preventDuplicates: true,
                            autoSelectFirstResult: true
                        });
            });

            function formatInput(item) {
                var extraCount = (item.extraCount == undefined) ? "" : item.extraCount;
                return "<li>" + item.dropdownItem + extraCount + '</li>';
            }
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="createForm" class="form-horizontal">
            <stripes:hidden name="submitString" value="${actionBean.submitString}"/>

            <div class="control-group">
                <stripes:label for="barcode" name="Enter barcode(s) *" class="control-label"/>
                <div class="controls">
                    <stripes:text id="barcode" name="barcode" size="50"/>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="reagentDesign" name="Available Reagents *" class="control-label"/>
                <div class="controls">
                    <stripes:text id="reagentDesign" name="reagentDesign" size="50"
                                  class="defaultText" title="Type to search for matching reagent"/>
                </div>
            </div>

            <div class="control-group">
                <div class="controls">
                    <div class="row-fluid">
                        <div class="span2">
                            <stripes:submit name="barcodeReagent" class="btn btn-primary" value="Save"/>
                        </div>
                        <div class="offset">
                            <stripes:link beanclass="${actionBean.class.name}" event="list">Cancel</stripes:link>
                        </div>
                    </div>
                </div>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
