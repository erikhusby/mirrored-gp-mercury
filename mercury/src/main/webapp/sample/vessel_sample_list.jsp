<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<script type="text/javascript">

    $j(document).ready(function () {
        $j('#vesselSampleListView').dataTable({
            "oTableTools":ttExportDefines,
            "aaSorting":[
                [1, 'asc']
            ],
            "aoColumns":[
                {"bSortable":false},
                {"bSortable":true},
                {"bSortable":true},
                {"bSortable":true},
                {"bSortable":true},
                {"bSortable":true},
                {"bSortable":true},
                {"bSortable":true}
            ]
        })
    });

    function showPlasticHistoryVisualizer(sampleKey) {
        $j('#plasticViewDiv').load('${ctxpath}/view/plasticHistoryView.action?sampleKey=' + sampleKey);
        $j('#plasticViewDiv').show();
    }
</script>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.VesselSampleListViewActionBean"/>

<table id="vesselSampleListView" class="table simple">
    <thead>
    <tr>
        <th width="30">Vessel History</th>
        <th width="50">Vessel Position</th>
        <th width="200">Label</th>
        <th width="50">Type</th>
        <th>Sample Name</th>
        <th width="150">PDO</th>
        <th width="200">Index Info</th>
        <th>Reagent Info</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${actionBean.vessel.vesselGeometry.positionNames}" var="position">
        <c:forEach items="${actionBean.getSampleInstancesAtPosition(position)}" var="sample">
            <tr>
                <td>
                    <a href="javascript:showPlasticHistoryVisualizer('${sample.startingSample.sampleKey}')">
                        <img width="30" height="30" name="" title="show plastic history view"
                             src="${ctxpath}/images/plate.png"/>
                    </a>
                </td>
                <td>
                        ${position}
                </td>
                <td>
                    <a href="${ctxpath}/search/all.action?search=&searchKey=${actionBean.getVesselAtPosition(position).label}">
                            ${actionBean.getVesselAtPosition(position).label}
                    </a>
                </td>
                <td>
                        ${actionBean.getVesselAtPosition(position).type.name}
                </td>
                <td>
                    <a href="${ctxpath}/search/all.action?search=&searchKey=${sample.startingSample.sampleKey}">
                            ${sample.startingSample.sampleKey}
                    </a>

                </td>
                <td>
                    <a href="${ctxpath}/search/all.action?search=&searchKey=${sample.productOrderKey}">
                            ${sample.productOrderKey}
                    </a>
                </td>
                <td>
                        ${actionBean.getIndexValueForSample(sample)}
                </td>
                <td>
                        ${actionBean.getReagentInfoForSample(sample)}
                </td>
            </tr>
        </c:forEach>
    </c:forEach>
    </tbody>
</table>
<div id="plasticViewDiv" style="display:none"></div>

