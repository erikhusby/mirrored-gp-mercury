<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.submitString}" sectionTitle="${actionBean.submitString} ${actionBean.editProduct.productName}">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(
                function () {
                    $j("#priceItem").tokenInput(
                        "${ctxpath}/products/product.action?priceItemAutocomplete=&product=${actionBean.editProduct.businessKey}", {
                            searchDelay: 500,
                            minChars: 2,
                            preventDuplicates: true,
                            <c:if test="${actionBean.priceItemCompleteData != null && actionBean.priceItemCompleteData != ''}">
                                prePopulate: ${actionBean.priceItemCompleteData},
                            </c:if>
                            tokenLimit: 1
                        }
                    );

                    $j("#addOns").tokenInput(
                            "${ctxpath}/products/product.action?addOnsAutocomplete=&product=${actionBean.editProduct.businessKey}", {
                                searchDelay: 500,
                                minChars: 2,
                                <c:if test="${actionBean.addOnCompleteData != null && actionBean.addOnCompleteData != ''}">
                                    prePopulate: ${actionBean.addOnCompleteData},
                                </c:if>
                                preventDuplicates: true
                            }
                    );

                    $j("#availabilityDate").datepicker();
                    $j("#discontinuedDate").datepicker();

                    updateBillingRules();
                }
            );

            function updateBillingRules() {
                if ($j('#useAutomatedBilling').attr('checked')) {
                    $j('#billingRules').show();
                } else {
                    $j('#billingRules').hide();
                }
            }
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="createForm" class="form-horizontal">
            <div style="float: left; margin-right: 40px; margin-top: 5px; width: 98%">
                <stripes:hidden name="product"/>
                <div class="control-group">
                    <stripes:label for="productFamily" name="Product Family" class="control-label"/>
                    <div class="controls">
                        <stripes:select name="editProduct.productFamily.productFamilyId" id="productFamily">
                            <stripes:option value="">Select a Product Family</stripes:option>
                            <stripes:options-collection collection="${actionBean.productFamilies}" label="name"
                                                        value="productFamilyId"/>
                        </stripes:select>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="productName" name="Product Name" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="productName" name="editProduct.productName" class="defaultText"
                            title="Enter the name of the new product"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="description" name="Product Description" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="description" name="editProduct.description" class="defaultText"
                            title="Enter the description of the new product"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="partNumber" name="Part Number" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="partNumber" name="editProduct.partNumber" class="defaultText"
                            title="Enter the part number of the new product"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="availabilityDate" name="Availability Date" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="availabilityDate" name="editProduct.availabilityDate" class="defaultText"
                            title="enter date (MM/dd/yyyy)"><fmt:formatDate
                                value="${actionBean.editProduct.availabilityDate}" dateStyle="short"/></stripes:text>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="discontinuedDate" name="Discontinued Date" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="discontinuedDate" name="editProduct.discontinuedDate" class="defaultText"
                            title="enter date (MM/dd/yyyy)"><fmt:formatDate
                                value="${actionBean.editProduct.discontinuedDate}" dateStyle="short"/></stripes:text>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="deliverables" name="Deliverables" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="deliverables" name="editProduct.deliverables" class="defaultText"
                            title="Enter the deliverables for this product"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="inputRequirements" name="Input Requirements" class="control-label"/>
                    <div class="controls">
                        <stripes:textarea id="inputRequirements" name="editProduct.inputRequirements" class="defaultText"
                            title="Enter the input requirements for this product" cols="50" rows="3"
                            value="${actionBean.editProduct.inputRequirements}"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="expectedCycleTimeDays" name="Expected Cycle Time (Days)" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="expectedCycleTimeDays" name="editProduct.expectedCycleTimeDays"
                            class="defaultText" title="Enter the expected cycle time in days"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="guaranteedCycleTimeDays" name="Guaranteed Cycle Time (Days)" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="guaranteedCycleTimeDays" name="editProduct.guaranteedCycleTimeDays"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="samplesPerWeek" name="Samples Per Week" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="samplesPerWeek" name="editProduct.samplesPerWeek"
                            class="defaultText" title="Enter the number of samples"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="minimumOrderSize" name="Minimum Order Size" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="minimumOrderSize" name="editProduct.minimumOrderSize"
                            class="defaultText" title="Enter the minimum order size"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="pdmOrderableOnly" name="PDM Orderable Only" class="control-label"/>
                    <div class="controls">
                        <stripes:checkbox id="pdmOrderableOnly" name="editProduct.pdmOrderableOnly" class="defaultText" style="margin-top: 10px;"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="priceItem" name="Primary Price Item" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="priceItem" name="priceItemList"
                            class="defaultText" title="Type to search for matching price items"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="addOns" name="Add-ons" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="addOns" name="addOnList"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="useAutomatedBilling" name="Billing" class="control-label"/>
                    <div class="controls">
                        <stripes:checkbox id="useAutomatedBilling" name="editProduct.useAutomatedBilling" onchange="updateBillingRules()" style="margin-top: 10px;"/>
                        <stripes:label for="useAutomatedBilling" name="Automated" class="control-label" style="width:auto;"/>
                    </div>

                    <div id="billingRules" style="clear:both;" class="controls">
                        <stripes:label for="requirementsAttribute" name="Bill When" class="control-label" style="width: auto; margin-right:5px;"/>

                        <stripes:text id="requirementsAttribute" name="editProduct.requirement.attribute"
                                      class="defaultText" title="Attribute to compare"/>
                        &#160;

                        <stripes:select style="width:50px;" name="editProduct.requirement.operator">
                            <stripes:options-enumeration enum="org.broadinstitute.gpinformatics.athena.entity.products.BillingRequirement.Operator" label="label"/>
                        </stripes:select>
                        &#160;

                        <stripes:text id="requirementsValue" name="editProduct.requirement.value"
                                      class="defaultText" title="Value to compare"/>
                    </div>
                </div>

                <div class="control-group">
                    <div class="controls">
                        <div class="row-fluid">
                            <div class="span2">
                                <stripes:submit name="save" value="Save" class="btn btn-primary"/>
                            </div>
                            <div class="offset">
                                <c:choose>
                                    <c:when test="${actionBean.creating}">
                                        <stripes:link beanclass="${actionBean.class.name}" event="list">Cancel</stripes:link>
                                    </c:when>
                                    <c:otherwise>
                                        <stripes:link beanclass="${actionBean.class.name}" event="view">
                                            <stripes:param name="product" value="${actionBean.editProduct.businessKey}"/>
                                            Cancel
                                        </stripes:link>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
