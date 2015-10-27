<%@ include file="/resources/layout/taglibs.jsp" %>
<%--
  ~ The Broad Institute
  ~ SOFTWARE COPYRIGHT NOTICE AGREEMENT
  ~ This software and its documentation are copyright 2014 by the
  ~ Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support
  ~ whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  ~ use, misuse, or functionality.
  --%>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Bucket View" sectionTitle="Change PDO for Vessels">
<stripes:layout-component name="extraHead">
    <script type="text/javascript">
        $j(document).ready(function () {
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
                ]
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
    <stripes:form beanclass="${actionBean.class.name}"
                  id="bucketEntryForm" class="form-horizontal">
        <div class="form-horizontal">
        <stripes:hidden name="selectedBucket" value="${actionBean.selectedBucket}"/>
        <stripes:hidden name="selectedWorkflowDef" value="${actionBean.selectedWorkflowDef}"/>
</div>

        <div class="borderHeader"><h4>Samples</h4></div>
        <br/>

        <div class="actionButtons">
            <stripes:submit name="changePdo" value="Change PDO" class="btn"/>
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
                <th width="50">PDO</th>
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
            <c:forEach items="${actionBean.allBucketEntries}" var="entry">
                <tr>
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
                    <td>
                        <stripes:select name="selectedPdo" >
                            <stripes:options-collection collection="${actionBean.findPdoForVessel(entry.labVessel.label)}"></stripes:options-collection>
                        </stripes:select>
                            ${entry.productOrder.businessKey}
                    </td>
                    <td>
                        <div class="ellipsis" style="width: 300px">${entry.productOrder.title}</div>
                    </td>
                    <td>
                            ${actionBean.getUserFullName(entry.productOrder.createdBy)}
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
                    <td>
                            ${entry.entryType.name}
                    </td>
                    <td>
                            ${entry.reworkDetail.reworkReason.value}
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
