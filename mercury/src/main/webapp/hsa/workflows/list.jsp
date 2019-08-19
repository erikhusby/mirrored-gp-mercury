<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.reagent.ReagentDesignActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%--
  ~ The Broad Institute
  ~ SOFTWARE COPYRIGHT NOTICE AGREEMENT
  ~ This software and its documentation are copyright 2013 by the
  ~ Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support
  ~ whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  ~ use, misuse, or functionality.
  --%>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.hsa.FiniteStateMachineActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="List Dragen Workflows" sectionTitle="List Dragen Workflows" showCreate="true">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#machineList').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[0,'asc']],
                    "aoColumns": [
                        {"bSortable": true, "sType": "html"},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true}
                    ]
                });

                $j('.machine-checkbox').enableCheckboxRangeSelection({
                    checkAllClass:'machine-checkAll',
                    countDisplayClass:'machine-checkedCount',
                    checkboxClass:'machine-checkbox'});
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="searchForm">
            <div class="search-horizontal">
                <div class="control-group">
                    <stripes:label for="runName" class="control-label">
                        Run Name
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="runName" name="runName" class="defaultText search-input"/>
                    </div>
                </div>
                <div class="control-group">
                    <div class="control-label">&#160;</div>
                    <div class="controls actionButtons">
                        <stripes:submit name="list" value="Search" style="margin-right: 10px;margin-top:10px;" class="btn btn-mini"/>
                    </div>
                </div>
            </div>
        </stripes:form>
        <stripes:form beanclass="${actionBean.class.name}" id="searchForm">
            <table id="machineList" class="table simple">
                <thead>
                <tr>
                    <th width="30px">
                        <input type="checkbox" class="machine-checkAll" title="Check All"/>
                        <span id="count" class="machine-checkedCount"></span>
                    </th>
                    <th>Machine Name</th>
                    <th>Status</th>
                    <th>State</th>
                    <th>Issues</th>
                    <th>Start Date</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.allActiveMachines}" var="machine">
                    <tr>
                        <td>
                            <stripes:checkbox name="selectedIds" class="machine-checkbox"
                                              value="${machine.finiteStateMachineId}"/>
                        </td>
                        <td>
                            <stripes:link beanclass="${actionBean.class.name}" event="view">
                                <stripes:param name="finiteStateMachineKey" value="${machine.finiteStateMachineId}"/>
                                ${machine.stateMachineName}
                            </stripes:link>
                        </td>
                        <td>${machine.status}</td>
                        <td>${machine.activeStateNames}</td>
                        <td>
                            <c:if test="${machine.getNumberOfActiveIssues() > 0}">
                                ${machine.getNumberOfActiveIssues()}!
                            </c:if>
                        </td>
                        <td>${machine.dateStarted}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
            <stripes:select name="overrideStatus">
                <stripes:options-enumeration label="statusName"
                                             enum="org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status"/>
            </stripes:select>
            <stripes:submit name="updateStateStatus" value="Update Status" class="btn btn-primary"/>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
