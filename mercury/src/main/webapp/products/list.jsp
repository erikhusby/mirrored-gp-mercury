<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ page import="static org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao.Availability.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Products" sectionTitle="List Products" showCreate="true">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#productList').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[0,'asc']],
                    "aoColumns": [
                        <security:authorizeBlock roles="<%= roles(Developer, PDM)%>">
                        {"bSortable": false},                           // checkbox
                        </security:authorizeBlock>
                        {"bSortable": true, "sType": "title-string"},   // Part Number
                        {"bSortable": true},                            // Product Name
                        {"bSortable": true},                            // Product Family
                        <security:authorizeBlock roles="<%= roles(Developer, PDM)%>">
                        {"bSortable": true, "sType": "title-string"},   // Units
                        {"bSortable": true, "sType": "title-string"},   // Price item Display Name
                        {"bSortable": true, "sType": "title-string"},   // Price Item Platform
                        {"bSortable": true, "sType": "numeric"},        // Quote Server Price
                        {"bSortable": true, "sType": "numeric"},        // SAP Price
                        {"bSortable": true, "sType": "numeric"},        // SAP Clinical Charge
                        {"bSortable": true, "sType": "numeric"},        // SAP Commerical Charge
                        {"bSortable": true, "sType": "numeric"},        // SAP SSF Intercompany Charge
                        </security:authorizeBlock>
                        {"bSortable": true, "sType" : "title-string"},  // Commercial Indicator
                        {"bSortable": true, "sType" : "title-string"},  // Clinical Indicator
                        {"bSortable": true, "sType" : "title-string"},  // PDM Orderable Indicator
                        {"bSortable": true, "sType" : "title-string"}]  // Availibility Indicaory
                })
            });

            function changeAvailability() {
                $j("#list").submit();
            }

        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <div class="clearfix"></div>
        <stripes:form id="list" beanclass="${actionBean.class.name}">
        <div class="actionButtons">
            <img src="${ctxpath}/images/pdficon_small.png" alt=""/>
            <stripes:link beanclass="${actionBean.class.name}" event="downloadProductDescriptions">
                Download Product Descriptions</stripes:link>

                <stripes:radio onchange="changeAvailability()" value="<%= ProductDao.Availability.ALL%>"
                               name="availability"/> All Products
                <stripes:radio onchange="changeAvailability()" value="<%= ProductDao.Availability.CURRENT%>"
                               name="availability"/> Available Products Only
            <security:authorizeBlock roles="<%= roles(Developer, PDM)%>">
                <stripes:submit name="publishProductsToSap" value="Publish Selected Product(s) to SAP"
                                class="btn padright" title="Click to publish products to SAP"/>
            </security:authorizeBlock>
        </div>

        <table id="productList" class="table simple">
            <thead>
            <tr>
                <security:authorizeBlock roles="<%= roles(Developer, PDM)%>">
                <th width="40">
                    <input for="count" type="checkbox" class="checkAll"/><span id="count" class="checkedCount"></span>
                </th>
                </security:authorizeBlock>
                <th>Part Number</th>
                <th>Product Name</th>
                <th>Product Family</th>
                <security:authorizeBlock roles="<%= roles(Developer, PDM)%>">
                    <th>Units</th>
                    <th>Price Item Display Name</th>
                    <th>Price Item Platform</th>
                    <th>Quote Server Price</th>
                    <th>SAP List Price</th>
                    <th>SAP Clinical Charge</th>
                    <th>SAP Commercial Charge</th>
                    <th>SAP SSF intercompany Charge</th>
                </security:authorizeBlock>
                <th>Commercial?</th>
                <th>Clinical?</th>
                <th>PDM Orderable</th>
                <th>Available</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${actionBean.allProducts}" var="product">
                <c:set var="priceClass" value="" />
                <c:set var="inSAP" value="${actionBean.productInSAP(product.partNumber, product.determineCompanyConfiguration())}" />
                <security:authorizeBlock roles="<%= roles(Developer, PDM)%>">
                    <c:if test="${inSAP && !product.quoteServerPrice.equals(product.sapFullPrice)}">
                        <c:set var="priceClass" value="bad-prices"/>
                    </c:if>
                </security:authorizeBlock>
                <tr class="${priceClass}">

                    <td>
                        <span class="bad-price-div">The prices for this product information differ between the quote server and SAP.  Please correct this before any orders can be placed or updated
                        </span>
                        <security:authorizeBlock roles="<%= roles(Developer, PDM)%>">
                        <c:if test="${!inSAP}">
                            <stripes:checkbox name="selectedProductPartNumbers" value="${product.partNumber}"
                                              class="shiftCheckbox"/>
                        </c:if>
                        </security:authorizeBlock>
                    </td>
                    <td>
                    <stripes:link beanclass="${actionBean.class.name}" event="view" title="${product.businessKey}">
                        <stripes:param name="product" value="${product.businessKey}"/>
                        ${product.partNumber}
                    </stripes:link>
                </td>

                <td>${product.productName}</td>
                <td>${product.productFamily.name}</td>
                <security:authorizeBlock roles="<%= roles(Developer, PDM)%>">
                    <td>${product.primaryPriceItem.units}</td>
                    <td>${product.primaryPriceItem.displayName}</td>
                    <td>${product.primaryPriceItem.platform}</td>
                    <td>${product.quoteServerPrice}</td>
                    <td>
                        <c:if test="${inSAP}">
                            ${product.sapFullPrice}
                        </c:if>
                    </td>

                    <td>
                        <c:if test="${inSAP}">
                            ${product.sapClinicalCharge}
                        </c:if>
                    </td>
                    <td>
                        <c:if test="${inSAP}">
                            ${product.sapCommercialCharge}
                        </c:if>
                    </td>
                    <td>
                        <c:if test="${inSAP}">
                            ${product.sapSSFIntercompanyCharge}
                        </c:if>
                    </td>
                </security:authorizeBlock>
                <td>
                    <c:if test="${product.externalOnlyProduct}">
                        <img src="${ctxpath}/images/check.png" alt="yes" title="yes"/>
                    </c:if>
                </td>
                <td>
                    <c:if test="${product.clinicalProduct}">
                        <img src="${ctxpath}/images/check.png" alt="yes" title="yes"/>
                    </c:if>
                </td>
                <td>
                    <c:if test="${product.pdmOrderableOnly}">
                        <img src="${ctxpath}/images/check.png" alt="yes" title="yes"/>
                    </c:if>
                </td>
                <td>
                    <c:if test="${product.available}">
                        <img src="${ctxpath}/images/check.png" alt="yes" title="yes"/>
                    </c:if>
                </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
