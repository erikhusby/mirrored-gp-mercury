<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%--@elvariable id="plateWells" type="java.util.List"--%>
<%--@elvariable id="barcode" type="java.lang.String"--%>
<%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean"--%>
<%--@elvariable id="index" type="java.lang.Integer"--%>
<stripes:layout-definition>
    <script type="text/javascript">
        $j(document).ready(function () {
            $j(".accordion").on("accordionactivate", function (event, ui) {
                var active = $j('.accordion').accordion('option', 'active');
            });
        });
    </script>
    <table id="vesselSampleListView${index - 1}" class="table simple" style="margin: 0 0; width: 100%;">
        <thead>
        <tr>
            <th>Well</th>
            <th>Collaborator Sample ID</th>
            <th>Collaborator Participant ID</th>
            <th>Cell Type</th>
            <th>Cells Per Well</th>
            <th>Volume</th>
            <th>Collection Date</th>
            <th>Positive Control?</th>
            <th>Negative Control?</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${plateWells}" var="row" varStatus="rowStatus">
            <%--@elvariable id="row" type="org.broadinstitute.gpinformatics.mercury.boundary.vessel.RowMetadata"--%>
            <c:set var="collabSampleIdValueIndex" value="${row.findByKeyName('Sample ID')}"/>
            <c:set var="participantIdValueIndex" value="${row.findByKeyName('Patient ID')}"/>
            <c:set var="cellTypeValueIndex" value="${row.findByKeyName('Cell Type')}"/>
            <c:set var="cellPerWellValueIndex" value="${row.findByKeyName('Cells Per Well')}"/>
            <c:set var="collectionDateValueIndex" value="${row.findByKeyName('Collection Date')}"/>
            <c:set var="speciesValueIndex" value="${row.findByKeyName('Species')}"/>
            <c:set var="positiveControlValueIndex" value="${row.findByKeyName('Positive Control')}"/>
            <c:set var="negativeControlValueIndex" value="${row.findByKeyName('Negative Control')}"/>
            <tr>
                <stripes:hidden name="uploadedPlates[${barcode}][${rowStatus.index}].metadataBeans[${speciesValueIndex.left}].value"
                                value="${speciesValueIndex.right.value}"/>
                <stripes:hidden name="uploadedPlates[${barcode}][${rowStatus.index}].metadataBeans[${speciesValueIndex.left}].name"
                                value="${speciesValueIndex.right.key.displayName}"/>

                <td>
                    <stripes:hidden name="uploadedPlates[${barcode}][${rowStatus.index}].well" value="${row.well}"/>
                    ${row.well}
                </td>
                <td>
                    <stripes:hidden name="uploadedPlates[${barcode}][${rowStatus.index}].metadataBeans[${collabSampleIdValueIndex.left}].value"
                                    value="${collabSampleIdValueIndex.right.value}"/>
                    <stripes:hidden name="uploadedPlates[${barcode}][${rowStatus.index}].metadataBeans[${collabSampleIdValueIndex.left}].name"
                                    value="${collabSampleIdValueIndex.right.key.displayName}"/>
                        ${collabSampleIdValueIndex.right.value}
                </td>
                <td>
                    <stripes:hidden name="uploadedPlates[${barcode}][${rowStatus.index}].metadataBeans[${participantIdValueIndex.left}].value"
                                    value="${participantIdValueIndex.right.value}"/>
                    <stripes:hidden name="uploadedPlates[${barcode}][${rowStatus.index}].metadataBeans[${participantIdValueIndex.left}].name"
                                    value="${participantIdValueIndex.right.key.displayName}"/>
                        ${participantIdValueIndex.right.value}
                </td>
                <td>
                    <stripes:hidden name="uploadedPlates[${barcode}][${rowStatus.index}].metadataBeans[${cellTypeValueIndex.left}].value"
                                    value="${cellTypeValueIndex.right.value}"/>
                    <stripes:hidden name="uploadedPlates[${barcode}][${rowStatus.index}].metadataBeans[${cellTypeValueIndex.left}].name"
                                    value="${cellTypeValueIndex.right.key.displayName}"/>
                    ${cellTypeValueIndex.right.value}
                </td>
                </td>
                <td>
                    <stripes:hidden name="uploadedPlates[${barcode}][${rowStatus.index}].metadataBeans[${cellPerWellValueIndex.left}].value"
                                    value="${cellPerWellValueIndex.right.value}"/>
                    <stripes:hidden name="uploadedPlates[${barcode}][${rowStatus.index}].metadataBeans[${cellPerWellValueIndex.left}].name"
                                    value="${cellPerWellValueIndex.right.key.displayName}"/>
                    ${cellPerWellValueIndex.right.value}
                </td>
                <td>
                    <stripes:hidden name="uploadedPlates[${barcode}][${rowStatus.index}].volume"
                                    value="${cellPerWellValueIndex.right.value}"/>
                    ${row.volume}
                </td>
                <td>
                    <stripes:hidden name="uploadedPlates[${barcode}][${rowStatus.index}].metadataBeans[${collectionDateValueIndex.left}].value"
                                    value="${collectionDateValueIndex.right.value}"/>
                    <stripes:hidden name="uploadedPlates[${barcode}][${rowStatus.index}].metadataBeans[${collectionDateValueIndex.left}].name"
                                    value="${collectionDateValueIndex.right.key.displayName}"/>
                    ${collectionDateValueIndex.right.value}
                </td>
                <td>
                    <stripes:hidden name="uploadedPlates[${barcode}][${rowStatus.index}].metadataBeans[${positiveControlValueIndex.left}].value"
                                    value="${positiveControlValueIndex.right.value}"/>
                    <stripes:hidden name="uploadedPlates[${barcode}][${rowStatus.index}].metadataBeans[${positiveControlValueIndex.left}].name"
                                    value="${positiveControlValueIndex.right.key.displayName}"/>
                        ${positiveControlValueIndex.right.value}
                </td>
                <td>
                    <stripes:hidden name="uploadedPlates[${barcode}][${rowStatus.index}].metadataBeans[${negativeControlValueIndex.left}].value"
                                    value="${negativeControlValueIndex.right.value}"/>
                    <stripes:hidden name="uploadedPlates[${barcode}][${rowStatus.index}].metadataBeans[${negativeControlValueIndex.left}].name"
                                    value="${negativeControlValueIndex.right.key.displayName}"/>
                        ${negativeControlValueIndex.right.value}
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</stripes:layout-definition>
