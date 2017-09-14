<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"/>

<stripes:form beanclass="${actionBean.class.name}">

    <table id="customizationData">

        <thead>
        <tr>
            <th>
                Product name
            </th>
            <th>
                Price Override
            </th>
            <th>
                Quantity Override
            </th>
            <th>
                Custom Product Name
            </th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${actionBean.productCustomizations}" var="customization">
            <tr>
                <td>${customization.productName}
                <stripes:hidden name="${customization.productPartNumber}" class="partNumber" />
                </td>
                <td><stripes:text name="${customization.price}" class="customPriceValue"/></td>
                <td><stripes:text name="${customization.quantity}" class="customQuantityValue"/></td>
                <td><stripes:text name="${customization.customName}" class="customProductNameValue"/></td>
            </tr>
        </c:forEach>
        </tbody>
    </table>

</stripes:form>