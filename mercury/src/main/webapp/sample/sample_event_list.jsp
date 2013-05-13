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
            $j(".accordion").on("accordionactivate", drawTable);
        });

        function drawTable() {
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
                "bRetrieve": true
            });
        }
    </script>

    <table id="sampleEventListView${index}" class="table simple" style="margin: 0 0; width: 100%;">
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
                        <fmt:formatDate value="${event.eventDate}" pattern="${bean.dateTimePattern}"/>
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
                                <tr>
                                    <td style="border: none">
                                        <c:forEach items="${curIndex.molecularIndexingScheme.indexes}" var="innerIndex">
                                            ${innerIndex.key} - ${innerIndex.value.sequence} &nbsp;
                                        </c:forEach>
                                    </td>
                                </tr>
                            </c:forEach>
                        </table>
                    </td>
                    <td style="padding: 0;">
                        <c:forEach items="${vessel.workflowLabBatchCompositions}" var="batchComposition">
                            <a target="JIRA" href="${bean.jiraUrl(batchComposition.labBatch.jiraTicket)}"
                               class="external" target="JIRA">
                                    ${batchComposition.labBatch.businessKey}
                                (${batchComposition.count}/${batchComposition.denominator})
                            </a>

                            <c:forEach items="${vessel.getSampleInstancesForSample(sample)}"
                                       var="sampleInstance">
                                <c:if test="${not empty sampleInstance.productOrderKey}">
                                    <stripes:link
                                            beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"
                                            event="view" style="margin-left: 3px;">
                                        <stripes:param name="productOrder"
                                                       value="${sampleInstance.productOrderKey}"/>
                                        ${sampleInstance.productOrderKey}
                                    </stripes:link>
                                </c:if>
                            </c:forEach>
                        </c:forEach>
                    </td>
                </tr>
            </c:forEach>
        </c:forEach>
        </tbody>
    </table>
</stripes:layout-definition>
