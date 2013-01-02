<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.reagent.ReagentDesignActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.submitString}" sectionTitle="${actionBean.submitString} ${actionBean.reagentDesign.designName}">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(
                function () {
                    $j("#addOns").tokenInput(
                        "${ctxpath}/products/product.action?addOnsAutocomplete=&productKey=${actionBean.reagentDesign.businessKey}", {
                            searchDelay: 500,
                            minChars: 2,
                            preventDuplicates: true
                        }
                    );

                    $j("#availabilityDate").datepicker();
                    $j("#discontinuedDate").datepicker();
                }
            );
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="createForm" class="form-horizontal">

            <div class="control-group">
                <stripes:label for="designName" name="Name" class="control-label"/>
                <div class="controls">
                    <stripes:text id="designName" name="reagentDesign.designName" class="defaultText"
                        title="Enter the name of the reagent design"/>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="targetSetName" name="Product Description" class="control-label"/>
                <div class="controls">
                    <stripes:text id="targetSetName" name="reagentDesign.targetSetName" class="defaultText"
                        title="Enter the name of the target set."/>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="manufacturersName" name="Part Number" class="control-label"/>
                <div class="controls">
                    <stripes:text id="manufacturersName" name="reagentDesign.manufacturersName" class="defaultText"
                        title="Enter the name of the manufacturer."/>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="reagentType" name="Product Family" class="control-label"/>
                <div class="controls">
                    <stripes:select name="reagentDesign.reagentType" id="reagentType">
                        <stripes:option value="">Select a Reagent Type</stripes:option>
                        <stripes:options-enumeration enum="org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign.ReagentType"/>
                    </stripes:select>
                </div>
            </div>

            <div class="control-group">
                <div class="controls">
                    <div class="row-fluid">
                        <div class="span2">
                            <stripes:submit name="save" value="Save"/>
                        </div>
                        <div class="offset">
                            <c:choose>
                                <c:when test="${actionBean.creating}">
                                    <stripes:link beanclass="${actionBean.class.name}" event="list">Cancel</stripes:link>
                                </c:when>
                                <c:otherwise>
                                    <stripes:link beanclass="${actionBean.class.name}" event="view">
                                        <stripes:param name="businessKey" value="${actionBean.reagentDesign.businessKey}"/>
                                        Cancel
                                    </stripes:link>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
