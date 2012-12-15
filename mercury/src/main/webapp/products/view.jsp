<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>

<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:h="http://java.sun.com/jsf/html"
      xmlns:f="http://java.sun.com/jsf/core"
      xmlns:ui="http://java.sun.com/jsf/facelets"
      xmlns:p="http://primefaces.org/ui"
      xmlns:security="http://mercury.broadinstitute.org/tags">
<body>

<h:link outcome="/security/login" value="Athena"/>
&gt;
<h:link outcome="list" value="Products"/>
&gt;
#{productView.product.partNumber}

<ui:composition template="/layout.xhtml">
    <ui:define name="title">Details for #{productView.product.partNumber}</ui:define>
    <ui:define name="metadata">
        <f:metadata>
            <f:viewParam name="product" value="#{productView.product}" converter="#{productConverter}" valueChangeListener="#{productConverter.updateModel}"/>
            <f:event type="preRenderView" listener="#{productView.onPreRenderView}"/>
        </f:metadata>
    </ui:define>
    <ui:define name="body">


        <h:form id="productForm" styleClass="form-horizontal form-readonly" rendered="#{productView.shouldRenderForm()}">

            <security:authorizeBlock roles="#{userBean.developerRole}, #{userBean.productManagerRole}">
                <div class="row-fluid" style="margin-bottom: 30px;">
                    <p:button outcome="create" value="Edit">
                        <f:param name="product" value="#{productView.product.partNumber}" />
                    </p:button>
                </div>
            </security:authorizeBlock>


            <div class="control-group">
                <label class="control-label label-form">Part Number</label>
                <div class="controls">
                    <div class="form-value">#{productView.product.partNumber} (<a href="Product Descriptions.pdf">Product Descriptions PDF</a>)</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Name</label>
                <div class="controls">
                    <div class="form-value">#{productView.product.productName}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Product Family</label>
                <div class="controls">
                    <div class="form-value">#{productView.product.productFamily.name}</div>
                </div>
            </div>


            <div class="control-group">
                <label class="control-label label-form">Availability Date</label>
                <div class="controls">
                    <div class="form-value">
                        <h:outputText value="#{productView.product.availabilityDate}">
                            <f:convertDateTime type="date" dateStyle="medium"/>
                        </h:outputText>
                    </div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Discontinued Date</label>
                <div class="controls">
                    <div class="form-value">
                        <h:outputText value="#{productView.product.discontinuedDate}">
                            <f:convertDateTime type="date" dateStyle="medium"/>
                        </h:outputText>
                    </div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Description</label>
                <div class="controls">
                    <div class="form-value">#{productView.product.description}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Deliverables</label>
                <div class="controls">
                    <div class="form-value">#{productView.product.deliverables}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Input Requirements</label>
                <div class="controls">
                    <div class="form-value">#{productView.product.inputRequirements}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Expected Cycle Time (Days)</label>
                <div class="controls">
                    <div class="form-value">#{productView.expectedCycleTimeDays}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Guaranteed Cycle Time (Days)</label>
                <div class="controls">
                    <div class="form-value">#{productView.guaranteedCycleTimeDays}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Samples Per Week</label>
                <div class="controls">
                    <div class="form-value">#{productView.product.samplesPerWeek}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Minimum Order Size</label>
                <div class="controls">
                    <div class="form-value">#{productView.product.minimumOrderSize}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Primary Price Item</label>
                <div class="controls">
                    <div class="form-value">#{productView.product.primaryPriceItem.platform}: #{productView.product.primaryPriceItem.category}: #{productView.product.primaryPriceItem.name}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">PDM Orderable Only?</label>
                <div class="controls">
                    <div class="form-value">#{productView.product.pdmOrderableOnly ? "Yes" : "No"}</div>
                </div>
            </div>


            <p:dataTable value="#{productView.addOnData.values}" var="addOn" >
                <f:facet name="header">
                    <h:outputText value="Add-ons" />
                </f:facet>

                <p:column sortBy="#{addOn.partNumber}">
                    <f:facet name="header">
                        <h:outputText value="Part Number"/>
                    </f:facet>
                    <h:link outcome="view" value="#{addOn.partNumber}">
                        <f:param name="product" value="#{addOn.businessKey}"/>
                    </h:link>
                </p:column>
                <p:column sortBy="#{addOn.productName}">
                    <f:facet name="header">
                        <h:outputText value="Name"/>
                    </f:facet>
                    <h:outputText value="#{addOn.productName}"/>
                </p:column>
                <p:column sortBy="#{addOn.productFamily.name}">
                    <f:facet name="header">
                        <h:outputText value="Product Family"/>
                    </f:facet>
                    <h:outputText value="#{addOn.productFamily.name}"/>
                </p:column>

            </p:dataTable>

            <p/>

            <p:dataTable value="#{productView.optionalPriceItemData.values}" var="priceItem" >
                <f:facet name="header">
                    <h:outputText value="Optional Price Items" />
                </f:facet>
                <p:column sortBy="#{priceItem.platform}">
                    <f:facet name="header">
                        <h:outputText value="Platform"/>
                    </f:facet>
                    <h:outputText value="#{priceItem.platform}"/>
                </p:column>
                <p:column sortBy="#{priceItem.category}">
                    <f:facet name="header">
                        <h:outputText value="Category"/>
                    </f:facet>
                    <h:outputText value="#{priceItem.category}"/>
                </p:column>
                <p:column sortBy="#{priceItem.name}">
                    <f:facet name="header">
                        <h:outputText value="Name"/>
                    </f:facet>
                    <h:outputText value="#{priceItem.name}"/>
                </p:column>
            </p:dataTable>

        </h:form>

    </ui:define>
</ui:composition>
</body>
</html>
