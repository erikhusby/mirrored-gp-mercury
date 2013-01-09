<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.reagent.ReagentDesignActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.submitString}"
                       sectionTitle="${actionBean.submitString} ${actionBean.reagentDesign.designName}">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j("#reagentDesign").tokenInput(
                        "${ctxpath}/reagent/design.action?reagentListAutocomplete=&businessKey=${actionBean.reagentDesign.businessKey}", {
                            <c:if test="${actionBean.reagentDesignCompleteData != null && actionBean.reagentDesignCompleteData != ''}">
                            prePopulate: ${actionBean.reagentDesignCompleteData},
                            </c:if>
                            searchDelay: 500,
                            minChars: 2, tokenLimit: 1, preventDuplicates: true
                        })
            });
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
                    <stripes:text id="reagentDesign" name="businessKey"
                                  class="defaultText" title="Type to search for matching reagent"/>
                </div>
            </div>

            <div class="control-group">
                <div class="controls">
                    <div class="row-fluid">
                        <div class="span2">
                            <stripes:submit name="barcodeReagent" value="Save"/>
                        </div>
                        <div class="offset">
                            <stripes:link beanclass="${actionBean.class.name}"
                                          event="list">Cancel</stripes:link>
                        </div>
                    </div>
                </div>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
