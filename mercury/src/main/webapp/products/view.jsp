<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="View Product" sectionTitle="View Product: ${actionBean.editProduct.productName}">

    <stripes:layout-component name="content">

        <div class="form-horizontal">
            <div class="control-group">
                <label class="control-label label-form">Product Family</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.productFamily.name}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Product Name</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.productName}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Part Number</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.partNumber}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Availability Date</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.availabilityDate}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Discontinued Date</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.discontinuedDate}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Description</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.description}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Deliverables</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.deliverables}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Input Requirements</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.inputRequirements}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Expected Cycle Time (Days)</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.expectedCycleTimeDays}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Guaranteed Cycle Time (Days)</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.guaranteedCycleTimeDays}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Samples Per Week</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.samplesPerWeek}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Minimum Order Size</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.minimumOrderSize}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Primary Price Items</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.primaryPriceItem.category} : ${actionBean.editProduct.primaryPriceItem.name}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">PDM Orderable Only</label>
                <div class="controls">
                    <div class="form-value">${actionBean.editProduct.pdmOrderableOnly}</div>
                </div>
            </div>
        </div>

        <div style="width:98%; border-bottom: 2px solid #4169e1;">
            Add Ons
        </div>
        <table id="addOnList" style="width:98%" class="table table-striped table-bordered">
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
                                <stripes:param name="productKey" value="${addOnProduct.businessKey}"/>
                                ${addOnProduct.partNumber}
                            </stripes:link>
                        </td>
                        <td>${addOnProduct.productName}</td>
                        <td>${addOnProduct.productFamily.name}</td>
                        <td>
                            ${addOnProduct.primaryPriceItem.category} : ${addOnProduct.primaryPriceItem.name}
                            <c:if test="${addOnProduct.available}">
                                <img src="${ctxpath}/images/check.png" alt="yes" title="yes"/>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

        <div style="width:98%; border-bottom: 2px solid #4169e1;">
            Optional Price Items
        </div>
        <table id="optionPriceList" style="width:98%" class="table table-striped table-bordered">
            <thead>
                <tr>
                    <th>Platform</th>
                    <th>Category</th>
                    <th>Name</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${actionBean.editProduct.optionalPriceItems}" var="optionalPriceItem">
                    <tr>
                        <td>${optionalPriceItem.platform}</td>
                        <td>${optionalPriceItem.category}</td>
                        <td>${optionalPriceItem.name}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </stripes:layout-component>
</stripes:layout-render>
