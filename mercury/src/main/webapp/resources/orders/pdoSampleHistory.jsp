<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.orders.ProductOrderSampleHistoryActionBean"/>
<script type="text/javascript">
    $j(document).ready(function () {
        $j('#sampleListView').dataTable({
            "oTableTools":ttExportDefines,
            "aaSorting":[
                [8, 'desc']
            ],
            "aoColumns":[
                {"bSortable":true, "sType":"date"},
                // first event
                {"bSortable":false},
                // history
                {"bSortable":true, "sType":"date"},
                // last event
                {"bSortable":true},
                // duration
                {"bSortable":true},
                // sample name
                {"bSortable":true},
                // last step
                {"bSortable":true},
                // last process
                {"bSortable":true}
            ]                    // user
        })
    });

</script>

<script type="text/javascript">
    $j('.sparkline').each(function (index) {
        $j(this).sparkline('html', { type:'bar', stackedBarColor:'blue', width:'300', height:'20', barWidth:'7',
                    tooltipFormat:'{{offset:offset}} {{value:value}}',
                    tooltipValueLookups:{
                        'offset':{
                            ${actionBean.getToolTipLookups()}
                        }
                    }
                }
        )
    });
</script>

<table id="sampleListView" class="table simple">
    <thead>
    <tr>
        <th width="120">First Event Date</th>
        <th width="300">History</th>
        <th width="120">Last Event Date</th>
        <th>Process Duration</th>
        <th>Sample Name</th>
        <th>Latest Step</th>
        <th>Latest Process</th>
        <th>Event User</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${actionBean.mercurySamples}" var="sample">
        <tr>
            <td>
                <fmt:formatDate value="${actionBean.getFirstLabEvent(sample).eventDate}"
                                pattern="MM/dd/yyyy HH:MM:ss"/>
            </td>
            <td>
                <span class="sparkline">${actionBean.getSparklineData(sample)}</span>
            </td>
            <td>
                <fmt:formatDate value="${actionBean.getLatestLabEvent(sample).eventDate}"
                                pattern="MM/dd/yyyy HH:MM:ss"/>
            </td>
            <td>
                    ${actionBean.getDuration(sample)}
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
        </tr>
    </c:forEach>
    </tbody>
</table>