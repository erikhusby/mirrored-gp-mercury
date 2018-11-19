<%@ taglib prefix="stipes" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.PicoDispositionActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.pageTitle} Confirm Rearray"
                       sectionTitle="Confirm Rearray" showCreate="false">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {

                $j('#dispositions').dataTable({
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[1,'asc']],
                    "aoColumns": [
                        {"bSortable": true, "sType": "html"},             // position
                        {"bSortable": true, "sType": "html"},             // barcode
                        {"bSortable": true, "sType": "html"},             // collaborator patient ID
                        {"bSortable": true, "sType": "numeric"},          // concentration
                        {"bSortable": true, "sType": "title-numeric"},    // expected next step
                        {"bSortable": true, "sType": "title-numeric"}     // actual next step
                    ]
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <div class="control-group" style="float:right">
            <div class="control-label">&#160;</div>
            <div class="controls actionButtons">
                <stripes:form beanclass="${actionBean.class.name}" id="confirmRearrayForm">
                    <stripes:submit name="confirmRearray" value="Confirm Rearray" class="btn btn-mini"/>
                </stripes:form>
            </div>
        </div>

        <div>
            <c:choose>
                <c:when test="${actionBean.listItems.isEmpty()}">
                    <h4>PASS</h4>
                    <p>All tubes go to ${actionBean.nextStepSelect}.</p>
                </c:when>
                <c:otherwise>
                    <h4>FAIL</h4>
                    <p>The problem tubes are shown below.</p>

                    <div class="clearfix"></div>
                    <table class="table simple" id="dispositions">
                        <thead>
                        <tr>
                            <th class="columnPosition">Position</th>
                            <th class="columnBarcode">Barcode</th>
                            <th class="columnCollabPatient">Collaborator Patient ID</th>
                            <th class="columnConcentration">Concentration</th>
                            <th class="columnExpectedNextStep">Expected Next Step</th>
                            <th class="columnActualNextStep">Actual Next Step</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${actionBean.listItems}" var="listItem">
                            <tr>
                                <td class="columnPosition">
                                        ${listItem.position}
                                </td>
                                <td class="columnBarcode">
                                        ${listItem.barcode}
                                </td>
                                <td class="columnCollabPatient">
                                        ${fn:join(listItem.collaboratorPatientIds, " ")}
                                </td>
                                <td class="columnConcentration">
                                        ${listItem.concentration}
                                </td>
                                <td class="columnExpectedNextStep">
                                    <div title="${listItem.disposition.sortOrder}">
                                        ${actionBean.nextStepSelect}
                                    </div>
                                </td>
                                <td class="columnActualNextStep">
                                    <div title="${listItem.disposition.sortOrder}">
                                        ${listItem.disposition.stepName}
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>

    </stripes:layout-component>
</stripes:layout-render>
