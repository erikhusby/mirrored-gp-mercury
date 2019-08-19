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

<stripes:layout-render name="/layout.jsp" pageTitle="View Workflows"
                       sectionTitle="View Workflows ${actionBean.editFiniteStateMachine.stateMachineName}">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {

                $j('#samplesTable').DataTable({
                    "oTableTools": {
                        "aButtons": ["copy", "csv"]
                    },
                    "aoColumns": [
                        {"bSortable": false},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true}
                    ]
                });

                $j('.task-checkbox').enableCheckboxRangeSelection({
                    checkAllClass:'task-checkAll',
                    countDisplayClass:'task-checkedCount',
                    checkboxClass:'task-checkbox'});

            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <c:forEach items="${actionBean.editFiniteStateMachine.activeStates}" var="state">
            <stripes:form beanclass="${actionBean.class.name}" id="metricsForm" class="form-horizontal">
                <stripes:hidden name="finiteStateMachineKey" value="${actionBean.finiteStateMachineKey}"/>
                <table class="table simple" id="runTable">
                    <c:if test="${state.class.simpleName == 'DemultiplexState'}">
                        <c:set var="demultiplexState" value="${state}"/>
                        <%--@elvariable id="demultiplexState" type="org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState--%>
                            <tr>
                                <td>Run Name</td>
                                <td>${demultiplexState.run.runName}</td>
                            </tr>
                    </c:if>
                    <tr>
                        <td>State Name</td>
                        <td>${state.stateName}</td>
                    </tr>
                    <tr>
                        <td>Alive?</td>
                        <td>${state.alive}</td>
                    </tr>
                </table>

                <table id="taskList" class="table simple">
                <thead>
                <tr>
                <tr>
                    <th width="30px">
                        <input type="checkbox" class="task-checkAll" title="Check All"/>
                        <span id="count" class="task-checkedCount"></span>
                    </th>
                    <td>Task Name</td>
                    <td>Start Time</td>
                    <td>End Time</td>
                    <td>Status</td>
                    <td>PID</td>
                    <td>arg</td>
                </tr>
                </thead>
                <c:forEach items="${state.tasks}" var="task">
                    <tr>
                        <td>
                            <stripes:checkbox class="task-checkbox" name="selectedIds"
                                                  value="${task.taskId}"/>
                        </td>
                        <td>${task.taskName}</td>
                        <td>${task.startTime}</td>
                        <td>${task.endTime}</td>
                        <td>${task.status}</td>
                        <td>
                            <c:if test="${task.class.superclass.simpleName == 'ProcessTask'}">
                                <%--@elvariable id="task" type="org.broadinstitute.gpinformatics.mercury.control.hsa.state.ProcessTask--%>
                                ${task.processId}
                            </c:if>
                        </td>
                        <td>
                            <c:if test="${task.class.superclass.simpleName == 'ProcessTask'}">
                                <%--@elvariable id="task" type="org.broadinstitute.gpinformatics.mercury.control.hsa.state.ProcessTask--%>
                                ${task.commandLineArgument}
                            </c:if>
                        </td>
                </c:forEach>
                </table>

                <c:if test="${state.exitTask.present}">
                    Exit Task?
                    <c:set var="task" value="${state.exitTask.get()}"/>
                    <%--@elvariable id="task" type="org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task--%>
                    <table id="exitTaskList" class="table simple">
                        <thead>
                        <tr>
                            <td></td>
                            <td>Task Name</td>
                            <td>Start Time</td>
                            <td>End Time</td>
                            <td>Status</td>
                        </tr>
                        </thead>
                        <tr>
                            <td>
                                <stripes:checkbox class="task-checkbox" name="selectedIds"
                                                  value="${task.taskId}"/>
                            </td>
                            <td>${task.taskName}</td>
                            <td>${task.startTime}</td>
                            <td>${task.endTime}</td>
                            <td>${task.status}</td>
                        </tr>
                    </table>
                </c:if>
                <stripes:select name="overrideStatus">
                    <stripes:options-enumeration label="statusName"
                                                 enum="org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status"/>
                </stripes:select>
                <stripes:submit name="updateTaskStatus" value="Update Status" class="btn btn-primary"/>
            </stripes:form>

        </c:forEach>
    </stripes:layout-component>
</stripes:layout-render>
