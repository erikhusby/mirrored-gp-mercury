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
                    oTableTools:ttExportDefines,
                    aaSorting:[
                        [1, 'asc']
                    ],
                    aoColumns:[
                        { bSortable:false},
                        { bSortable:true, sWidth:'100px', sType:'html'},
                        { bSortable:true, sType:'numeric'},
                        { bSortable:true, sType:'numeric'},
                        { bSortable:true, sType:'numeric'},
                        { bSortable:true, sType:'numeric'},
                        { bSortable:true, sType:'numeric'},
                        { bSortable:true, sType:'numeric'},
                        { bSortable:true},
                        { bSortable:true},
                        { bSortable:true},
                        { bSortable:true},
                        { bSortable:true},
                        { bSortable:true},
                        { bSortable:true},
                        { bSortable:true, sType:'date'}

                    ],
                    bRetrieve:true,
                    sScrollY:500
                });
                $j('.sample-checkbox' + active).enableCheckboxRangeSelection({
                    checkAllClass:'sample-checkAll' + active,
                    countDisplayClass:'sample-checkedCount' + active,
                    checkboxClass:'sample-checkbox' + active});

                $j('.initialPico').heatcolor(function () {
                    return $j(this).text();
                }, { lightness:0.6, maxval:60, minval:10, colorStyle:'greentored' });
                $j('.exportPico').heatcolor(function () {
                    return $j(this).text();
                }, { lightness:0.6, maxval:3.5, minval:1.5, colorStyle:'greentored' });
                $j('.pondPico').heatcolor(function () {
                    return $j(this).text();
                }, { lightness:0.6, maxval:75, minval:25, colorStyle:'greentored' });
                $j('.catchPico').heatcolor(function () {
                    return $j(this).text();
                }, { lightness:0.6, maxval:15, minval:2, colorStyle:'greentored' });
                $j('.ecoQPCR').heatcolor(function () {
                    return $j(this).text();
                }, { lightness:0.6, maxval:60, minval:10, colorStyle:'greentored' });
            });
        });
    </script>

    <stripes:form beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.SampleLibrariesActionBean">
        <div class="actionButtons">
            <stripes:hidden name="searchKey"/>
            <stripes:submit name="showLibraries" value="Show Libraries" class="btn" style="margin-right:30px;"/>
            JIRA Link <a target="JIRA" href="${batch.jiraTicket.browserUrl}" class="external"
                         target="JIRA"> ${batch.businessKey} </a>
        </div>
        <table id="batchSampleListView${index - 1}" class="table simple" style="margin: 0 0; width: 100%;">
            <thead>
            <tr>
                <th width="10">
                    <input type="checkbox" class="sample-checkAll${index - 1}"/><span id="count"
                                                                                      class="sample-checkedCount${index - 1}"></span>
                </th>
                <th>Sample</th>
                <th>Initial Stock Volume</th>
                <th>BSP Initial Pico</th>
                <th>BSP Export Quant</th>
                <th>Pond Pico</th>
                <th>Catch Pico</th>
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
                <c:forEach items="${vessel.sampleInstances}" var="sample">
                    <tr>
                        <td>
                            <stripes:checkbox class="sample-checkbox${index - 1}" name="selectedSamples"
                                              value="${sample.startingSample.sampleKey}"/>
                        </td>
                        <td><stripes:link
                                beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.SampleSearchActionBean"
                                event="sampleSearch">
                            <stripes:param name="searchKey" value="${sample.startingSample.sampleKey}"/>
                            ${sample.startingSample.sampleKey}
                        </stripes:link></td>
                        <td> ${bean.sampleToBspPicoValueMap.get(sample.startingSample.sampleKey).volume}</td>
                        <td class="initialPico">${bean.sampleToBspPicoValueMap.get(sample.startingSample.sampleKey).concentration}</td>
                        <td class="exportPico"> ${bean.getExportedSampleConcentration(vessel)}</td>
                        <td class="pondPico"> ${vessel.metricsForVesselAndDescendants.get("Pond Pico").value} </td>
                        <td class="catchPico"> ${vessel.metricsForVesselAndDescendants.get("Catch Pico").value} </td>
                        <td class="ecoQPCR"> ${vessel.metricsForVesselAndDescendants.get("ECO QPCR").value} </td>
                        <td> ${bean.getPositionsForEvent(vessel, "SAMPLE_IMPORT")}</td>
                        <td> ${bean.getPositionsForEvent(vessel, "SHEARING_TRANSFER")}</td>
                        <td> ${bean.getPositionsForEvent(vessel, "POND_REGISTRATION")}</td>
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
    </stripes:form>
</stripes:layout-definition>
