<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib uri="http://mercury.broadinstitute.org/Mercury/security" prefix="security" %>
<%@ page import="static org.broadinstitute.gpinformatics.mercury.entity.DB.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.mercury.entity.DB.roles" %>
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
<c:set var="readOnly" value="false"/>
<security:authorizeBlock roles="<%= roles(LabUser, PDM) %>">
    <c:set var="readOnly" value="true"/>
</security:authorizeBlock>
<stripes:form beanclass="${actionBean.class.name}" id="bucketForm">
    <div class="control-group">
        <div class="controls">
            <stripes:select id="bucketSelect" name="selectedBucket" style="margin-bottom: 0;">
                <stripes:options-collection collection="${actionBean.buckets}" label="name"
                                            value="name"/>
            </stripes:select>
            <stripes:submit name="viewBucket" class="btn" value="View Bucket" onclick="javascript:showJiraInfo();"/>
        </div>
    </div>
</stripes:form>
<stripes:form beanclass="${actionBean.class.name}"
              id="bucketEntryForm">
<stripes:hidden name="selectedBucket" value="${actionBean.selectedBucket}"/>
<c:if test="${actionBean.jiraEnabled}">
    <table>
        <tr>
            <td valign="top">

                <div id="newTicketDiv">
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
                    <div class="controls" style="margin-left: 80px;">
                        <stripes:submit name="createBatch" value="Create Batch"/>
                    </div>
                </div>
            </td>
        </tr>
    </table>
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
                <c:choose>
                <c:when test="${!readOnly}">
                <a href="${ctxpath}/search/all.action?search=&searchKey=${entry.labVessel.label}">
                        ${entry.labVessel.label}
                </a></td>
            </c:when>
            <c:otherwise>
                ${entry.labVessel.label}
            </c:otherwise>
            </c:choose>
            <td>
                <c:forEach items="${entry.labVessel.mercurySamples}"
                           var="mercurySample"
                           varStatus="stat">
                    <c:choose>
                        <c:when test="${!readOnly}">
                            <a href="${ctxpath}/search/all.action?search=&searchKey=${mercurySample.sampleKey}">
                                    ${mercurySample.sampleKey}
                            </a>
                        </c:when>
                        <c:otherwise>
                            ${mercurySample.sampleKey}
                        </c:otherwise>
                    </c:choose>
                    <c:if test="${!stat.last}">&nbsp;</c:if>
                </c:forEach>
            </td>
            <td>
                <c:choose>
                    <c:when test="${!readOnly}">
                        <a href="${ctxpath}/search/all.action?search=&searchKey=${entry.poBusinessKey}">
                                ${entry.poBusinessKey}
                        </a>
                    </c:when>
                    <c:otherwise>
                        ${entry.poBusinessKey}
                    </c:otherwise>
                </c:choose>
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
                    <c:choose>
                        <c:when test="${!readOnly}">
                            <a href="${ctxpath}/search/all.action?search=&searchKey=${batch.businessKey}">
                                    ${batch.businessKey}
                            </a>
                        </c:when>
                        <c:otherwise>
                            ${batch.businessKey}
                        </c:otherwise>
                    </c:choose>
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
                                  value="${reworkVessel.label}"/>
            </td>
            <td>
                <c:choose>
                    <c:when test="${!readOnly}">
                        <a href="${ctxpath}/search/all.action?search=&searchKey=${reworkVessel.label}">
                                ${reworkVessel.label}
                        </a>
                    </c:when>
                    <c:otherwise>
                        ${reworkVessel.label}
                    </c:otherwise>
                </c:choose>
            </td>
            <td>
                <c:forEach items="${actionBean.getSampleNames(reworkVessel)}" var="sampleName"
                           varStatus="loopstatus">
                    <c:choose>
                        <c:when test="${!readOnly}">
                            <a href="${ctxpath}/search/all.action?search=&searchKey=${sampleName}"> ${sampleName} </a>
                        </c:when>
                        <c:otherwise>
                            ${sampleName}
                        </c:otherwise>
                    </c:choose>
                    <c:if test="${!loopstatus.last}">, </c:if>
                </c:forEach>
            </td>
            <td>
                <c:choose>
                    <c:when test="${!readOnly}">
                        <a href="${ctxpath}/search/all.action?search=&searchKey=${actionBean.getSinglePDOBusinessKey(reworkVessel)}">
                                ${actionBean.getSinglePDOBusinessKey(reworkVessel)}
                        </a>
                    </c:when>
                    <c:otherwise>
                        ${actionBean.getSinglePDOBusinessKey(reworkVessel)}
                    </c:otherwise>
                </c:choose>
            </td>
            <td>
                <div class="tdfield">${actionBean.getPDODetails(actionBean.getSinglePDOBusinessKey(reworkVessel)).title}</div>
            </td>
            <td>
                    ${actionBean.getUserFullName(actionBean.getPDODetails(actionBean.getSinglePDOBusinessKey(reworkVessel)).createdBy)}
            </td>
            <td>
                <c:forEach items="${reworkVessel.nearestWorkflowLabBatches}" var="batch"
                           varStatus="stat">
                    <c:choose>
                        <c:when test="${!readOnly}">
                            <a href="${ctxpath}/search/all.action?search=&searchKey=${batch.businessKey}">
                                    ${batch.businessKey}
                            </a>
                        </c:when>
                        <c:otherwise>
                            ${actionBean.getSinglePDOBusinessKey(reworkVessel)}
                        </c:otherwise>
                    </c:choose>
                    <c:if test="${!stat.last}">&nbsp;</c:if>
                </c:forEach>

            </td>
            <td>
                    ${actionBean.getReworkReason(reworkVessel)}
            </td>
            <td>
                    ${actionBean.getReworkComment(reworkVessel)}
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
