<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<%@ taglib prefix="str" uri="http://stripes.sourceforge.net/stripes-dynattr.tld" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.CreateBatchActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Create Batches from Vessels" sectionTitle="Search">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            function showResult(div) {
                $("." + div).show();
            }

            function hideResult(div) {
                $("." + div).hide();
            }
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="searchForm" class="form-horizontal">
            <table>
                <tr>
                    <td>
                        <div class="form-horizontal">
                            <div class="control-group" style="margin-bottom:5px;">
                                <stripes:label for="barcode" class="control-label"
                                               style="width: 60px;">barcode</stripes:label>
                                <div class="controls" style="margin-left: 80px;">
                                    <stripes:textarea rows="5" cols="80" name="searchKey" id="name"
                                                      title="Enter the value to search"
                                                      style="font-size: x-small; width:auto;" class="defaultText"/>
                                </div>
                            </div>

                            <div class="control-group">
                                <div class="controls" style="margin-left: 80px;">
                                    <stripes:submit name="search" value="Search"/>
                                </div>
                            </div>
                        </div>
                    </td>
                </tr>
                <c:if test="${not empty actionBean.foundVessels}">
                    <tr>
                        <td>
                            <stripes:radio value="${actionBean.existingJiraTicketValue}"
                                           name="jiraInputType">Use Existing Jira Ticket</stripes:radio>
                            <stripes:radio value="${actionBean.newJiraTicketValue}"
                                           name="jiraInputType">Create a New Jira Ticket</stripes:radio>
                        </td>
                        <div id="jiraIdDiv">
                            <div class="control-group">
                                <stripes:label for="jiraTicketId" name="Jira Ticket Key" class="control-label"/>
                                <div class="controls">
                                    <stripes:text name="jiraTicketId" class="defaultText"
                                                  title="Enter an existing batch ticket"/>
                                </div>
                            </div>
                        </div>
                        <div id="newTicketDIv" style="display: none;">
                            <div class="control-group">
                                <stripes:label for="summary" name="Summary" class="control-label"/>
                                <div class="controls">
                                    <stripes:text name="summary" class="defaultText"
                                                  title="Enter a summary for a new batch ticket"/>
                                </div>
                            </div>

                            <div class="control-group">
                                <stripes:label for="description" name="Description" class="control-label"/>
                                <div class="controls">
                                    <stripes:textarea name="description" class="defaultText"
                                                      title="Enter a description for a new batch ticket"/>
                                </div>
                            </div>

                            <div class="control-group">
                                <stripes:label for="important" name="Important Information" class="control-label"/>
                                <div class="controls">
                                    <stripes:textarea name="important" class="defaultText"
                                                      title="Enter important info for a new batch ticket"/>
                                </div>
                            </div>

                            <div class="control-group">
                                <stripes:label for="dueDate" name="Availability Date" class="control-label"/>
                                <div class="controls">
                                    <stripes:text id="dueDate" name="dueDate" class="defaultText"
                                        title="enter date (MM/dd/yyyy)"><fmt:formatDate
                                            value="${actionBean.dueDate}" dateStyle="short"/></stripes:text>
                                </div>
                            </div>
                        </div>
                        <stripes:submit name="createBatch" value="Create Batcfh" class="btn btn-primary"/>
                    </tr>
                </c:if>
            </table>
            <%--</stripes:form>--%>


            <c:if test="${not actionBean.resultsAvailable}">
                No Results Found
            </c:if>

            <%--<stripes:form beanclass="${actionBean.class.name}" id="createBatchForm" class="form-horizontal">--%>

            <c:if test="${not empty actionBean.foundVessels}">
                <div class="tableBar">
                    Found ${fn:length(actionBean.foundVessels)} Vessels
                        <%--<a id="vesselAnchor" href="javascript:showResult('vesselDiv')" style="margin-left: 20px;">show</a>--%>
                        <%--<a id="vesselAnchorHide" href="javascript:hideResult('vesselDiv')"--%>
                        <%--style="display:none; margin-left: 20px;">hide</a>--%>
                </div>
                <%--<div id="vesselDiv" style="display:none">--%>
                <div id="vesselDiv">

                    <table id="productOrderList" class="table simple">
                        <thead>
                        <tr>
                            <th width="40">
                                <input for="count" type="checkbox" class="checkAll"/><span id="count"
                                                                                           class="checkedCount"></span>
                            </th>
                            <th>Sample Details</th>
                            <th>Sample #</th>
                            <th>Label</th>
                            <th>Type</th>
                            <th>Lab Batches</th>
                            <th>Latest Event</th>
                            <th>Event User</th>
                            <th>Event Date</th>
                            <th>Creation Date</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${actionBean.foundVessels}" var="vessel">
                            <tr>
                                <td width="40">
                                    <stripes:checkbox class="shiftCheckbox" name="selectedBatchVesselLabels"
                                                      value="${vessel.label}"/>
                                </td>
                                <td>

                                </td>
                                <td>${vessel.sampleInstanceCount}</td>
                                <td>${vessel.label}</td>
                                <td>${vessel.type.name}</td>
                                <td>
                                    <c:forEach items="${vessel.nearestLabBatches}" var="batch">
                                        ${batch.batchName}
                                    </c:forEach>
                                </td>
                                <td>${vessel.latestEvent.labEventType.name}</td>
                                <td>${vessel.latestEvent.labEventType.name}</td>
                                <td>
                                    <fmt:formatDate value="${vessel.latestEvent.eventDate}" pattern="MM/dd/yyyy"/>
                                </td>
                                <td><fmt:formatDate value="${vessel.createdOn}" pattern="MM/dd/yyyy"/></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                </div>

                <div class="control-group">
                    <div class="controls" style="margin-left: 80px;">
                        <stripes:submit name="createBatch" value="Create Batch"/>
                    </div>
                </div>

            </c:if>
        </stripes:form>


    </stripes:layout-component>
</stripes:layout-render>
