<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%--@elvariable id="batch" type="org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch"--%>
<%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.LCSetSearchActionBean"--%>
<%--@elvariable id="index" type="java.lang.Integer"--%>

<stripes:layout-definition>
    <script type="text/javascript">
        $j(document).ready(function () {
            $j(".accordion").on("accordionactivate", function (event, ui) {
                var active = $j('.accordion').accordion('option', 'active');
                var resultsId = "#batchSampleListView" + active;
                $j(resultsId).dataTable({
                    "oTableTools":ttExportDefines,
                    "aaSorting":[
                        [0, 'asc']
                    ],
                    "aoColumns":[
                        {"bSortable":true, sWidth:'100px', sType:'html'},
                        { "bSortable":true, sType:'numeric'},
                        { "bSortable":true, sType:'numeric'},
                        { "bSortable":true, sType:'numeric'},
                        { "bSortable":true, sType:'numeric'},
                        { "bSortable":true},
                        { "bSortable":true},
                        { "bSortable":true},
                        { "bSortable":true},
                        { "bSortable":true},
                        { "bSortable":true},
                        { "bSortable":true},
                        {"bSortable":true, sType:'date'}

                    ],
                    "bRetrieve":true,
                    "sScrollY":500
                });
            });
        });
    </script>
    <div>
        JIRA Link <a target="JIRA" href="${batch.jiraTicket.browserUrl}" class="external"
                     target="JIRA"> ${batch.businessKey} </a>
    </div>
    <table id="batchSampleListView${index - 1}" class="table simple" style="margin: 0 0; width: 100%;">
        <thead>
        <tr>
            <th>Sample</th>
            <th>BSP Pico</th>
            <th>Catch Pico</th>
            <th>Pond Pico</th>
            <th>ECO QPCR</th>
            <th>Export Position</th>
            <th>Shearing Position</th>
            <th>Pond Position</th>
            <th>Catch Position</th>
            <th>Latest Event</th>
            <th>Event Operator</th>
            <th>Event Location</th>
            <th>Event Date</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${batch.startingBatchLabVessels}" var="vessel">
            <c:forEach items="${vessel.getSampleInstances('WITH_PDO', null)}" var="sample">
                <tr>
                    <td><stripes:link
                            beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.SampleSearchActionBean"
                            event="sampleSearch">
                        <stripes:param name="searchKey" value="${sample.startingSample.sampleKey}"/>
                        ${sample.startingSample.sampleKey}
                    </stripes:link></td>
                    <td> ${bean.sampleToBspPicoValueMap.get(sample.startingSample.sampleKey).concentration} </td>
                    <td> ${vessel.metricsForVesselandDescendants.get("Catch Pico").value} </td>
                    <td> ${vessel.metricsForVesselandDescendants.get("Pond Pico").value} </td>
                    <td> ${vessel.metricsForVesselandDescendants.get("ECO QPCR").value} </td>
                    <td> ${bean.getPositionsForEvent(vessel, "SAMPLE_IMPORT")}</td>
                    <td> ${bean.getPositionsForEvent(vessel, "SHEARING_TRANSFER")}</td>
                    <td> ${bean.getPositionsForEvent(vessel, "POND_ENRICHMENT")}</td>
                    <td> ${bean.getPositionsForEvent(vessel, "NORMALIZED_CATCH_REGISTRATION")}</td>
                    <td> ${bean.getLatestEventForVessel(vessel).labEventType.name} </td>
                    <td> ${bean.getUserFullName(bean.getLatestEventForVessel(vessel).eventOperator)} </td>
                    <td> ${bean.getLatestEventForVessel(vessel).eventLocation} </td>
                    <td><fmt:formatDate value="${bean.getLatestEventForVessel(vessel).eventDate}"
                                        pattern="${bean.dateTimePattern}"/></td>
                </tr>
            </c:forEach>
        </c:forEach>
        </tbody>
    </table>
</stripes:layout-definition>
