<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.hsa.FiniteStateMachineActionBean" %>
<%--
This fragment is used by  workflows view.jsp to re-use code when displaying state data in the FSM
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%--@elvariable id="state" type="org.broadinstitute.gpinformatics.mercury.control.hsa.state"--%>
<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.hsa.FiniteStateMachineActionBean"--%>
<%--@elvariable id="finiteStateMachine" type="org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine"--%>

<stripes:form beanclass="${actionBean.class.name}" id="metricsForm" class="form-horizontal">
    <stripes:hidden name="finiteStateMachineKey" value="${actionBean.finiteStateMachineKey}"/>
    <c:set var="isUpload" value="${state.isUpload()}"/>
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
            <td>State ID</td>
            <td>${state.stateId}</td>
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
            <th width="30px">ID</th>
            <th width="150px">Task Name</th>
            <th width="150px">Start Time</th>
            <th width="150px">End Time</th>
            <th width="50px">Status</th>
            <th width="30px">PID</th>
            <th>arg</th>
            <c:choose>
                <c:when test="${isUpload}">
                    <th>Progress</th>
                </c:when>
                <c:otherwise>
                    <th>Log</th>
                </c:otherwise>
            </c:choose>
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
                <c:if test="${task.class.superclass.simpleName == 'ProcessTask' or task.class.superclass.simpleName == 'PicardTask' or task.class.superclass.simpleName == 'ComputeTask'}">
                    <%--@elvariable id="task" type="org.broadinstitute.gpinformatics.mercury.control.hsa.state.ProcessTask--%>
                    ${task.processId}
                </c:if>
            </td>
            <td>
                <c:if test="${task.class.superclass.simpleName == 'ProcessTask' or task.class.superclass.simpleName == 'PicardTask' or task.class.superclass.simpleName == 'ComputeTask'}">
                    <%--@elvariable id="task" type="org.broadinstitute.gpinformatics.mercury.control.hsa.state.ProcessTask--%>
                    ${task.commandLineArgument}
                </c:if>
            </td>
            <td>
                <c:choose>
                    <c:when test="${isUpload}">
                        <c:set var="progressResult" value="${actionBean.fetchUploadProgress(task.processId)}"/>
                        <%--@elvariable id="progressResult" type="org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.GsUtilLogReader.Result--%>
                        <c:if test="${not empty progressResult}">
                            <progress value="${progressResult.uploaded}" max="${progressResult.fileSize}"></progress>
                            ${progressResult.line}
                        </c:if>
                    </c:when>
                    <c:when test="${task.class.superclass.simpleName == 'ProcessTask' or task.class.superclass.simpleName == 'PicardTask'}">
                        <%--@elvariable id="task" type="org.broadinstitute.gpinformatics.mercury.control.hsa.state.ProcessTask--%>
                        <c:if test="${not empty task.processId and task.class.superclass.simpleName != 'PicardTask'}">
                            <input name="viewLog" type="submit" class="btn btn-info viewLog" data-processid="${task.processId}" value="View Log">
                        </c:if>
                    </c:when>
                </c:choose>
            </td>
            </c:forEach>
    </table>

    <c:if test="${state.exitTask.present}">
        Exit Task?
        <%--@elvariable id="task" type="org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task--%>
        <c:set var="task" value="${state.exitTask.get()}"/>
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

    <div class="row">
        <c:if test="${not empty finiteStateMachine.getTransitionsFromState(state)}">
            Next States:
            <c:forEach items="${finiteStateMachine.getTransitionsFromState(state)}" var="transition">
                <c:set var="nextState" value="${transition.toState}"/>
                <table class="table simple" id="runTable">
                    <tr>
                        <td>State Name</td>
                        <td>${nextState.stateName}</td>
                    </tr>
                    <c:forEach items="${nextState.tasks}" var="task">
                        <tr>
                            <c:if test="${task.class.superclass.simpleName == 'ProcessTask' or task.class.superclass.simpleName == 'PicardTask' or task.class.superclass.simpleName == 'ComputeTask'}">
                                <td>Command</td>
                                <td>${task.commandLineArgument}</td>
                            </c:if>
                        </tr>
                    </c:forEach>
                </table>
            </c:forEach>
        </c:if>
    </div>

    <div class="row">
        <c:set var="pendingDemultiplexes" value="${actionBean.fetchPendingDemultiplexesForAggregation(state)}"/>
        <c:if test="${state.class.simpleName == 'AggregationState' and not empty pendingDemultiplexes}">
            Pending Demultiplexes
            <table class="table simple" id="runTable">
                <c:forEach items="${pendingDemultiplexes}" var="pending">
                    <tr>
                        <td>${pending}</td>
                    </tr>
                </c:forEach>
            </table>
        </c:if>
    </div>
</stripes:form>