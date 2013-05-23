<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-definition>

    <%--@elvariable id="pdos" type="java.util.Collection"--%>
    <%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean"--%>
    <script type="text/javascript">
        $j(document).ready(function () {
            $j('#pdoListView').dataTable({
                "oTableTools": ttExportDefines,
                "aaSorting": [
                    [1, 'asc']
                ],
                "aoColumns": [
                    {"bSortable": false},
                    {"bSortable": true},
                    {"bSortable": true},
                    {"bSortable": true},
                    {"bSortable": true, "sType": "numeric"},
                    {"bSortable": true},
                    {"bSortable": true, "sType": "date"}
                ]
            });

            if (${fn:length(pdos) == 1}) {
                showPDOSampleHistory('${pdos[0].businessKeyList}');
            }

        });

        function showPDOSampleHistory(label) {
            $j('#viewerDiv').html("<img src=\"${ctxpath}/images/spinner.gif\"/>");
            $j('#viewerDiv').load('${ctxpath}/view/pdoSampleHistory.action?businessKeyList=' + label);
            $j('#viewerDiv').show();
        }

    </script>
    <table id="pdoListView" class="table simple">
        <thead>
        <tr>
            <th width=30>Sample History</th>
            <th>PDO ID</th>
            <th>PDO Title</th>
            <th>Product</th>
            <th>Sample Count</th>
            <th>Created By</th>
            <th>Create Date</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${pdos}" var="pdo">
            <tr>
                <td>
                    <a href="javascript:showPDOSampleHistory('${pdo.businessKeyList}')">
                        <img width="30" height="30" name="" title="show sample history"
                             src="${ctxpath}/images/list.png"/>
                    </a>
                </td>
                <td>
                    <a href="${ctxpath}/search/all.action?search=&searchKey=${pdo.businessKeyList}">
                            ${pdo.businessKeyList}
                    </a>
                </td>
                <td>
                    <stripes:link
                            beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"
                            event="view" class="external">
                        <stripes:param name="productOrder" value="${pdo.businessKeyList}"/>
                        ${pdo.title}
                    </stripes:link>
                </td>
                <td>
                        ${pdo.product.productName}
                </td>
                <td>
                        ${pdo.totalSampleCount}
                </td>
                <td>
                        ${bean.getUserFullName(pdo.createdBy)}
                </td>
                <td>
                    <fmt:formatDate value="${pdo.createdDate}" pattern="${bean.datePattern}"/>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
    <div id="viewerDiv" style="display:none"></div>
</stripes:layout-definition>