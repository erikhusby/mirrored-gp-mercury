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
                Price Per Unit (e.g. $20/sample)
            </th>
            <th>
                Total Number of Units for the Order
            </th>
            <th>
                Alternate Product Title for Invoice (40 Characters Max -- SAP Orders Only)
            </th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${actionBean.productCustomizations}" var="customization">
            <tr>
                <td>${customization.productName}:  list price -- ${customization.originalPrice}
                <input type="hidden" value="${customization.productPartNumber}" class="partNumber" />
                </td>
                <td><stripes:text name="customPricePlaceholder" value="${customization.price}" class="customPriceValue" disabled="${!actionBean.canEditPrice(customization.units)}"/></td>
                <td><input type="text" value="${customization.quantity}" class="customQuantityValue"/></td>
                <td><input type="text" value="${customization.customName}" class="customProductNameValue"/></td>
            </tr>
        </c:forEach>
        </tbody>
    </table>

</stripes:form>