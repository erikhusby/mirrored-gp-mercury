<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<script type="text/javascript">

</script>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.orders.ProductOrderSampleHistoryActionBean"/>

<table id="sampleListView" class="table simple">
    <thead>
    <tr>
        <th>Sample Name</th>
        <th>Latest Step</th>
        <th>Latest Process</th>
        <th>Event User</th>
        <th>Event Date</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${actionBean.mercurySamples}" var="sample">
        <tr>
            <td>
                    ${sample.sampleKey}
            </td>
            <td>
                    ${actionBean.getLatestLabEvent(sample).labEventType.name}
            </td>
            <td>
                    ${actionBean.getLatestProcess(actionBean.getLatestLabEvent(sample)).processDef.name}
            </td>
            <td>
                    ${actionBean.getUserFullName(actionBean.getLatestLabEvent(sample).eventOperator)}
            </td>
            <td>
                <fmt:formatDate value="${actionBean.getLatestLabEvent(sample).eventDate}"
                                pattern="MM/dd/yyyy HH:MM:SS"/>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>