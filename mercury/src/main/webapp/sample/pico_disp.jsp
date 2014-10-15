<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.PicoDispositionActionBean" />

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.pageTitle}" sectionTitle="${actionBean.pageTitle}" showCreate="false">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {

                $j('#dispositions').dataTable({
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[3,'asc']],
                    "aoColumns": [
                        {"bSortable": true, "sType": "html"},             // position
                        {"bSortable": true, "sType": "html"},             // barcode
                        {"bSortable": true, "sType": "html"},             // collaborator sample ID
                        {"bSortable": true, "sType": "numeric"},          // concentration
                        {"bSortable": true, "sType": "title-numeric"}     // next step
                    ]
                })
            });
        </script>

    </stripes:layout-component>

    <stripes:layout-component name="content">
        <div class="control-group" style="float:right">
            <div class="control-label">&#160;</div>
            <div class="controls actionButtons">
                <stripes:form beanclass="${actionBean.class.name}" id="scanRackForm">
                    <stripes:submit name="reviewScannedRack" value="Review Scanned Rack" class="btn btn-mini"/>
                </stripes:form>
                <stripes:form beanclass="${actionBean.class.name}" id="confirmRearrayForm">
                    <stripes:submit name="confirmRearray" value="Confirm Rearray" class="btn btn-mini"/>
                </stripes:form>
            </div>
        </div>

        <div class="clearfix"></div>
        <table class="table simple" id="dispositions">
        <thead>
            <tr>
                <th class="columnPosition">Position</th>
                <th class="columnBarcode">Barcode</th>
                <th class="columnCollabSample">Collaborator Sample ID</th>
                <th class="columnConcentration">Concentration</th>
                <th class="columnNextStep">Next Step</th>
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
                    <td class="columnCollabSample">
                            ${fn:join(listItem.collaboratorSampleIds, " ")}
                    </td>
                    <td class="columnConcentration">
                            ${listItem.concentration}
                    </td>
                    <td class="columnNextStep">
                        <div title="${listItem.disposition.sortOrder}">
                            ${listItem.disposition.stepName}
                        </div>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>

    </stripes:layout-component>
</stripes:layout-render>
