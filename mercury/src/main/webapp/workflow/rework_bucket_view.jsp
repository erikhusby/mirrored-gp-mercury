<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Rework Bucket View" sectionTitle="Select Rework Bucket">
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
                      action="/view/bucketView.action?viewRework">
            <div class="control-group">
                <stripes:label for="bucketselect" name="Select Bucket" class="control-label"/>
                <div class="controls">
                    <stripes:select id="bucketSelect" name="selectedBucket" onchange="submitBucket()">
                        <stripes:options-collection collection="${actionBean.buckets}" label="name"
                                                    value="name"/>
                    </stripes:select>
                </div>
            </div>
        </stripes:form>
        <stripes:form beanclass="${actionBean.class.name}"
                      id="bucketEntryForm" class="form-horizontal">
            <stripes:hidden name="selectedBucket" value="${actionBean.selectedBucket}"/>
            <stripes:hidden name="selectedProductWorkflowDef" value="${actionBean.selectedProductWorkflowDef}"/>
            <div class="control-group">
                <stripes:label for="lcsetText" class="control-label"/>
                <div class="controls">
                    <stripes:text id="lcsetText" name="selectedLcset"/>
                </div>
            </div>
            <div class="control-group">
                <div class="controls">
                    <stripes:submit name="addToBatch" value="Add to Batch" class="btn btn-primary"/>
                </div>
            </div>

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
                            <c:forEach items="${reworkVessel.labVessel.mercurySamples}"
                                       var="mercurySample"
                                       varStatus="stat">
                                <a href="${ctxpath}/search/sample.action?sampleSearch=&searchKey=${mercurySample.sampleKey}">
                                        ${mercurySample.sampleKey}
                                </a>

                                <c:if test="${!stat.last}">&nbsp;</c:if>
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
                                ${actionBean.getUserFullName(reworkVessel.reworkDetail.addToReworkBucketEvent.eventOperator)}
                        </td>
                        <td>
                            <fmt:formatDate value="${reworkVessel.reworkDetail.addToReworkBucketEvent.eventDate}"
                                            pattern="MM/dd/yyyy HH:mm:ss"/>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
