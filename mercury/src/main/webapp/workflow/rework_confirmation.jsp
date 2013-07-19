<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Confirm Rework Addition" sectionTitle="Confirm Rework">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $(document).ready(function () {
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
            <stripes:hidden name="selectedReworks"/>
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
                <c:forEach items="${actionBean.reworkVessels}" var="reworkVessel">
                    <tr>
                        <td>

                            <a href="${ctxpath}/search/vessel.action?vesselSearch=&searchKey=${reworkVessel.label}">
                                    ${reworkVessel.label}
                            </a>
                        </td>
                        <td>
                            <c:forEach items="${reworkVessel.mercurySamples}"
                                       var="mercurySample"
                                       varStatus="stat">
                                <a href="${ctxpath}/search/sample.action?sampleSearch=&searchKey=${mercurySample.sampleKey}">
                                        ${mercurySample.sampleKey}
                                </a>

                                <c:if test="${!stat.last}">&nbsp;</c:if>
                            </c:forEach>
                        </td>
                        <td>
                                ${actionBean.getSinglePDOBusinessKey(reworkVessel)}
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
