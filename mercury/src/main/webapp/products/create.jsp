<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Athena View Product ${actionBean.product.productName}">

    <stripes:layout-component name="extraHead">
    </stripes:layout-component>
    <stripes:layout-component name="content">

        <dl class="control-group">
            <dt class="control-lable">Product Family</dt>
            <dd class="controls">${actionBean.product.productFamily.name}</dd>
        </dl>
            <p:outputLabel for="productFamily" value="Product Family" styleClass="control-label"/>
            <div class="controls">
                <p:selectOneMenu id="productFamily" value="#{productForm.product.productFamily}" converter="#{productFamilyConverter}" required="true">
                    <f:selectItem itemLabel="Select a Product Family" itemValue="" />
                    <f:selectItems value="#{productForm.productFamilies}" var="fam" itemLabel="#{fam.name}"  itemValue="#{fam}" />
                </p:selectOneMenu>
            </div>
        </div>

        <div class="control-group">
            <p:outputLabel for="productName" value="Product Name" styleClass="control-label" />
            <div class="controls">
                <p:inputText id="productName" value="#{productForm.product.productName}" size="50" required="true"/>
                <p:watermark for="productName" value="Enter the name of the new product"/>
                <p:message for="productName"/>
            </div>
        </div>


        <div class="control-group">
            <p:outputLabel for="description" value="Product Description" styleClass="control-label"/>
            <div class="controls">
                <p:inputTextarea id="description" value="#{productForm.product.description}" cols="50" rows="3" required="true"/>
                <p:watermark for="description" value="Enter the description of the product"/>
                <p:message for="description"/>
            </div>
        </div>

        <div class="control-group">
            <p:outputLabel for="partNumber" value="Part Number" styleClass="control-label"/>
            <div class="controls">
                <p:inputText id="partNumber" value="#{productForm.product.partNumber}" size="50" required="true"/>
                <p:watermark for="partNumber" value="Enter the part number of this product"/>
                <p:message for="partNumber"/>
            </div>
        </div>


        <div class="control-group">
            <p:outputLabel for="availabilityDate" value="Availability Date" styleClass="control-label"/>
            <div class="controls">
                <p:calendar value="#{productForm.product.availabilityDate}" id="availabilityDate" size="50" required="true"/>
                <p:watermark for="availabilityDate" value="Click here to select a date"/>
                <p:message for="availabilityDate"/>
            </div>
        </div>

        <div class="control-group">
            <p:outputLabel for="discontinuedDate" value="Discontinued Date" styleClass="control-label"/>
            <div class="controls">
                <p:calendar value="#{productForm.product.discontinuedDate}" id="discontinuedDate" size="50"/>
                <p:watermark for="discontinuedDate" value="Click here to select a date"/>
                <p:message for="discontinuedDate"/>
            </div>
        </div>

        <div class="control-group">
            <p:outputLabel for="deliverables" value="Deliverables" styleClass="control-label"/>
            <div class="controls">
                <p:inputTextarea id="deliverables" value="#{productForm.product.deliverables}" cols="50" rows="3"/>
                <p:watermark for="deliverables" value="Enter the deliverables for this product"/>
                <p:message for="deliverables"/>
            </div>
        </div>

        <div class="control-group">
            <p:outputLabel for="inputRequirements" value="Input Requirements" styleClass="control-label"/>
            <div class="controls">
                <p:inputTextarea id="inputRequirements" value="#{productForm.product.inputRequirements}" cols="50" rows="3"/>
                <p:watermark for="inputRequirements" value="Enter the input requirements for this product"/>
                <p:message for="inputRequirements"/>
            </div>
        </div>

        <div class="control-group">
            <p:outputLabel for="expectedCycleTimeDays" value="Expected Cycle Time (Days)" styleClass="control-label"/>
            <div class="controls">
                <p:inputText id="expectedCycleTimeDays" value="#{productForm.expectedCycleTimeDays}" size="50" required="false"/>
                <p:watermark for="expectedCycleTimeDays" value="Enter the expected cycle time in days"/>
                <p:message for="expectedCycleTimeDays"/>
            </div>
        </div>

        <div class="control-group">
            <p:outputLabel for="guaranteedCycleTimeDays" value="Guaranteed Cycle Time (Days)" styleClass="control-label"/>
            <div class="controls">
                <p:inputText id="guaranteedCycleTimeDays" value="#{productForm.guaranteedCycleTimeDays}" size="50" required="false"/>
                <p:watermark for="guaranteedCycleTimeDays" value="Enter the guaranteed cycle time in days"/>
                <p:message for="guaranteedCycleTimeDays"/>
            </div>
        </div>

        <div class="control-group">
            <p:outputLabel for="samplesPerWeek" value="Samples Per Week" styleClass="control-label"/>
            <div class="controls">
                <p:inputText id="samplesPerWeek" value="#{productForm.product.samplesPerWeek}" size="50" required="false"/>
                <p:watermark for="samplesPerWeek" value="Enter the number of samples"/>
                <p:message for="samplesPerWeek"/>
            </div>
        </div>

        <div class="control-group">
            <p:outputLabel for="minimumOrderSize" value="Minimum Order Size" styleClass="control-label"/>
            <div class="controls">
                <p:inputText id="minimumOrderSize" value="#{productForm.product.minimumOrderSize}" size="50" required="false"/>
                <p:watermark for="minimumOrderSize" value="Enter the minimum order size"/>
                <p:message for="minimumOrderSize"/>
            </div>
        </div>

        <div class="control-group">
            <p:outputLabel for="pdmOrderableOnly" value="PDM Orderable Only" styleClass="control-label"/>
            <div class="controls">
                <p:watermark for="pdmOrderableOnly" value="Is this Product orderable only by Product Managers (PDMs) ?"/>
                <p:selectBooleanCheckbox id="pdmOrderableOnly" value="#{productForm.product.pdmOrderableOnly}" />
                <p:message for="pdmOrderableOnly"/>
            </div>
        </div>

        <div class="control-group">
            <p:outputLabel for="defaultPriceItem" value="Primary Price Item *" styleClass="control-label"/>
            <div class="controls">
                <p:autoComplete id="defaultPriceItem" value="#{productForm.defaultPriceItem}"
                                completeMethod="#{productForm.searchSelectedPriceItems}" var="defaultPriceItem"
                                itemLabel='#{productForm.labelFor(defaultPriceItem)}' scrollHeight="250"
                                itemValue="#{defaultPriceItem}" converter="#{priceItemConverter}"
                                forceSelection="true" size="60">
                    <p:column style="width:100%">
                        <div class="ac-dropdown-text">#{defaultPriceItem.name}</div>
                        <div class="ac-dropdown-subtext">#{defaultPriceItem.platformName} | #{defaultPriceItem.categoryName}</div>
                    </p:column>
                </p:autoComplete>
                <p:watermark for="defaultPriceItem" value="Type text to search "/>
                <p:message for="defaultPriceItem" />
            </div>
        </div>

        <div class="control-group">
            <p:outputLabel for="priceItem" value="Optional Price Items" styleClass="control-label"/>
            <div class="controls">
                <p:autoComplete id="priceItem" value="#{productForm.priceItems}"
                                completeMethod="#{productForm.searchPriceItems}" var="priceItem"
                                itemLabel="#{productForm.labelFor(priceItem)}"
                                itemValue="#{priceItem}" converter="#{priceItemConverter}" multiple="true"
                                scrollHeight="250">

                    <p:column style="width:100%">
                        <div>#{priceItem.platformName} | #{priceItem.categoryName} | #{priceItem.name}</div>
                    </p:column>
                    <p:ajax event="itemSelect" listener="#{productForm.removeDuplicates(productForm.priceItems, 'priceItem')}" update="priceItem optionalPriceItemsMessage :productParam"/>
                </p:autoComplete>
                <p:watermark for="priceItem" value="Type text to search for Price Items"/>
                <p:message id="optionalPriceItemsMessage" for="priceItem" />
            </div>
        </div>

        <div class="control-group">
            <p:outputLabel for="addOns" value="Add-ons" styleClass="control-label"/>
            <div class="controls">
                <p:autoComplete id="addOns" value="#{productForm.addOns}" completeMethod="#{productForm.searchProductsForAddonsInProductEdit}"
                                var="product" itemLabel="#{productForm.addOnLabel(product)}" itemValue="#{product}"
                                converter="#{productConverter}" multiple="true" scrollHeight="250">
                    <p:column style="width:100%">
                        <div class="ac-dropdown-text">#{product.productName} - #{product.partNumber}</div>
                        <div class="ac-dropdown-subtext">Available(now or in the future):#{product.availableNowOrLater ? "Yes" : "No"}</div>
                        <div class="ac-dropdown-subtext">#{product.productFamily.name}</div>
                    </p:column>
                    <p:ajax event="itemSelect" listener="#{productForm.removeDuplicates(productForm.addOns, 'addOns')}" update="addOns addOnsMessage :productParam"/>
                </p:autoComplete>
                <p:watermark for="addOns" value="Type text to search to search for Add-ons"/>
                <p:message id="addOnsMessage" for="addOns"/>
            </div>
        </div>


        <div class="control-group">
            <div class="controls">
                <div class="row-fluid">
                    <div class="span1">
                        <b:commandButton value="Save" action="#{productForm.save}" ajax="false" styleClass="ui-priority-primary"/>
                    </div>
                    <div class="offset2">
                        <h:link outcome="list" value="Cancel" rendered="#{productForm.creating}"/>
                        <h:link outcome="view" value="Cancel" rendered="#{!productForm.creating}">
                            <f:param name="product" value="#{productForm.product.businessKey}"/>
                        </h:link>
                    </div>
                </div>
            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
