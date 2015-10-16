<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.selectedBucket}"
                       sectionTitle="Confirm ${actionBean.confirmationPageTitle}">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j('#selectedReworks').dataTable({
                    "oTableTools":ttExportDefines,
                    "aaSorting":[
                        [1, 'asc']
                    ],
                    "aoColumns":[
                        {"bSortable":true, sType:'html'},
                        {"bSortable":true, sType:'html'},
                        {"bSortable":true},
                        {"bSortable":true},
                        {"bSortable":true},
                        {"bSortable":true}
                    ],
                    "sScrollY":500,
                    "bScrollCollapse":true
                });
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" class="form-horizontal">
            <stripes:hidden name="selectedLcset"/>
            <stripes:hidden name="selectedBucket"/>
            <stripes:hidden name="selectedEntryIds"/>
            <h5>Adding samples :</h5>
            <table id="selectedReworkVessels" class="table simple">
                <thead>
                <tr>
                    <th width="60">Vessel Name</th>
                    <th width="50">Sample Name</th>
                    <th width="50">PDO</th>
                    <th width="300">PDO Name</th>
                    <th width="200">PDO Owner</th>
                    <th>Last Batch Name</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.selectedEntries}" var="entry">
                    <tr>
                        <td>

                            <a href="${ctxpath}/search/vessel.action?vesselSearch=&searchKey=${entry.labVessel.label}">
                                    ${entry.labVessel.label}
                            </a>
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
                        <td>
                                ${entry.productOrder.businessKey}
                        </td>
                        <td>
                            <div class="ellipsis" style="width: 300px;">${entry.productOrder.title}</div>
                        </td>
                        <td>
                                ${actionBean.getUserFullName(entry.productOrder.createdBy)}
                        </td>
                        <td>
                            <c:forEach items="${entry.labVessel.nearestWorkflowLabBatches}" var="batch"
                                       varStatus="stat">
                                ${batch.businessKey}
                                <c:if test="${!stat.last}">&nbsp;</c:if>
                            </c:forEach>

                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
            <h5>To Batch :</h5>

            <div class="control-group" style="margin-bottom: 0;">
                <stripes:label name="Batch Name" class="control-label"/>
                <div class="controls" style="padding-top: 5px;">
                        ${actionBean.batch.businessKey}
                </div>
            </div>
            <div class="control-group" style="margin-bottom: 0;">
                <stripes:label name="Create Date" class="control-label"/>
                <div class="controls" style="padding-top: 5px;">
                    <fmt:formatDate value="${actionBean.batch.createdOn}"
                                    pattern="${actionBean.datePattern}"/>
                </div>
            </div>
            <div class="control-group" style="margin-bottom: 0;">
                <stripes:label name="Sample Count" class="control-label"/>
                <div class="controls" style="padding-top: 5px;">
                        ${fn:length(actionBean.batch.startingBatchLabVessels)}
                </div>
            </div>
            <div class="control-group" style="margin-bottom: 0;">
                <stripes:label name="Bucket" class="control-label"/>
                <div class="controls" style="padding-top: 5px;">
                        ${actionBean.selectedBucket}
                </div>
            </div>
            <stripes:submit name="reworkConfirmed" id="confirmBtn" value="Confirm" class="btn btn-primary"/>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
