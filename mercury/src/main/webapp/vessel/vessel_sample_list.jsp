<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%--@elvariable id="vessel" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel"--%>
<%--@elvariable id="index" type="java.lang.Integer"--%>

<stripes:layout-definition>
    <script type="text/javascript">
        $j(document).ready(function () {
            var resultsId = "#vesselSampleListView${index}";
            $j(resultsId).dataTable({
                "oTableTools":ttExportDefines,
                "aaSorting":[
                    [2, 'asc']
                ],
                "aoColumns":[
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true, "sType":"html"}
                ],
                "sDom":""
            })
        });
    </script>

    <table id="vesselSampleListView${index}" class="table simple">
        <thead>
        <tr>
            <th width="150">Sample</th>
            <th width="150">Index</th>
            <th width="150">Position</th>
            <th width="150">JIRAs + PDOs</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${vessel.sampleInstances}" var="sample">
            <tr>
                <td>
                        ${sample.startingSample.sampleKey}
                </td>
                <td style="padding: 0;">
                    <table style="padding: 0; border: none;">
                        <c:forEach items="${sample.indexes}" var="curIndex">
                            <c:forEach items="${curIndex.molecularIndexingScheme.indexes}" var="innerIndex">
                                <tr>
                                    <td style="border: none">
                                            ${innerIndex.value.sequence}
                                    </td>
                                </tr>
                            </c:forEach>
                        </c:forEach>
                    </table>
                </td>
                <td>
                    <c:forEach items="${vessel.getPositionsOfSample(sample)}" var="position">
                        ${position}
                    </c:forEach>
                </td>
                <td style="padding: 0;">
                    <table style="padding: 0">
                        <c:forEach items="${sample.allWorkflowLabBatches}" var="batch">

                            <tr>
                                <td>
                                        ${batch.businessKey}
                                </td>
                                <td>
                                    PDO
                                </td>
                            </tr>
                        </c:forEach>
                    </table>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</stripes:layout-definition>
