<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="List Products" sectionTitle="List Products">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#productList').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[1,'asc']],
                    "aoColumns": [
                        {"bSortable": true, "sType": "html"},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true, "sType" : "title-string"}]
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <p>
            <stripes:link title="New Product" beanclass="${actionBean.class.name}" event="create" class="pull-right">
                New product
            </stripes:link>
        </p>

        <div class="clearfix"></div>

        <table id="productList" class="table simple">
            <thead>
                <tr>
                    <th>Part Number</th>
                    <th>Product Name</th>
                    <th>Product Family</th>
                    <th>Is Available</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${actionBean.allProducts}" var="product">
                    <tr>
                        <td>
                            <stripes:link beanclass="${actionBean.class.name}" event="view">
                                <stripes:param name="productKey" value="${product.businessKey}"/>
                                ${product.partNumber}
                            </stripes:link>
                        </td>
                        <td>${product.productName}</td>
                        <td>${product.productFamily.name}</td>
                        <td>
                            <c:if test="${product.available}">
                                <img src="${ctxpath}/images/check.png" alt="yes" title="yes"/>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </stripes:layout-component>
</stripes:layout-render>
