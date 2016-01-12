<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.LabManager" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.PDM" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.PM" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.Developer" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>

<%--
  ~ The Broad Institute
  ~ SOFTWARE COPYRIGHT NOTICE AGREEMENT
  ~ This software and its documentation are copyright 2015 by the
  ~ Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support
  ~ whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  ~ use, misuse, or functionality.
  --%>


<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean"--%>
<%--@elvariable id="tableId" type="java.lang.String" --%>
<%--@elvariable id="formsEnabled" type="java.lang.Boolean" --%>
<c:set var="tableId" value="bucketEntryView"/>
<c:set var="formsEnabled" value="true"/>
<stripes:layout-definition>
    <style type="text/css">
        td.editable {
            width: 105px !important;
            height: 78px;
        }
        .editable select {
            width: auto !important;
        }

    </style>
    <script src="${ctxpath}/resources/scripts/jquery.jeditable.mini.js" type="text/javascript"></script>
    <script type="text/javascript">
        var formsEnabled=${formsEnabled}

        $j(document).ready(function () {
            oTable = $j('#${tableId}').dataTable({
                "oTableTools":ttExportDefines,
                "aaSorting": [[1,'asc'], [7,'asc']],
                "aoColumns":[
                    {"bSortable":false},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true, "sType":"date"},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true}
                ]
            });
            if (formsEnabled) {
                oTable.fnDrawCallback = editablePdo();
            }
            includeAdvancedFilter(oTable, "#${tableId}");

            $j('.bucket-checkbox').enableCheckboxRangeSelection({
                checkAllClass: 'bucket-checkAll',
                countDisplayClass: 'bucket-checkedCount',
                checkboxClass: 'bucket-checkbox'
            });
        });

        function editablePdo() {
            var columnsEditable = false;
            <security:authorizeBlock roles="<%= roles(LabManager, PDM, PM, Developer) %>">
            columnsEditable = true;
            </security:authorizeBlock>

            if (columnsEditable) {
                var oTable = $j('#bucketEntryView').dataTable();
                $j("td.editable").editable('${ctxpath}/workflow/bucketView.action?changePdo', {
                    'loadurl': '${ctxpath}/workflow/bucketView.action?findPdo',
                    'callback': function (sValue, y) {
                        var jsonValues = $j.parseJSON(sValue);
                        var pdoKeyCellValue = '<span class="ellipsis">' + jsonValues.jiraKey + '</span><span style="display: none;" class="icon-pencil"></span>';

                        var aPos = oTable.fnGetPosition(this);
                        oTable.fnUpdate(pdoKeyCellValue, aPos[0] /*row*/, aPos[1]/*column*/);

                        var pdoTitleCellValue = '<div class="ellipsis" style="width: 300px">' + jsonValues.pdoTitle + '</div>';
                        oTable.fnUpdate(pdoTitleCellValue, aPos[0] /*row*/, aPos[1] + 1/*column*/);

                        var pdoCreatorCellValue = jsonValues.pdoOwner;
                        oTable.fnUpdate(pdoCreatorCellValue, aPos[0] /*row*/, aPos[1] + 2/*column*/);
                    },
                    'submitdata': function (value, settings) {
                        return {
                            "selectedEntryIds": this.parentNode.getAttribute('id'),
                            "column": oTable.fnGetPosition(this)[2],
                            "newPdoValue": $j(this).find(':selected').text()
                        };
                    },
                    'loaddata': function (value, settings) {
                        return {
                            "selectedEntryIds": this.parentNode.getAttribute('id')
                        };
                    },
//                        If you need to debug the generated html you need to ignore onblur events
                    "onblur": "ignore",
                    cssclass: "editable",
                    tooltip: 'Click the value in this field to edit',
                    type: "select",
                    indicator: '<img src="${ctxpath}/images/spinner.gif">',
                    submit: 'Save',
                    cancel: 'Cancel',
                    height: "auto",
                    width: "auto"
                });
                $j(".icon-pencil").show();
            } else {
                $j(".icon-pencil").hide();
                $j(".editable").removeClass("editable")
            }
        }
    </script>

    <table id="${tableId}" class="bucket-checkbox table simple">
                <thead>
                <tr>
                    <c:if test="${formsEnabled}">
                        <th width="10">
                            <input type="checkbox" class="bucket-checkAll"/>
                            <span id="count" class="bucket-checkedCount"></span>
                        </th>
                    </c:if>                    <th width="60">Vessel Name</th>
                    <th width="50">Sample Name</th>
                    <th>Material Type</th>
                    <th>PDO</th>
                    <th width="300">PDO Name</th>
                    <th width="200">PDO Owner</th>
                    <th>Batch Name</th>
                    <th>Workflow</th>
                    <th>Product</th>
                    <th>Add-ons</th>
                    <th width="100">Created Date</th>
                    <th>Bucket Entry Type</th>
                    <th>Rework Reason</th>
                    <th>Rework Comment</th>
                    <th>Rework User</th>
                    <th>Rework Date</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.collectiveEntries}" var="entry">
                    <tr id="${entry.bucketEntryId}" data-vessel-label="${entry.labVessel.label}">
                        <c:if test="${formsEnabled}">
                            <td>
                                <stripes:checkbox class="bucket-checkbox" name="selectedEntryIds"
                                                  value="${entry.bucketEntryId}"/>
                            </td>
                        </c:if>
                        <td>
                            <a href="${ctxpath}/search/vessel.action?vesselSearch=&searchKey=${entry.labVessel.label}">
                                    ${entry.labVessel.label}</a>
                        </td>

                        <td>
                            <c:forEach items="${entry.labVessel.mercurySamples}"
                                       var="mercurySample"
                                       varStatus="stat">
                                <a href="${ctxpath}/search/sample.action?sampleSearch=&searchKey=${mercurySample.sampleKey}">
                                        ${mercurySample.sampleKey}
                                </a>

                                <c:if test="${!stat.last}">&nbsp;</c:if>
                            </c:forEach>
                        </td>
                        <td class="ellipsis">
                            ${entry.labVessel.latestMaterialType.displayName}
                        </td>
                        <td class="editable"><span class="ellipsis">${entry.productOrder.businessKey}</span><span style="display: none;"
                                                                                               class="icon-pencil"></span>
                        </td>
                        <td>
                            <div class="ellipsis" style="width: 300px">${entry.productOrder.title}</div>
                        </td>
                        <td class="ellipsis">
                                ${actionBean.getUserFullName(entry.productOrder.createdBy)}
                        </td>
                        <td>
                            <c:forEach items="${entry.labVessel.nearestWorkflowLabBatches}" var="batch" varStatus="stat">
                                ${batch.businessKey}
                                <c:if test="${!stat.last}">&nbsp;</c:if>
                            </c:forEach>

                        </td>
                        <td>
                            <div class="ellipsis" style="max-width: 250px;">
                                ${mercuryStatic:join(entry.workflowNames, "<br/>")}
                            </div>
                        </td>
                        <td>
                            <div class="ellipsis" style="max-width: 250px;">${entry.productOrder.product.name}</div>
                        </td>
                        <td>
                            <div class="ellipsis" style="max-width: 250px;">
                                ${entry.productOrder.getAddOnList("<br/>")}
                            </div>
                        </td>
                        <td class="ellipsis">
                            <fmt:formatDate value="${entry.createdDate}" pattern="MM/dd/yyyy HH:mm:ss"/>
                        </td>
                        <td>
                                ${entry.entryType.name}
                        </td>
                        <td>
                                ${entry.reworkDetail.reason.reason}
                        </td>
                        <td>
                                ${entry.reworkDetail.comment}
                        </td>
                        <td>
                            <c:if test="${entry.reworkDetail != null}">
                                ${actionBean.getUserFullName(entry.reworkDetail.addToReworkBucketEvent.eventOperator)}
                            </c:if>
                        </td>
                        <td>
                            <fmt:formatDate value="${entry.reworkDetail.addToReworkBucketEvent.eventDate}"
                                            pattern="MM/dd/yyyy HH:mm:ss"/>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
</stripes:layout-definition>
