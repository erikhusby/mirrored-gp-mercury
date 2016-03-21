<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%--@elvariable id="vessel" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel"--%>
<%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean"--%>
<%--@elvariable id="index" type="java.lang.Integer"--%>

<stripes:layout-definition>
    <script type="text/javascript">
        $j(document).ready(function () {
            $j(".accordion").on("accordionactivate", function (event, ui) {
                var active = $j('.accordion').accordion('option', 'active');
                var resultsId = "#vesselSampleListView" + active;
                $j(resultsId).dataTable({
                    "oTableTools":ttExportDefines,
                    "aaSorting":[
                        [2, 'asc']
                    ],
                    "aoColumns":[
                        {"bSortable":true, sWidth:'100px'},
                        {"bSortable":true},
                        {"bSortable":true, sWidth:'1px'},
                        {"bSortable":true, "sType":"html"}
                    ],
                    "bRetrieve":true,
                    "sScrollY":500
                });
            });
        });
    </script>

    <table id="vesselSampleListView${index - 1}" class="table simple" style="margin: 0 0; width: 100%;">
        <thead>
        <tr>
            <th>Sample</th>
            <th>Index</th>
            <th>Position</th>
            <th>JIRAs + PDOs</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${vessel.sampleInstancesV2}" var="sample">
            <%--@elvariable id="sample" type="org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2"--%>
            <tr>
                <td>
                    <stripes:link
                            beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.SampleSearchActionBean"
                            event="sampleSearch">
                        <stripes:param name="searchKey" value="${sample.nearestMercurySampleName}"/>
                        ${sample.nearestMercurySampleName}
                    </stripes:link>
                </td>
                <td style="padding: 0;">
                    <table style="padding: 0;">
                        <c:forEach items="${vessel.getIndexesForSampleInstance(sample)}" var="curIndex">
                            <%--@elvariable id="curIndex" type="org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent"--%>
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
                    <table style="padding: 0">
                        <c:if test="${vessel.containerRole != null}">
                            <c:forEach items="${vessel.containerRole.getPositionsOfSampleInstanceV2(sample)}"
                                       var="position">
                                <tr>
                                    <td style="border: none;">
                                            ${position}
                                    </td>
                                </tr>
                            </c:forEach>
                        </c:if>
                    </table>
                </td>
                <td style="padding: 0;">
                    <c:choose>
                        <c:when test="${sample.singleBatch != null}">
                            <a target="JIRA" href="${sample.singleBatch.jiraTicket.browserUrl}"
                               class="external" target="JIRA">
                                    ${sample.singleBatch.jiraTicket.ticketName}
                            </a>&nbsp;&nbsp;
                        </c:when>
                        <c:otherwise>
                            <c:forEach items="${sample.allWorkflowBatches}"
                                       var="labBatch">
                                <a target="JIRA" href="${labBatch.jiraTicket.browserUrl}"
                                   class="external">
                                        ${labBatch.jiraTicket.ticketName}
                                </a>&nbsp;&nbsp;
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                    <c:choose>
                        <c:when test="${sample.singleProductOrderSample != null}">
                            <c:set var="productOrderSample" value="${sample.singleProductOrderSample}"/>
                            <stripes:link
                                    beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"
                                    event="view">
                                <stripes:param name="productOrder" value="${productOrderSample.productOrder.jiraTicketKey}"/>
                                ${productOrderSample.productOrder.jiraTicketKey}
                            </stripes:link>
                            <br/>
                        </c:when>
                        <c:otherwise>
                            <c:forEach items="${sample.allProductOrderSamples}"
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
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</stripes:layout-definition>
