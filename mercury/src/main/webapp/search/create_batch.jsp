<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<%@ taglib prefix="str" uri="http://stripes.sourceforge.net/stripes-dynattr.tld" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Create Batches from Vessels" sectionTitle="Search">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            function showResult(type) {
                $j('#' + type + 'Div').show();
            }

            function hideResult(type) {
                $j('#' + type + 'Div').hide();
            }
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="searchForm" class="form-horizontal">
            <table>
                <tr>
                    <td valign="top">
                        <div class="form-horizontal">
                            <div class="control-group" style="margin-bottom:5px;">
                                <stripes:label for="barcode" class="control-label"
                                               style="width: 60px;">Barcodes, PDOs or Sample names</stripes:label>
                                <div class="controls" style="margin-left: 80px;">
                                    <stripes:textarea rows="5" cols="80" name="searchKey" id="name"
                                                      title="Enter the value to search"
                                                      style="width:auto;" class="defaultText"/>
                                </div>
                            </div>

                            <div class="control-group">
                                <div class="controls" style="margin-left: 80px;">
                                    <stripes:submit name="searchBatchCandidates" value="Search"/>
                                </div>
                            </div>
                        </div>
                    </td>

                    <c:if test="${not empty actionBean.foundVessels}">
                    <td valign="top">
                        <div class="control-group">
                            <div class="controls">
                                <stripes:radio value="${actionBean.existingJiraTicketValue}"
                                               name="jiraInputType"
                                               onclick="javascript:showResult('jiraId');hideResult('newTicket');"/>
                                Use Existing Jira Ticket
                            </div>
                            <div class="controls">
                                <stripes:radio value="${actionBean.newJiraTicketValue}"
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
                                                  title="Enter a summary for a new batch ticket" id="summary" value="${actionBean.summary}"/>
                                </div>
                            </div>

                            <div class="control-group">
                                <stripes:label for="description" name="Description" class="control-label"/>
                                <div class="controls">
                                    <stripes:textarea name="description" class="defaultText"
                                                      title="Enter a description for a new batch ticket" id="description" value="${actionBean.description}"/>
                                </div>
                            </div>

                            <div class="control-group">
                                <stripes:label for="important" name="Important Information" class="control-label"/>
                                <div class="controls">
                                    <stripes:textarea name="important" class="defaultText"
                                                      title="Enter important info for a new batch ticket" id="important" value="${actionBean.important}"/>
                                </div>
                            </div>

                            <div class="control-group">
                                <stripes:label for="dueDate" name="Due Date" class="control-label"/>
                                <div class="controls">
                                    <stripes:text id="dueDate" name="dueDate" class="defaultText"
                                                  title="enter date (MM/dd/yyyy)" value="${actionBean.dueDate}"><fmt:formatDate
                                            value="${actionBean.dueDate}" pattern="${actionBean.datePattern}"/></stripes:text>
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
                </c:if>
            </table>

            <c:if test="${not actionBean.resultsAvailable}">
                No Results Found
            </c:if>

            <c:if test="${not empty actionBean.foundVessels}">
                <div class="tableBar">
                    Found ${fn:length(actionBean.foundVessels)} Vessels
                </div>
                <div id="vesselDiv">
                    <stripes:layout-render name="/vessel/vessel_list.jsp" vessels="${actionBean.foundVessels}"
                                           bean="${actionBean}" showCheckboxes="true"/>
                </div>
            </c:if>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
