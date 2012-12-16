<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="List Products">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#productList').dataTable( {
                    "sDom": '<"H"lfr>t<"F"Tip>',
                    "sWrapper": "dataTables_wrapper form-inline",
                    "bAutoWidth": false,
                    "bInfo": false,
                    "bStateSave": false,
                    "bJQueryUI": true,
                    "bPaginate": false,
                    "sSearch": "Filter:",
                    "bLengthChange": false,
                    "asStripClasses": [ 'odd', 'even' ],
                    "aaSorting": [[1,'asc']],
                    "aoColumns": [
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": false}],
                    "oTableTools": {
                        "sSwfPath": "${ctxpath}/resources/scripts/DataTables-1.9.4/extras/TableTools/media/swf/copy_csv_xls.swf",
                        "aButtons": [ "copy", "csv", "print" ]
                    }
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <table id="productList" class="table table-striped table-bordered">
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
                            <stripes:link href="/products/product.action" event="view">
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
