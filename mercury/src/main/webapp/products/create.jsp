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
            <stripes:hidden name="submitString"/>
            <div class="form-horizontal">
                <stripes:hidden name="product"/>
                <div class="control-group">
                    <stripes:label for="productFamily" class="control-label">
                        Product Family
                    </stripes:label>
                    <div class="controls">
                        <stripes:select name="editProduct.productFamily.productFamilyId" id="productFamily">
                            <stripes:option value="">Select a Product Family</stripes:option>
                            <stripes:options-collection collection="${actionBean.productFamilies}" label="name"
                                                        value="productFamilyId"/>
                        </stripes:select>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="productName" class="control-label">
                        Product Name
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="productName" name="editProduct.productName" class="defaultText"
                            title="Enter the name of the new product"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="description" class="control-label">
                        Product Description
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="description" name="editProduct.description" class="defaultText"
                            title="Enter the description of the new product"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="partNumber" class="control-label">
                        Part Number
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="partNumber" name="editProduct.partNumber" class="defaultText"
                            title="Enter the part number of the new product"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="availabilityDate" class="control-label">
                        Availability Date
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="availabilityDate" name="editProduct.availabilityDate" class="defaultText"
                            title="Enter date (MM/dd/yyyy)" value="${actionBean.editProduct.availabilityDate}" formatPattern="MM/dd/yyyy" />

                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="discontinuedDate" class="control-label">
                        Discontinued Date
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="discontinuedDate" name="editProduct.discontinuedDate" class="defaultText" title="Enter date (MM/dd/yyyy)"
                                      value="${actionBean.editProduct.discontinuedDate}" formatPattern="MM/dd/yyyy" />

                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="deliverables" class="control-label">
                        Deliverables
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="deliverables" name="editProduct.deliverables" class="defaultText"
                            title="Enter the deliverables for this product"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="inputRequirements" class="control-label">
                        Input Requirements
                    </stripes:label>
                    <div class="controls">
                        <stripes:textarea id="inputRequirements" name="editProduct.inputRequirements" class="defaultText"
                            title="Enter the input requirements for this product" cols="50" rows="3"
                            value="${actionBean.editProduct.inputRequirements}"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="expectedCycleTimeDays" class="control-label">
                        Expected Cycle Time (Days)
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="expectedCycleTimeDays" name="editProduct.expectedCycleTimeDays"
                            class="defaultText" title="Enter the expected cycle time in days"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="guaranteedCycleTimeDays" class="control-label">
                        Guaranteed Cycle Time (Days)
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="guaranteedCycleTimeDays" name="editProduct.guaranteedCycleTimeDays"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="samplesPerWeek" class="control-label">
                        Samples Per Week
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="samplesPerWeek" name="editProduct.samplesPerWeek"
                            class="defaultText" title="Enter the number of samples"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="minimumOrderSize" class="control-label">
                        Minimum Order Size
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="minimumOrderSize" name="editProduct.minimumOrderSize"
                            class="defaultText" title="Enter the minimum order size"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="pdmOrderableOnly" class="control-label">
                        PDM Orderable Only
                    </stripes:label>
                    <div class="controls">
                        <stripes:checkbox id="pdmOrderableOnly" name="editProduct.pdmOrderableOnly" class="defaultText" style="margin-top: 10px;"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="priceItem" class="control-label">
                        Primary Price Item
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="priceItem" name="priceItemList"
                            class="defaultText" title="Type to search for matching price items"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="addOns" class="control-label">
                        Add-ons
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="addOns" name="addOnList"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="useAutomatedBilling" class="control-label">
                        Billing
                    </stripes:label>
                    <div class="controls">
                        <stripes:checkbox id="useAutomatedBilling" name="editProduct.useAutomatedBilling" onchange="updateBillingRules()" style="margin-top: 10px;"/>
                        <stripes:label for="useAutomatedBilling" class="control-label" style="width:auto;">
                            Automated
                        </stripes:label>
                    </div>

                    <div id="billingRules" style="clear:both;" class="controls">
                        <stripes:label for="requirementsAttribute" class="control-label" style="width: auto; margin-right:5px;">
                            Bill When
                        </stripes:label>

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
                            <div class="span1">
                                <stripes:submit name="save" value="Save" class="btn btn-primary"/>
                            </div>
                            <div class="span1">
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
