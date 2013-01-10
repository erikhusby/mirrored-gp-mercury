<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-definition>

    <%--@elvariable id="pdos" type="java.util.Collection"--%>
    <%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean"--%>

    <table id="pdoListView" class="table simple">
        <thead>
        <tr>
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
                        ${pdo.businessKey}
                </td>
                <td>
                    <stripes:link
                            beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"
                            event="view">
                        <stripes:param name="productOrder" value="${pdo.businessKey}"/>
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
                        ${bean.fullNameMap[pdo.createdBy]}
                </td>
                <td>
                    <fmt:formatDate value="${pdo.createdDate}" pattern="MM/dd/yyyy"/>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</stripes:layout-definition>