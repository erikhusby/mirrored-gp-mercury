<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.entity.Metadata.Key" %>


<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean"/>

<c:set var="session" value="${actionBean.selectedSession}"/>
<stripes:layout-render name="/layout.jsp"
                       pageTitle="${session.researchProject.businessKey}: COVID Sample Accessioning: ${session.sessionName}"
                       sectionTitle="${session.researchProject.businessKey}: COVID Sample Accessioning: ${session.sessionName}"
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
                        {"bSortable": true}, // Patient ID
                        {"bSortable": true}, // Requesting Physician
                        {"bSortable": true, "sType": "date"}, // collection Date
                        {"bSortable": true}, // Institution
                        {"bSortable": true}, // Collaborator
                    ]
                }).fnSetFilteringDelay(300);
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <c:set var="patientIdKey" value="<%= Key.PATIENT_ID %>"/>
        <c:set var="physicianKey" value="<%= Key.REQUESTING_PHYSICIAN%>"/>
        <c:set var="sampleIdKey" value="<%= Key.SAMPLE_ID %>"/>
        <c:set var="collectionDateKey" value="<%= Key.COLLECTION_DATE%>"/>
        <c:set var="instituteKey" value="<%= Key.INSTITUTE_ID%>"/>

        <table id="sampleList" class="table simple">
            <thead>
            <tr>
                <th width="20px">Has Error?</th>
                <th Width="50px">Manifest Row Number</th>
                <th width="75px">${patientIdKey.displayName}</th>
                <th width="50px">${physicianKey.displayName}</th>
                <th width="100px">${collectionDateKey.displayName}</th>
                <th width="50px">${instituteKey.displayName}</th>
                <th width="120px">${sampleIdKey.displayName}</th>
            </tr>
            </thead>

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
                            ${record.getValueByKey(patientIdKey)}
                    </td>
                    <td>
                            ${record.getValueByKey(physicianKey)}
                    </td>
                    <td>
                            ${record.getValueByKey(collectionDateKey)}
                    </td>
                    <td>
                            ${record.getValueByKey(instituteKey)}
                    </td>
                    <td>
                            ${record.getValueByKey(sampleIdKey)}
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
