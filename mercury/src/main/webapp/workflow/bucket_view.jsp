<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Bucket View" sectionTitle="Select Bucket">
<stripes:layout-component name="extraHead">
    <style type="text/css">
        .tdfield {
            width: 300px;
            height: 15px;
            text-overflow: ellipsis;
            white-space: nowrap;
            overflow: hidden;
        }
    </style>
    <script type="text/javascript">
        function submitBucket() {
            $j('#bucketForm').submit();
            showJiraInfo();
        }

        $(document).ready(function () {
            $j('#bucketEntryView').dataTable({
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
                    {"bSortable":true, "sType":"date"}
                ]
            });

            $j('.bucket-checkbox').enableCheckboxRangeSelection({
                checkAllClass:'bucket-checkAll',
                countDisplayClass:'bucket-checkedCount',
                checkboxClass:'bucket-checkbox'});

            $j('#reworkEntryView').dataTable({
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
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true, "sType":"date"}
                ]
            });


            $j('.rework-checkbox').enableCheckboxRangeSelection({
                checkAllClass:'rework-checkAll',
                countDisplayClass:'rework-checkedCount',
                checkboxClass:'rework-checkbox'});

            $j("#dueDate").datepicker();
        });

        function showJiraInfo() {
            $j('#jiraTable').show();
        }
    </script>
</stripes:layout-component>

<stripes:layout-component name="content">
<stripes:form id="bucketForm" class="form-horizontal"
              action="/view/bucketView.action?viewBucket">
    <div class="form-horizontal">
        <div class="control-group">
            <stripes:label for="bucketselect" name="Select Bucket" class="control-label"/>
            <div class="controls">
                <stripes:select id="bucketSelect" name="selectedBucket" onchange="submitBucket()">
                    <stripes:options-collection collection="${actionBean.buckets}" label="name"
                                                value="name"/>
                </stripes:select>
            </div>
        </div>
    </div>
</stripes:form>
<stripes:form beanclass="${actionBean.class.name}"
              id="bucketEntryForm" class="form-horizontal">
<div class="form-horizontal">
<stripes:hidden name="selectedBucket" value="${actionBean.selectedBucket}"/>
<stripes:hidden name="selectedProductWorkflowDef" value="${actionBean.selectedProductWorkflowDef}"/>
<c:if test="${actionBean.jiraEnabled}">
    <div id="newTicketDiv">
        <div class="control-group">
            <stripes:label for="workflowSelect" name="Select Workflow" class="control-label"/>
            <div class="controls">
                <stripes:select id="workflowSelect" name="selectedProductWorkflowDef">
                    <stripes:options-collection collection="${actionBean.allProductWorkflowDefs}" label="name"
                                                value="name"/>
                </stripes:select>
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
    <div class="control-group">
        <div class="controls">
            <stripes:submit name="createBatch" value="Create Batch" class="btn btn-primary"/>
        </div>
    </div>
    </div>
</c:if>
<div class="borderHeader"><h4>Samples</h4></div>
<br/>
<table id="bucketEntryView" class="table simple">
    <thead>
    <tr>
        <th width="10">
            <input type="checkbox" class="bucket-checkAll"/><span id="count"
                                                                  class="bucket-checkedCount"></span>
        </th>
        <th width="60">Vessel Name</th>
        <th width="50">Sample Name</th>
        <th width="50">PDO</th>
        <th width="300">PDO Name</th>
        <th width="200">PDO Owner</th>
        <th>Batch Name</th>
        <th width="100">Created Date</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${actionBean.bucketEntries}" var="entry">
        <tr>
            <td>
                <stripes:checkbox class="bucket-checkbox" name="selectedVesselLabels"
                                  value="${entry.labVessel.label}"/>
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
            <td>

                    ${entry.poBusinessKey}
            </td>
            <td>
                <div class="tdfield">${actionBean.getPDODetails(entry.poBusinessKey).title}</div>
            </td>
            <td>
                    ${actionBean.getUserFullName(actionBean.getPDODetails(entry.poBusinessKey).createdBy)}
            </td>
            <td>
                <c:forEach items="${entry.labVessel.nearestWorkflowLabBatches}" var="batch"
                           varStatus="stat">

                    ${batch.businessKey}
                    <c:if test="${!stat.last}">&nbsp;</c:if></c:forEach>

            </td>
            <td>
                <fmt:formatDate value="${entry.createdDate}" pattern="MM/dd/yyyy HH:mm:ss"/>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>

<div class="borderHeader"><h4>Samples for Rework</h4></div>
<br/>
<table id="reworkEntryView" class="table simple">
    <thead>
    <tr>
        <th width="10">
            <input type="checkbox" class="rework-checkAll"/>
        </th>
        <th width="60">Vessel Name</th>
        <th width="50">Sample Name</th>
        <th width="50">PDO</th>
        <th width="300">PDO Name</th>
        <th width="200">PDO Owner</th>
        <th>Batch Name</th>
        <th>Rework Reason</th>
        <th>Rework Comment</th>
        <th>Rework User</th>
        <th>Rework Date</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${actionBean.reworkEntries}" var="reworkVessel">
        <tr>
            <td>
                <stripes:checkbox class="bucket-checkbox" name="selectedReworks"
                                  value="${reworkVessel.labVessel.label}"/>
            </td>
            <td>

                <a href="${ctxpath}/search/vessel.action?vesselSearch=&searchKey=${reworkVessel.labVessel.label}">
                        ${reworkVessel.labVessel.label}
                </a>
            </td>
            <td>
                <c:forEach items="${actionBean.getSampleNames(reworkVessel.labVessel)}" var="sampleName"
                           varStatus="loopstatus">

                    <a href="${ctxpath}/search/sample.action?sampleSearch=&searchKey=${sampleName}"> ${sampleName} </a>
                    <c:if test="${!loopstatus.last}">, </c:if>
                </c:forEach>
            </td>
            <td>
                    ${actionBean.getSinglePDOBusinessKey(reworkVessel.labVessel)}
            </td>
            <td>
                <div class="tdfield">${actionBean.getPDODetails(actionBean.getSinglePDOBusinessKey(reworkVessel.labVessel)).title}</div>
            </td>
            <td>
                    ${actionBean.getUserFullName(actionBean.getPDODetails(actionBean.getSinglePDOBusinessKey(reworkVessel.labVessel)).createdBy)}
            </td>
            <td>
                <c:forEach items="${reworkVessel.labVessel.nearestWorkflowLabBatches}" var="batch"
                           varStatus="stat">
                    ${batch.businessKey}
                    <c:if test="${!stat.last}">&nbsp;</c:if>
                </c:forEach>

            </td>
            <td>
                ${reworkVessel.reworkDetail.reworkReason.value}
            </td>
            <td>
                ${reworkVessel.reworkDetail.comment}
            </td>
            <td>
                    ${actionBean.getUserFullName(actionBean.getReworkOperator(reworkVessel))}
            </td>
            <td>
                <fmt:formatDate value="${actionBean.getReworkLogDate(reworkVessel)}" pattern="MM/dd/yyyy HH:mm:ss"/>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>
</stripes:form>
</stripes:layout-component>
</stripes:layout-render>
