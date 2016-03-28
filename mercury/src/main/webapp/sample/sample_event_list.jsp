<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%--@elvariable id="vessels" type="java.util.Collection<org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel>"--%>
<%--@elvariable id="sample" type="org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample"--%>
<%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.SampleSearchActionBean"--%>
<%--@elvariable id="index" type="java.lang.Integer"--%>

<stripes:layout-definition>
    <script type="text/javascript">
        $j(document).ready(function () {
            $j(".accordion").on("accordionactivate", function (event, ui) {
                var active = $j('.accordion').accordion('option', 'active');
                var resultsId = "#sampleEventListView-${sample.sampleKey}";
                $j(resultsId).dataTable({
                    "oTableTools":ttExportDefines,
                    "aaSorting":[
                        [4, 'asc']
                    ],
                    "aoColumns":[
                        {"bSortable":true},
                        {"bSortable":true},
                        {"bSortable":true, "sType":"html"},
                        {"bSortable":true},
                        {"bSortable":true, "sType":"date"},
                        {"bSortable":true},
                        {"bSortable":true},
                        {"bSortable":true},
                        {"bSortable":true},
                        {"bSortable":true, "sType":"html"}
                    ],
                    "bRetrieve":true,
                    "sScrollX":"100%",
                    "sScrollY":500
                });
            });
        });
    </script>

    <style type="text/css">
        .columnEventVessel { width: 5em; }
        .columnPosition { width: 4em; }
        .columnEventSample { width: 5em; }
        .columnEvent { width: 20em; }
        .columnDate { width: 5em; }
        .columnLocation { width: 8em; }
        .columnScript { width: 8em; }
        .columnOperator { width: 5em; }
        .columnIndex { width: 12em; }
        .columnJirasAndPdos { width: 9em; }
    </style>

    <table id="sampleEventListView-${sample.sampleKey}" class="table simple" style="table-layout: fixed;">
        <thead>
        <tr>
            <th class="columnEventVessel">Event Vessel</th>
            <th class="columnPosition">Position</th>
            <th class="columnEventSample">Event Sample</th>
            <th class="columnEvent">Event</th>
            <th class="columnDate">Date</th>
            <th class="columnLocation">Location</th>
            <th class="columnScript">Script</th>
            <th class="columnOperator">Operator</th>
            <th class="columnIndex">Index</th>
            <th class="columnJirasAndPdos">JIRAs + PDOs</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${vessels}" var="vessel">
            <c:forEach items="${vessel.inPlaceAndTransferToEvents}" var="event">
                <tr>
                    <td class="columnEventVessel">
                        <stripes:link
                                beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.VesselSearchActionBean"
                                event="vesselSearch">
                            <stripes:param name="searchKey" value="${vessel.label}"/>
                            ${vessel.label}
                        </stripes:link>
                    </td>
                    <td class="columnPosition">
                        <c:forEach items="${bean.getSampleInstancesForSample(vessel, sample)}"
                                   var="sampleInstance">
                            <%--@elvariable id="sampleInstance" type="org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2"--%>
                            <c:if test="${vessel.containerRole != null}">
                                <c:forEach items="${vessel.containerRole.getPositionsOfSampleInstanceV2(sampleInstance)}"
                                           var="position">
                                    ${position}
                                </c:forEach>
                            </c:if>
                        </c:forEach>
                    </td>
                    <td class="columnEventSample">
                        <c:forEach items="${bean.getSampleInstancesForSample(vessel, sample)}"
                                   var="sampleInstance">
                            <%--@elvariable id="sampleInstance" type="org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2"--%>
                            <%--@elvariable id="sampleLink" type="org.broadinstitute.gpinformatics.infrastructure.presentation.SampleLink"--%>
                            <c:set var="sampleLink" value="${bean.getSampleLink(sampleInstance.nearestMercurySample)}"/>
                            <c:choose>
                                <c:when test="${sampleLink.hasLink}">
                                    <stripes:link class="external" target="${sampleLink.target}"
                                                  title="${sampleLink.label}" href="${sampleLink.url}">
                                        ${sampleInstance.nearestMercurySampleName}
                                    </stripes:link>
                                </c:when>
                                <c:otherwise>
                                    ${sampleInstance.nearestMercurySampleName}
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </td>
                    <td class="columnEvent ellipsis">
                            ${event.labEventType.name}
                    </td>
                    <td class="columnDate">
                        <fmt:formatDate value="${event.eventDate}" pattern="${bean.dateTimePattern}"/>
                    </td>
                    <td class="columnLocation">
                            ${event.eventLocation}
                    </td>
                    <td class="columnScript ellipsis">
                            ${event.programName}
                    </td>
                    <td class="columnOperator">
                            ${bean.getUserFullName(event.eventOperator)}
                    </td>
                    <td class="columnIndex" style="padding: 0;">
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
                    <td class="columnJirasAndPdos" style="padding: 0;">
                        <c:forEach items="${bean.getSampleInstancesForSample(vessel, sample)}"
                                   var="sampleInstance">
                            <%--@elvariable id="sampleInstance" type="org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2"--%>
                            <c:choose>
                                <c:when test="${sampleInstance.singleBatch != null}">
                                    <a target="JIRA" href="${sampleInstance.singleBatch.jiraTicket.browserUrl}"
                                       class="external" target="JIRA">
                                            ${sampleInstance.singleBatch.jiraTicket.ticketName}
                                    </a>&nbsp;&nbsp;
                                </c:when>
                                <c:otherwise>
                                    <c:forEach items="${sampleInstance.allWorkflowBatches}"
                                               var="labBatch">
                                        <a target="JIRA" href="${labBatch.jiraTicket.browserUrl}"
                                           class="external" target="JIRA">
                                                ${labBatch.jiraTicket.ticketName}
                                        </a>&nbsp;&nbsp;
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                            <c:choose>
                                <c:when test="${sampleInstance.singleProductOrderSample != null}">
                                    <c:set var="productOrderSample" value="${sampleInstance.singleProductOrderSample}"/>
                                    <stripes:link
                                            beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"
                                            event="view">
                                        <stripes:param name="productOrder" value="${productOrderSample.productOrder.jiraTicketKey}"/>
                                        ${productOrderSample.productOrder.jiraTicketKey}
                                    </stripes:link>
                                    <br/>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach items="${sampleInstance.allProductOrderSamples}"
                                               var="productOrderSample">
                                        <stripes:link
                                                beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"
                                                event="view">
                                            <stripes:param name="productOrder" value="${productOrderSample.productOrder.jiraTicketKey}"/>
                                            ${productOrderSample.productOrder.jiraTicketKey}
                                        </stripes:link>
                                        <br/>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </td>
                </tr>
            </c:forEach>
        </c:forEach>
        </tbody>
    </table>
</stripes:layout-definition>
