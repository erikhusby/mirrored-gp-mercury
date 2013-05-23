<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ page import="static org.broadinstitute.gpinformatics.mercury.entity.DB.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.mercury.entity.DB.roles" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Products" sectionTitle="List Products" showCreate="true">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j('#productList').dataTable({
                    "oTableTools": ttExportDefines,
                    "aaSorting": [
                        [0, 'asc']
                    ],
                    "aoColumns": [
                        {"bSortable": true, "sType": "title-string"},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true, "sType": "title-string"}
                    ]
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <div class="clearfix"></div>

        <table id="productList" class="table simple">
            <thead>
            <tr>
                <th>Part Number</th>
                <th>Product Name</th>
                <th>Product Family</th>
                <th>Available</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${actionBean.allProducts}" var="product">
                <tr>
                    <td>
                        <stripes:link beanclass="${actionBean.class.name}" event="view"
                                      title="${product.businessKeyList}">
                            <stripes:param name="product" value="${product.businessKeyList}"/>
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
