<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.entity.Metadata.Key" %>


<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean"/>

<c:set var="session" value="${actionBean.selectedSession}"/>
<stripes:layout-render name="/layout.jsp"
                       pageTitle="${session.researchProject.businessKey}: Buick Sample Accessioning: ${session.sessionName}"
                       sectionTitle="${session.researchProject.businessKey}: Buick Sample Accessioning: ${session.sessionName}"
                       showCreate="false">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

            $j(document).ready(function () {
                $j("div[name='quarantinedIndicator']").parentsUntil("tbody").addClass("error");

                $j('#sampleList').dataTable({
                    "oTableTools": ttExportDefines,
                    "aaSorting": [
                        [1, 'asc']
                    ],
                    "asStripeClasses": [ '' ],
                    "aoColumns": [
                        {"bSortable": true}, // Error Indicator
                        {"bSortable": true}, // Spreadsheet row number
                        {"bSortable": true}, // Sample ID
                        {"bSortable": true}, // Patient ID
                        {"bSortable": true}, // Gender
                        {"bSortable": true}, // Tumor/Normal
                        {"bSortable": true, "sType": "date"}, // collection Date
                        {"bSortable": true} // Visit
                    ]
                }).fnSetFilteringDelay(300);
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <table id="sampleList" class="table simple">
            <thead>
            <tr>
                <th width="20px">Has Error?</th>
                <th Width="50px">Manifest Row Number</th>
                <th width="120px">Sample ID</th>
                <th width="75px">Patient ID</th>
                <th width="50px">Gender</th>
                <th width="50px">Tumor/Normal</th>
                <th width="100px">Collection Date</th>
                <th width="100px">Visit</th>
            </tr>
            </thead>
            <c:set var="sampleIdKey" value="<%= Key.SAMPLE_ID %>"/>
            <c:set var="patientIdKey" value="<%= Key.PATIENT_ID %>"/>
            <c:set var="genderKey" value="<%= Key.GENDER%>"/>
            <c:set var="tumorNormalKey" value="<%= Key.TUMOR_NORMAL%>"/>
            <c:set var="collectionDateKey" value="<%= Key.BUICK_COLLECTION_DATE%>"/>
            <c:set var="visitKey" value="<%= Key.BUICK_VISIT%>"/>

            <tbody>
            <c:forEach items="${session.records}" var="record">
                <tr>
                    <td>
                        <c:if test="${record.quarantined}">
                            <div name="quarantinedIndicator">X</div>
                        </c:if>
                    </td>
                    <td>
                            ${record.spreadsheetRowNumber}
                    </td>
                    <td>
                            ${record.getValueByKey(sampleIdKey)}
                    </td>
                    <td>
                            ${record.getValueByKey(patientIdKey)}
                    </td>
                    <td>
                            ${record.getValueByKey(genderKey)}
                    </td>
                    <td>
                            ${record.getValueByKey(tumorNormalKey)}
                    </td>
                    <td>
                            ${record.getValueByKey(collectionDateKey)}
                    </td>
                    <td>
                            ${record.getValueByKey(visitKey)}
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>

        <div id="acceptUpload">
            <stripes:form name="acceptUploadForm" id="acceptUploadForm" beanclass="${actionBean.class.name}">
                <stripes:hidden name="selectedSessionId" value="${session.manifestSessionId}"/>
                <div class="actionButtons">
                    <stripes:submit name="acceptUpload" value="Accept Upload" class="btn"/>
                    <stripes:link beanclass="${actionBean.class.name}">
                        Exit Session
                    </stripes:link>
                </div>
            </stripes:form>
        </div>
    </stripes:layout-component>
</stripes:layout-render>