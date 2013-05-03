<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%--@elvariable id="vessels" type="java.util.Collection<org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel>"--%>
<%--@elvariable id="sample" type="org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample>"--%>
<%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean"--%>
<%--@elvariable id="index" type="java.lang.Integer"--%>

<stripes:layout-definition>
    <script type="text/javascript">
        $j(document).ready(function () {
            var resultsId = "#sampleEventListView${index}";
            $j(resultsId).dataTable({
                "oTableTools":ttExportDefines,
                "aaSorting":[
                    [1, 'asc']
                ],
                "aoColumns":[
                    {"bSortable":true},
                    {"bSortable":true, "sType":"date"},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true, "sType":"html"}
                ],
                "sDom":""
            })
        });
    </script>

    <table id="sampleEventListView${index}" class="table simple" style="margin: 0 0; width: 1024px">
        <thead>
        <tr>
            <th>Event</th>
            <th>Date</th>
            <th>Location</th>
            <th>Operator</th>
            <th>Index</th>
            <th>JIRAs + PDOs</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${vessels}" var="vessel">
            <c:forEach items="${vessel.inPlaceAndTransferToEvents}" var="event">
                <tr>
                    <td>
                            ${event.labEventType.name}
                    </td>
                    <td>
                        <fmt:formatDate value="${event.eventDate}" pattern="yyyy.MM.dd HH:mm:ss.SSS"/>
                    </td>
                    <td>
                            ${event.eventLocation}
                    </td>
                    <td>
                            ${bean.getUserFullName(event.eventOperator)}
                    </td>
                    <td style="padding: 0;">
                        <table style="padding: 0;">
                            <c:forEach items="${vessel.getIndexesForSample(sample)}" var="curIndex">
                                <c:forEach items="${curIndex.molecularIndexingScheme.indexes}" var="innerIndex">
                                    <tr>
                                        <td style="border: none">
                                                ${innerIndex.key} - ${innerIndex.value.sequence}
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:forEach>
                        </table>
                    </td>
                    <td style="padding: 0;">
                        <table style="padding: 0">
                            <c:forEach items="${vessel.labBatchCompositions}"
                                       var="batchComposition">
                                <tr>
                                    <td>
                                        <a target="JIRA" href="${bean.jiraUrl(batchComposition.labBatch.jiraTicket)}"
                                           class="external" target="JIRA">
                                                ${batchComposition.labBatch.businessKey}
                                            (${batchComposition.count}/${batchComposition.denominator})
                                        </a>
                                    </td>
                                    <td>
                                        <c:forEach items="${vessel.getSampleInstancesForSample(sample)}"
                                                   var="sampleInstance">
                                            <stripes:link
                                                    beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"
                                                    event="view">
                                                <stripes:param name="productOrder"
                                                               value="${sampleInstance.productOrderKey}"/>
                                                ${sampleInstance.productOrderKey}
                                            </stripes:link>
                                        </c:forEach>
                                    </td>
                                </tr>
                            </c:forEach>
                        </table>
                    </td>
                </tr>
            </c:forEach>
        </c:forEach>
        </tbody>
    </table>
</stripes:layout-definition>
