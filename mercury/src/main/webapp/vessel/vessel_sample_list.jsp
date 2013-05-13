<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%--@elvariable id="vessel" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel"--%>
<%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean"--%>
<%--@elvariable id="index" type="java.lang.Integer"--%>

<stripes:layout-definition>
    <script type="text/javascript">

        $j(document).ready(function () {
            $j(".accordion").on("accordionactivate", drawTable);
        });

        function drawTable() {
            var resultsId = "#vesselSampleListView${index}";
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
                "bRetrieve": true
            });
        }
    </script>

    <table id="vesselSampleListView${index}" class="table simple" style="margin: 0 0; width: 100%;">
        <thead>
        <tr>
            <th>Sample</th>
            <th>Index</th>
            <th>Position</th>
            <th>JIRAs + PDOs</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${vessel.sampleInstances}" var="sample">
            <tr>
                <td>
                    <stripes:link
                            beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.SampleSearchActionBean"
                            event="sampleSearch">
                        <stripes:param name="searchKey" value="${sample.startingSample.sampleKey}"/>
                        ${sample.startingSample.sampleKey}
                    </stripes:link>
                </td>
                <td style="padding: 0;">
                    <table style="padding: 0;">
                        <c:forEach items="${vessel.getIndexesForSample(sample.startingSample)}" var="curIndex">
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
                        <c:forEach items="${vessel.getPositionsOfSample(sample)}" var="position">
                            <tr>
                                <td style="border: none;">
                                        ${position}
                                </td>
                            </tr>
                        </c:forEach>
                    </table>
                </td>
                <td style="padding: 0;">
                    <c:forEach items="${sample.getLabBatchCompositionInVesselContext(vessel)}"
                               var="batchComposition">
                        <c:if test="${not empty batchComposition.labBatch.businessKey}">
                            <a target="JIRA" href="${bean.jiraUrl(batchComposition.labBatch.jiraTicket)}"
                               class="external" target="JIRA">
                                    ${batchComposition.labBatch.businessKey}
                                (${batchComposition.count}/${batchComposition.denominator})
                            </a>
                        </c:if>

                        <c:if test="${not empty sample.productOrderKey}">
                            <stripes:link
                                    beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"
                                    event="view">
                                <stripes:param name="productOrder" value="${sample.productOrderKey}"/>
                                ${sample.productOrderKey}
                            </stripes:link>
                        </c:if>
                        <br/>
                    </c:forEach>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</stripes:layout-definition>
