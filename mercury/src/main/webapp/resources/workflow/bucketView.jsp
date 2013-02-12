<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean"/>
<stripes:useActionBean var="batchActionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.CreateBatchActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Bucket View" sectionTitle="Select Bucket">
    <stripes:layout-component name="extraHead">
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
                        {"bSortable":true, "sType":"date"}
                    ]
                });

                $j('.bucket-checkbox').enableCheckboxRangeSelection({
                    checkAllClass:'bucket-checkAll',
                    countDisplayClass:'bucket-checkedCount',
                    checkboxClass:'bucket-checkbox'});
            })

            function showResult(type) {
                $j('#' + type + 'Div').show();
            }

            function hideResult(type) {
                $j('#' + type + 'Div').hide();
            }

        </script>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="bucketForm">
            <div class="control-group">
                <div class="control">
                    <stripes:select name="selectedBucket">
                        <stripes:options-collection collection="${actionBean.buckets}" label="name"
                                                    value="name"/>
                    </stripes:select>
                    <stripes:submit name="viewBucket" value="View Bucket"/>
                </div>
            </div>
        </stripes:form>
        <stripes:form beanclass="${batchActionBean.class.name}"
                      id="bucketEntryForm">
            <table>
                <tr>
                    <td valign="top">
                        <div class="control-group">
                            <div class="controls">
                                <stripes:radio value="${batchActionBean.existingJiraTicketValue}"
                                               name="jiraInputType"
                                               onclick="javascript:showResult('jiraId');hideResult('newTicket');"/>
                                Use Existing Jira Ticket
                            </div>
                            <div class="controls">
                                <stripes:radio value="${batchActionBean.newJiraTicketValue}"
                                               name="jiraInputType"
                                               onclick="javascript:showResult('newTicket');hideResult('jiraId');"/>
                                Create a New Jira Ticket
                            </div>
                        </div>

                        <div id="jiraIdDiv">
                            <div class="control-group">
                                <stripes:label for="jiraTicketId" name="Jira Ticket Key" class="control-label"/>
                                <div class="controls">
                                    <stripes:text name="jiraTicketId" class="defaultText"
                                                  title="Enter an existing batch ticket" id="jiraTicketId"/>
                                </div>
                            </div>
                        </div>
                        <div id="newTicketDiv" style="display: none;">
                            <div class="control-group">
                                <stripes:label for="summary" name="Summary" class="control-label"/>
                                <div class="controls">
                                    <stripes:text name="summary" class="defaultText"
                                                  title="Enter a summary for a new batch ticket" id="summary"
                                                  value="${batchActionBean.summary}"/>
                                </div>
                            </div>

                            <div class="control-group">
                                <stripes:label for="description" name="Description" class="control-label"/>
                                <div class="controls">
                                    <stripes:textarea name="description" class="defaultText"
                                                      title="Enter a description for a new batch ticket"
                                                      id="description" value="${batchActionBean.description}"/>
                                </div>
                            </div>

                            <div class="control-group">
                                <stripes:label for="important" name="Important Information"
                                               class="control-label"/>
                                <div class="controls">
                                    <stripes:textarea name="important" class="defaultText"
                                                      title="Enter important info for a new batch ticket"
                                                      id="important"
                                                      value="${batchActionBean.important}"/>
                                </div>
                            </div>

                            <div class="control-group">
                                <stripes:label for="dueDate" name="Availability Date" class="control-label"/>
                                <div class="controls">
                                    <stripes:text id="dueDate" name="dueDate" class="defaultText"
                                                  title="enter date (MM/dd/yyyy)"
                                                  value="${batchActionBean.dueDate}"><fmt:formatDate
                                            value="${batchActionBean.dueDate}"
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

            <table id="bucketEntryView" class="table simple">
                <thead>
                <tr>
                    <th width="40">
                        <input type="checkbox" class="bucket-checkAll"/><span id="count"
                                                                              class="bucket-checkedCount"></span>
                    </th>
                    <th>Vessel Name</th>
                    <th>Sample Name</th>
                    <th>PDO</th>
                    <th>Batch Name</th>
                    <th>Sample Type</th>
                    <th>Created Date</th>
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
                            <a href="${ctxpath}/search/all.action?search=&searchKey=${entry.labVessel.label}">
                                    ${entry.labVessel.label}
                            </a>
                        </td>
                        <td>
                            <c:forEach items="${entry.labVessel.mercurySamples}" var="mercurySample">
                                <a href="${ctxpath}/search/all.action?search=&searchKey=${mercurySample.sampleKey}">
                                        ${mercurySample.sampleKey}
                                </a>
                            </c:forEach>
                        </td>
                        <td>
                            <a href="${ctxpath}/search/all.action?search=&searchKey=${entry.poBusinessKey}">
                                    ${entry.poBusinessKey}
                            </a>
                        </td>
                        <td>
                            <c:forEach items="${entry.labVessel.nearestLabBatches}" var="batch">
                                <a href="${ctxpath}/search/all.action?search=&searchKey=${batch.businessKey}">
                                        ${batch.businessKey}
                                </a>
                            </c:forEach>
                        </td>
                        <td>
                            <c:forEach items="${entry.labVessel.mercurySamples}" var="mercurySample">
                                ${mercurySample.bspSampleDTO.materialType}
                            </c:forEach>
                        </td>
                        <td>
                            <fmt:formatDate value="${entry.createdDate}" pattern="MM/dd/yyyy HH:MM:ss"/>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>