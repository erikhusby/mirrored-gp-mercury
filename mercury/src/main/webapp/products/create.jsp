<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.submitString}" sectionTitle="${actionBean.submitString} ${actionBean.editProduct.productName}">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(
                function () {
                    $j("#addOns").tokenInput(
                        "${ctxpath}/products/product.action?addOnsAutocomplete=&productKey=${actionBean.editProduct.businessKey}", {
                            searchDelay: 2000,
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

        <stripes:form action="/products/product.action" id="createForm" class="form-horizontal">
            <div class="control-group">
                <stripes:label for="productFamily" name="Product Family" class="control-label"/>
                <div class="controls">
                    <stripes:select name="editProduct.productFamily.productFamilyId"
                                    value="${actionBean.editProduct.productFamily.productFamilyId}" id="productFamily">
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
                        title="Enter the name of the new product" value="${actionBean.editProduct.productName}"/>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="description" name="Product Description" class="control-label"/>
                <div class="controls">
                    <stripes:text id="description" name="editProduct.description" class="defaultText"
                        title="Enter the description of the new product" value="${actionBean.editProduct.description}"/>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="partNumber" name="Part Number" class="control-label"/>
                <div class="controls">
                    <stripes:text id="partNumber" name="editProduct.partNumber" class="defaultText"
                        title="Enter the part number of the new product" value="${actionBean.editProduct.partNumber}"/>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="availabilityDate" name="Availability Date" class="control-label"/>
                <div class="controls">
                    <stripes:text id="availabilityDate" name="editProduct.availabilityDate" class="defaultText"
                        title="enter date (MM/dd/yyyy)"><fmt:formatDate
                            value="${actionBean.editProduct.availabilityDate}" pattern="MM/dd/yyyy"/></stripes:text>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="discontinuedDate" name="Discontinued Date" class="control-label"/>
                <div class="controls">
                    <stripes:text id="discontinuedDate" name="editProduct.discontinuedDate" class="defaultText"
                        title="enter date (MM/dd/yyyy)"><fmt:formatDate
                            value="${actionBean.editProduct.discontinuedDate}" pattern="MM/dd/yyyy"/></stripes:text>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="deliverables" name="Deliverables" class="control-label"/>
                <div class="controls">
                    <stripes:text id="deliverables" name="editProduct.deliverables" class="defaultText"
                        title="Enter the deliverables for this product" value="${actionBean.editProduct.deliverables}"/>
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
                        class="defaultText" title="Enter the expected cycle time in days"
                        value="${actionBean.editProduct.expectedCycleTimeDays}"/>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="guaranteedCycleTimeDays" name="Guaranteed Cycle Time (Days)" class="control-label"/>
                <div class="controls">
                    <stripes:text id="guaranteedCycleTimeDays" name="editProduct.guaranteedCycleTimeDays"
                        class="defaultText" title="Enter the expected cycle time in days"
                        value="${actionBean.editProduct.guaranteedCycleTimeDays}"/>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="samplesPerWeek" name="Samples Per Week" class="control-label"/>
                <div class="controls">
                    <stripes:text id="samplesPerWeek" name="editProduct.samplesPerWeek"
                        class="defaultText" title="Enter the number of samples"
                        value="${actionBean.editProduct.samplesPerWeek}"/>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="minimumOrderSize" name="Minimum Order Size" class="control-label"/>
                <div class="controls">
                    <stripes:text id="minimumOrderSize" name="editProduct.minimumOrderSize"
                        class="defaultText" title="Enter the minimum order size"
                        value="${actionBean.editProduct.minimumOrderSize}"/>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="pdmOrderableOnly" name="PDM Orderable Only" class="control-label"/>
                <div class="controls">
                    <stripes:checkbox id="pdmOrderableOnly" name="editProduct.pdmOrderableOnly"
                        class="defaultText" value="${actionBean.editProduct.pdmOrderableOnly}"/>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="defaultPriceItem" name="Minimum Order Size" class="control-label"/>
                <div class="controls">
                    <stripes:text id="defaultPriceItem" name="editProduct.primaryPriceItem"
                        class="defaultText" title="Type to search for matching price items"
                        value="${actionBean.editProduct.primaryPriceItem}"/>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="addOns" name="Add-ons" class="control-label"/>
                <div class="controls">
                    <stripes:text id="addOns" name="editProduct.addOns" class="defaultText"
                        title="Type text to search to search for Add-ons"
                        value="${actionBean.editProduct.addOns}"/>
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
                                    <stripes:link href="${ctxpath}/products/product.action?list=">Cancel</stripes:link>
                                </c:when>
                                <c:otherwise>
                                    <stripes:link href="${ctxpath}/products/product.action?view=">
                                        <stripes:param name="productKey" value="${actionBean.editProduct.businessKey}"/>
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
