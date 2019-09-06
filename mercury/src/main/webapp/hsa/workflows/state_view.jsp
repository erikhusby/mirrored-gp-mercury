<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.hsa.FiniteStateMachineActionBean" %>
<%--
This fragment is used by  workflows view.jsp to re-use code when displaying state data in the FSM
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%--@elvariable id="state" type="org.broadinstitute.gpinformatics.mercury.control.hsa.state"--%>
<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.hsa.FiniteStateMachineActionBean"--%>

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
        <tr>
            <td># Tasks Running?</td>
            <td>${state.getNumberOfRunningTasks()}</td>
        </tr>
        <tr>
            <td># Tasks Queued?</td>
            <td>${state.getNumberOfQueuedTasks()}</td>
        </tr>
        <tr>
            <td># Tasks Failed?</td>
            <td>${state.getNumberOfFailedTasks()}</td>
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
            <td>ID</td>
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
            <td>${task.taskId}</td>
            <td>${task.taskName}</td>
            <td>${task.startTime}</td>
            <td>${task.endTime}</td>
            <td>${task.status}</td>
            <td>
                <c:if test="${task.class.superclass.simpleName == 'ProcessTask' or task.class.superclass.simpleName == 'PicardTask'}">
                    <%--@elvariable id="task" type="org.broadinstitute.gpinformatics.mercury.control.hsa.state.ProcessTask--%>
                    ${task.processId}
                </c:if>
            </td>
            <td>
                <c:if test="${task.class.superclass.simpleName == 'ProcessTask' or task.class.superclass.simpleName == 'PicardTask'}">
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