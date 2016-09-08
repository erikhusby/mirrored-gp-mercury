<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ page import="static org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao.Availability.*" %>
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
                        {"bSortable": false},
                        {"bSortable": true, "sType": "title-string"},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true, "sType" : "title-string"},
                        {"bSortable": true, "sType" : "title-string"}]
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

            <stripes:submit name="publishProductsToSap" value="Publish Selected Product(s) to SAP" class="btn padright" title="Click to publish products to SAP" />
        </div>

        <table id="productList" class="table simple">
            <thead>
            <tr>
                <th width="40">
                    <input for="count" type="checkbox" class="checkAll"/><span id="count" class="checkedCount"></span>
                </th>
                <th>Part Number</th>
                <th>Product Name</th>
                <th>Product Family</th>
                <th>PDM Orderable</th>
                <th>Available</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${actionBean.allProducts}" var="product">
                <tr>
                    <td>
                        <c:if test="${not product.savedInSAP}">
                            <stripes:checkbox name="selectedProductPartNumbers" value="${product.partNumber}" class="shiftCheckbox" />
                        </c:if>
                    </td>
                    <td>
                        <stripes:link beanclass="${actionBean.class.name}" event="view" title="${product.businessKey}">
                            <stripes:param name="product" value="${product.businessKey}"/>
                            ${product.partNumber}
                        </stripes:link>
                    </td>
                    <td>${product.productName}</td>
                    <td>${product.productFamily.name}</td>
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
