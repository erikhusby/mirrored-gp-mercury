<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.orders.ProductOrderSampleHistoryActionBean"/>

<script type="text/javascript">
    $j('.sparkline').each(function (index) {
        $j(this).sparkline('html', { type:'bar', stackedBarColor:'blue', width:'300', height:'20', barWidth:'7',
                    tooltipFormat:'{{offset:offset}} {{value:value}}',
                    tooltipValueLookups:{
                        'offset':{
                            ${actionBean.getToolTipLookups()}
                        }
                    },
                }
        )
    });
</script>

<table id="sampleListView" class="table simple">
    <thead>
    <tr>
        <th width="300">History</th>
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
                <span class="sparkline">${actionBean.getSparklineData(sample)}</span>
            </td>
            <td>
                <a href="${ctxpath}/search/all.action?search=&searchKey=${sample.sampleKey}">
                        ${sample.sampleKey}
                </a>
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