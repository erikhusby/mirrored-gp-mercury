<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib uri="http://mercury.broadinstitute.org/Mercury/security" prefix="security" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Bucket View" sectionTitle="Select Bucket">
<stripes:layout-component name="extraHead">
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
        function submitBucket() {
            $j('#bucketForm').submit();
            showJiraInfo();
        }

        function submitWorkflow() {
            $j('#bucketWorkflowForm').submit();
            showJiraInfo();
        }

        $j(document).ready(function () {
            var columnsEditable=false;
            <security:authorizeBlock roles="<%= roles(LabManager, PDM, PM, Developer) %>">
                columnsEditable=true;
            </security:authorizeBlock>

                var editablePdo = function()  {
                if (columnsEditable) {
                    var oTable = $j('#bucketEntryView').dataTable();
                    $j("td.editable").editable('${ctxpath}/view/bucketView.action?changePdo', {
                        'loadurl': '${ctxpath}/view/bucketView.action?findPdo',
                        'callback': function (sValue, y) {
                            var cellValue='<span class="ellipsis">'+sValue+'</span><span style="display: none;" class="icon-pencil"></span>';
                            var aPos = oTable.fnGetPosition(this);
                            oTable.fnUpdate(cellValue, aPos[0], aPos[1]);
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
                        "onblur" : "ignore",
                        cssclass: "editable",
                        tooltip: 'Click the value in this field to edit',
                        type: "select",
                        indicator : '<img src="${ctxpath}/images/spinner.gif">',
                        submit: 'Save',
                        height: "auto",
                        width: "auto"
                    });
                    $j(".icon-pencil").show();
                } else {
                    $j(".icon-pencil").hide();
                    $j(".editable").removeClass("editable")
                }

            };

            $j('#bucketEntryView').dataTable({
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
                    {"bSortable":true, "sType":"date"},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true}
                ],
                "fnDrawCallback": editablePdo
            });

            $j('.bucket-checkbox').enableCheckboxRangeSelection({
                checkAllClass:'bucket-checkAll',
                countDisplayClass:'bucket-checkedCount',
                checkboxClass:'bucket-checkbox'});

            $j("#dueDate").datepicker();
        });

        function showJiraInfo() {
            $j('#jiraTable').show();
        }
    </script>
</stripes:layout-component>

<stripes:layout-component name="content">
    <stripes:form id="bucketForm" class="form-horizontal" action="/view/bucketView.action?setBucket">
        <div class="form-horizontal">
            <div class="control-group">
                <stripes:label for="bucketselect" name="Select Bucket" class="control-label"/>
                <div class="controls">
                    <stripes:select id="bucketSelect" name="selectedBucket" onchange="submitBucket()">
                        <stripes:option value="">Select a Bucket</stripes:option>
                        <stripes:options-collection collection="${actionBean.buckets}"/>
                    </stripes:select>
                </div>
            </div>
        </div>
    </stripes:form>
    <stripes:form id="bucketWorkflowForm" class="form-horizontal" action="/view/bucketView.action?viewBucket">
        <div class="form-horizontal">
        <stripes:hidden name="selectedBucket" value="${actionBean.selectedBucket}"/>
        <div class="control-group">
            <stripes:label for="workflowSelect" name="Select Workflow" class="control-label"/>
            <div class="controls">
                <stripes:select id="workflowSelect" name="selectedWorkflowDef" onchange="submitWorkflow()"
                                value="selectedWorkflowDef.name">
                    <stripes:option value="">Select a Workflow</stripes:option>
                    <stripes:options-collection collection="${actionBean.possibleWorkflows}" label="name" value="name"/>
                </stripes:select>
            </div>
        </div>
    </stripes:form>
    <stripes:form beanclass="${actionBean.class.name}"
                  id="bucketEntryForm" class="form-horizontal">
        <div class="form-horizontal">
        <stripes:hidden name="selectedBucket" value="${actionBean.selectedBucket}"/>
        <stripes:hidden name="selectedWorkflowDef" value="${actionBean.selectedWorkflowDef}"/>
        <c:if test="${actionBean.jiraEnabled}">
            <div id="newTicketDiv">
                <div class="control-group">
                    <stripes:label for="lcsetText" name="LCSet Name" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="lcsetText" class="defaultText" name="selectedLcset"
                                      title="Enter if you are adding to a batch"/>
                    </div>
                </div>
                <div class="control-group">
                    <stripes:label for="summary" name="Summary" class="control-label"/>
                    <div class="controls">
                        <stripes:text name="summary" class="defaultText"
                                      title="Enter a summary for a new batch ticket" id="summary"
                                      value="${actionBean.summary}"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="description" name="Description" class="control-label"/>
                    <div class="controls">
                        <stripes:textarea name="description" class="defaultText"
                                          title="Enter a description for a new batch ticket"
                                          id="description" value="${actionBean.description}"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="important" name="Important Information"
                                   class="control-label"/>
                    <div class="controls">
                        <stripes:textarea name="important" class="defaultText"
                                          title="Enter important info for a new batch ticket"
                                          id="important"
                                          value="${actionBean.important}"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="dueDate" name="Due Date" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="dueDate" name="dueDate" class="defaultText"
                                      title="enter date (MM/dd/yyyy)"
                                      value="${actionBean.dueDate}"
                                      formatPattern="MM/dd/yyyy"><fmt:formatDate
                                value="${actionBean.dueDate}"
                                dateStyle="short"/></stripes:text>
                    </div>
                </div>
            </div>
            </div>
        </c:if>
        <div class="borderHeader"><h4>Samples</h4></div>
        <br/>
        <ul><li>If you would like to change the value of a PDO for an item in the bucket, click on the value of the PDO in the table and select the new value.</li></ul>
        <div class="actionButtons">
            <stripes:submit name="createBatch" value="Create Batch" class="btn"/>
            <stripes:submit name="addToBatch" value="Add to Batch" class="btn"/>
            <stripes:submit name="removeFromBucket" value="Remove From Bucket" class="btn"/>
        </div>
        <table id="bucketEntryView" class="table simple">
            <thead>
            <tr>
                <th width="10">
                    <input type="checkbox" class="bucket-checkAll"/>
                    <span id="count" class="bucket-checkedCount"></span>
                </th>
                <th width="60">Vessel Name</th>
                <th width="50">Sample Name</th>
                <th>PDO</th>
                <th width="300">PDO Name</th>
                <th width="200">PDO Owner</th>
                <th>Batch Name</th>
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
                <tr id="${entry.bucketEntryId}">
                    <td>
                        <stripes:checkbox class="bucket-checkbox" name="selectedEntryIds"
                                          value="${entry.bucketEntryId}"/>
                    </td>
                    <td>
                        <a href="${ctxpath}/search/vessel.action?vesselSearch=&searchKey=${entry.labVessel.label}">
                                ${entry.labVessel.label}
                        </a></td>

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
                    <td class="editable"><span class="ellipsis">${entry.poBusinessKey}</span><span style="display: none;"
                                                                                           class="icon-pencil"></span>
                    </td>
                    <td>
                        <div class="ellipsis" style="width: 300px">${actionBean.getPDODetails(entry.poBusinessKey).title}</div>
                    </td>
                    <td class="ellipsis">
                            ${actionBean.getUserFullName(actionBean.getPDODetails(entry.poBusinessKey).createdBy)}
                    </td>
                    <td>
                        <c:forEach items="${entry.labVessel.nearestWorkflowLabBatches}" var="batch"
                                   varStatus="stat">

                            ${batch.businessKey}
                            <c:if test="${!stat.last}">&nbsp;</c:if></c:forEach>

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
    </stripes:form>
</stripes:layout-component>
</stripes:layout-render>
