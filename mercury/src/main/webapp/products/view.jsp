<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="View Product: #{actionBean.editProduct.partNumber}"
                       sectionTitle="View Product: #{actionBean.editProduct.partNumber}"
                       businessKeyValue="${actionBean.editProduct.businessKey}">

    <stripes:layout-component name="content">

        <div class="form-horizontal span7">
            <div class="view-control-group control-group">
                <label class="control-label label-form">Part Number</label>
                <div class="controls">
                    <div class="form-value">
                        <img src="${ctxpath}/images/pdficon_small.png" alt="">
                        <stripes:link beanclass="${actionBean.class.name}" event="downloadProductDescriptions">
                            ${actionBean.editProduct.partNumber}
                            <stripes:param name="editProduct.partNumber" value="${actionBean.editProduct.partNumber}"/>
                        </stripes:link>
                    </div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Product Name</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.productName}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Product Family</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.productFamily.name}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Availability Date</label>
                <div class="controls">
                    <div class="form-value">
                        <fmt:formatDate value="${actionBean.editProduct.availabilityDate}" pattern="${actionBean.datePattern}"/>
                    </div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Discontinued Date</label>
                <div class="controls">
                    <div class="form-value">
                        <fmt:formatDate value="${actionBean.editProduct.discontinuedDate}" pattern="${actionBean.datePattern}"/>
                    </div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Description</label>
                <div class="controls">
                    <div class="form-value" style="white-space: pre-wrap;">${actionBean.editProduct.description}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Deliverables</label>
                <div class="controls">
                    <div class="form-value" style="white-space: pre-wrap;">${actionBean.editProduct.deliverables}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Input Requirements</label>
                <div class="controls">
                    <div class="form-value" style="white-space: pre-wrap;">${actionBean.editProduct.inputRequirements}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Expected Cycle Time (Days)</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.expectedCycleTimeDays}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Guaranteed Cycle Time (Days)</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.guaranteedCycleTimeDays}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Samples Per Week</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.samplesPerWeek}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Minimum Order Size</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.minimumOrderSize}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Primary Price Items</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.primaryPriceItem.displayName}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">PDM Orderable Only</label>
                <div class="controls">
                    <div class="form-value">
                        ${actionBean.editProduct.pdmOrderableOnly ? "Yes" : "No"}
                    </div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Risk Criteria</label>
                <div class="controls">
                    <div class="form-value">
                        <c:choose>
                            <c:when test="${empty actionBean.editProduct.riskCriteria}">
                                No risk criteria
                            </c:when>
                            <c:otherwise>
                                A sample is on risk if:<br/>
                                <c:forEach items="${actionBean.editProduct.riskCriteria}" var="criterion">
                                    ${criterion.calculationString}<br/>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>

            <security:authorizeBlock roles="<%= roles(Developer) %>">
                <div class="view-control-group control-group">
                    <label class="control-label label-form">Billing</label>
                    <div class="controls">
                        <div class="form-value">
                            ${actionBean.editProduct.useAutomatedBilling ? "Automatic When:" : "Manual with tracker"}
                        </div>
                    </div>
                </div>

                <c:if test="${actionBean.editProduct.useAutomatedBilling}">
                    <div class="control-group">
                        <label class="control-label label-form">&nbsp;</label>
                        <div class="controls">
                            <div class="form-value">${actionBean.editProduct.requirement.attribute}&#160;
                                    ${actionBean.editProduct.requirement.operator.label}&#160;
                                    ${actionBean.editProduct.requirement.value}
                            </div>
                        </div>
                    </div>
                </c:if>
            </security:authorizeBlock>

            <security:authorizeBlock roles="<%= roles(Developer) %>">
                <div class="view-control-group control-group">
                    <label class="control-label label-form">Workflow</label>
                    <div class="controls">
                        <div class="form-value">
                        ${actionBean.editProduct.workflow.workflowName}
                        </div>
                    </div>
                </div>
            </security:authorizeBlock>

        </div>

        <div class="form-horizontal span5">
            <fieldset>
                <legend><h4>Pipeline Analysis</h4></legend>

                <!-- Aggregation Data Type -->

                <div class="view-control-group control-group">
                    <label class="control-label label-form">Aggregation Data Type</label>
                    <div class="controls">
                        <div class="form-value">
                                ${actionBean.editProduct.aggregationDataType}
                        </div>
                    </div>
                </div>

                <div class="view-control-group control-group">
                    <label class="control-label label-form">Analysis Type</label>
                    <div class="controls">
                        <div class="form-value">
                            <c:if test="${!empty actionBean.editProduct.analysisTypeKey}">
                                ${(actionBean.getAnalysisType(actionBean.editProduct.analysisTypeKey)).displayName}
                            </c:if>
                        </div>
                    </div>
                </div>

                <div class="view-control-group control-group">
                    <label class="control-label label-form"><abbr title="aka Reagent Design">Bait Design</abbr></label>
                    <div class="controls">
                        <div class="form-value">
                            <c:if test="${!empty actionBean.editProduct.reagentDesignKey}">
                                ${(actionBean.getReagentDesign(actionBean.editProduct.reagentDesignKey)).displayName}
                            </c:if>
                        </div>
                    </div>
                </div>
            </fieldset>
        </div>

        <div class="tableBar" style="clear:both;">
            <h4 style="display:inline">Add-ons</h4>
        </div>

        <table id="addOnList" class="table simple">
            <thead>
                <tr>
                    <th>Part Number</th>
                    <th>Product Name</th>
                    <th>Product Family</th>
                    <th>Price Item</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${actionBean.editProduct.addOns}" var="addOnProduct">
                    <tr>
                        <td>
                            <stripes:link href="/products/product.action" event="view">
                                <stripes:param name="product" value="${addOnProduct.businessKey}"/>
                                ${addOnProduct.partNumber}
                            </stripes:link>
                        </td>
                        <td>${addOnProduct.productName}</td>
                        <td>${addOnProduct.productFamily.name}</td>
                        <td>
                            ${addOnProduct.primaryPriceItem.displayName}
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

        <div class="tableBar">
            <h4 style="display:inline">Replacement Price Items</h4>
        </div>
        <table id="optionPriceList" class="table table-striped table-bordered">
            <thead>
                <tr>
                    <th>Platform</th>
                    <th>Category</th>
                    <th>Name</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${actionBean.replacementPriceItems}" var="replacementPriceItem">
                    <tr>
                        <td>${replacementPriceItem.platformName}</td>
                        <td>${replacementPriceItem.categoryName}</td>
                        <td>${replacementPriceItem.name}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

        <div class="tableBar">
            <h4 style="display:inline">Material Types</h4>
        </div>
        <table id="allowedMaterialTypes" class="table table-striped table-bordered">
            <thead>
                <tr>
                    <th>Category</th>
                    <th>Name</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${actionBean.editProduct.allowableMaterialTypes}" var="allowableMaterialType">
                    <tr>
                        <td>${allowableMaterialType.category}</td>
                        <td>${allowableMaterialType.name}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

    </stripes:layout-component>
</stripes:layout-render>
