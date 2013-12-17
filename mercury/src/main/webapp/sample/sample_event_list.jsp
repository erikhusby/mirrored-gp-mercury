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
                        <c:forEach items="${bean.getSampleInstancesForSample(vessel, sample, 'ANY')}"
                                   var="sampleInstance">
                            <%--@elvariable id="sampleInstance" type="org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance"--%>
                            <c:if test="${vessel.containerRole != null}">
                                <c:forEach items="${vessel.containerRole.getPositionsOfSampleInstance(sampleInstance)}"
                                           var="position">
                                    ${position}
                                </c:forEach>
                            </c:if>
                        </c:forEach>
                    </td>
                    <td class="columnEventSample">
                        <c:forEach items="${bean.getSampleInstancesForSample(vessel, sample, 'ANY')}"
                                   var="sampleInstance">
                            <%--@elvariable id="sampleLink" type="org.broadinstitute.gpinformatics.infrastructure.presentation.SampleLink"--%>
                            <c:set var="sampleLink" value="${bean.getSampleLink(sampleInstance.startingSample)}"/>
                            <c:choose>
                                <c:when test="${sampleLink.hasLink}">
                                    <stripes:link class="external" target="${sampleLink.target}"
                                                  title="${sampleLink.label}" href="${sampleLink.url}">
                                        ${sampleInstance.startingSample.sampleKey}
                                    </stripes:link>
                                </c:when>
                                <c:otherwise>
                                    ${sampleInstance.startingSample.sampleKey}
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
                        <c:forEach items="${bean.getSampleInstancesForSample(vessel, sample, 'WITH_PDO')}"
                                   var="sampleInstance">
                            <%--@elvariable id="sampleInstance" type="org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance"--%>
                            <c:forEach items="${sampleInstance.getLabBatchCompositionInVesselContext(vessel)}"
                                       var="batchComposition">
                                <c:if test="${not empty batchComposition.labBatch.businessKey}">
                                    <a target="JIRA" href="${batchComposition.labBatch.jiraTicket.browserUrl}"
                                       class="external" target="JIRA">
                                            ${batchComposition.labBatch.businessKey}
                                        (${batchComposition.count}/${batchComposition.denominator})
                                    </a>&nbsp;&nbsp;
                                </c:if>

                                <c:if test="${not empty sampleInstance.productOrderKey}">
                                    <stripes:link
                                            beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"
                                            event="view">
                                        <stripes:param name="productOrder" value="${sampleInstance.productOrderKey}"/>
                                        ${sampleInstance.productOrderKey}
                                    </stripes:link>
                                </c:if>
                                <br/>
                            </c:forEach>
                        </c:forEach>
                    </td>
                </tr>
            </c:forEach>
        </c:forEach>
        </tbody>
    </table>
</stripes:layout-definition>
