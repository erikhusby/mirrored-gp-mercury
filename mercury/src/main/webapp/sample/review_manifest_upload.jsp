<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.entity.Metadata.Key" %>


<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="" sectionTitle="" showCreate="false">

    <stripes:layout-component name="extraHead">
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <c:set var="session" value="${actionBean.selectedSession}" />
        <div>${session.researchProject.businessKey}: Buick Sample Accessioning: ${session.sessionName}</div>

        <table id="sampleList" class="table simple">
            <thead>
            <tr>
                <th></th>
                <th>Sample ID</th>
                <th>Patient ID</th>
                <th>Gender</th>
                <th>Tumor/Normal</th>
                <th>Collection Date</th>
                <th>Visit</th>
            </tr>
            </thead>
            <c:set var="sampleIdKey" value="<%= Key.SAMPLE_ID %>" />
            <c:set var="patientIdKey" value="<%= Key.PATIENT_ID %>" />
            <c:set var="genderKey" value="<%= Key.GENDER%>" />
            <c:set var="tumorNormalKey" value="<%= Key.TUMOR_NORMAL%>" />
            <c:set var="collectionDateKey" value="<%= Key.BUICK_COLLECTION_DATE%>" />
            <c:set var="visitKey" value="<%= Key.BUICK_VISIT%>" />

            <tbody>
            <c:forEach items="${session.records}" var="record">
                <tr>
                    <td>
                        <c:if test="${record.quarantined}">X</c:if>
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
                <stripes:hidden name="selectedSessionId" value="${session.manifestSessionId}" />
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