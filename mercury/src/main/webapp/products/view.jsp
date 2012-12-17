<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="View Product" sectionTitle="View Product: ${actionBean.product.productName}">

    <stripes:layout-component name="extraHead">
    </stripes:layout-component>
    <stripes:layout-component name="content">

        <div class="form-horizontal">
            <div class="control-group">
                <label class="control-label label-form">Product Family</label>
                <div class="controls">
                    <div class="form-value">${actionBean.product.productFamily.name}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Product Name</label>
                <div class="controls">
                    <div class="form-value">${actionBean.product.productName}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Part Number</label>
                <div class="controls">
                    <div class="form-value">${actionBean.product.partNumber}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Availability Date</label>
                <div class="controls">
                    <div class="form-value">${actionBean.product.availabilityDate}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Discontinued Date</label>
                <div class="controls">
                    <div class="form-value">${actionBean.product.discontinuedDate}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Description</label>
                <div class="controls">
                    <div class="form-value">${actionBean.product.description}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Deliverables</label>
                <div class="controls">
                    <div class="form-value">${actionBean.product.deliverables}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Input Requirements</label>
                <div class="controls">
                    <div class="form-value">${actionBean.product.inputRequirements}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Expected Cycle Time (Days)</label>
                <div class="controls">
                    <div class="form-value">${actionBean.product.expectedCycleTimeDays}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Guaranteed Cycle Time (Days)</label>
                <div class="controls">
                    <div class="form-value">${actionBean.product.guaranteedCycleTimeDays}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Samples Per Week</label>
                <div class="controls">
                    <div class="form-value">${actionBean.product.samplesPerWeek}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Minimum Order Size</label>
                <div class="controls">
                    <div class="form-value">${actionBean.product.minimumOrderSize}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">Primary Price Items</label>
                <div class="controls">
                    <div class="form-value">${actionBean.product.primaryPriceItem.category} : ${actionBean.product.primaryPriceItem.name}</div>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label label-form">PDM Orderable Only</label>
                <div class="controls">
                    <div class="form-value">${actionBean.product.pdmOrderableOnly}</div>
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
                <c:forEach items="${actionBean.product.addOns}" var="addOnProduct">
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
                <c:forEach items="${actionBean.product.optionalPriceItems}" var="optionalPriceItem">
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
