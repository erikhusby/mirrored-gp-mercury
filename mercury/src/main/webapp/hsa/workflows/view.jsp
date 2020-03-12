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

<stripes:layout-render name="/layout.jsp" pageTitle="View Workflow"
                       sectionTitle="View Workflow: ${actionBean.editFiniteStateMachine.stateMachineName}: Current Status ${actionBean.editFiniteStateMachine.status}"
                       dataTablesVersion="1.10" >

    <stripes:layout-component name="extraHead">
        <style type="text/css">
            .ui-widget-content{
                background:#FFFFFF;
            }
        </style>
        <script type="text/javascript">
            $j(document).ready(function () {

                $j('#taskList').dataTable({
                    renderer: "bootstrap",
                    columns: [
                        {sortable: true},
                        {sortable: true},
                        {sortable: true},
                        {sortable: true},
                        {sortable: true},
                        {sortable: true},
                        {sortable: true},
                        {sortable: true},
                        {sortable: true}
                    ],
                });

                $j('.task-checkbox').enableCheckboxRangeSelection({
                    checkAllClass:'task-checkAll',
                    countDisplayClass:'task-checkedCount',
                    checkboxClass:'task-checkbox'});

                $j("#accordion").accordion({ collapsible:true, active:false, heightStyle:"content", autoHeight:false });
                $j("#accordion").show();
                if(${fn:length(actionBean.editFiniteStateMachine.activeStates) == 1}) {
                    $j("#accordion").accordion({active: 0})
                }

                $j("#log_overlay").dialog({
                    title: "View Logs",
                    autoOpen: false,
                    height: 600,
                    width: 800,
                    modal: true,
                    buttons: {
                        "OK": {
                            text: "OK",
                            id: "okbtnid",
                            click: function () {
                                $j(this).dialog("close");
                            }
                        },
                    },
                    open: function(){

                    }
                });

                $j(".viewLog").click(function (event) {
                    event.preventDefault();
                    var processId = $j(this).data("processid");
                    $j.ajax({
                        url: "/Mercury/hsa/workflows/dragen.action?viewLog=&processId=" + processId,
                        type: 'POST',
                        async: true,
                        success: function (contents) {
                            console.log(contents);
                            $j("#logFileDiv").text(contents);
                            $j("#log_overlay").dialog("open");
                        },
                        error: function(contents){
                            console.log("Error message");
                            console.log(contents);
                        },
                        cache: false,
                        datatype: "application/text",
                        processData: false,
                        contentType: false
                    });
                });
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <div id="log_overlay">
            <div id="logFileDiv"></div>
        </div>


        <stripes:form beanclass="${actionBean.class.name}" id="machineStatusForm">
            <stripes:hidden name="selectedIds[0]" value="${actionBean.editFiniteStateMachine.finiteStateMachineId}"/>
            <stripes:select name="overrideStatus" value="${actionBean.editFiniteStateMachine.status}">
                <stripes:options-enumeration label="statusName"
                                             enum="org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status"/>
            </stripes:select>
            <stripes:submit name="updateStateStatus" value="Update Machine Status" class="btn btn-primary"/>
        </stripes:form>

        <c:set var="activeStates" value="${actionBean.editFiniteStateMachine.activeStates}"/>
        <c:choose>
            <c:when test="${empty activeStates}">
                <h4>Completed/Not Active States</h4>
                <div id="accordion" style="display:none;" class="accordion">
                    <c:forEach items="${actionBean.editFiniteStateMachine.states}" var="state">
                        <c:set var="state" value="${state}" scope="request"/>
                        <c:set var="finiteStateMachine" value="${actionBean.editFiniteStateMachine}" scope="request"/>
                        <div style="padding-left: 30px;padding-bottom: 2px">
                            <div class="fourcolumn">
                                <div>State Name: ${state.stateName}</div>
                                <div>Type: <td>${state.class.simpleName}</td></div>
                                <div>
                                    Start Time: <td>${state.startTime}</td>
                                </div>
                            </div>
                        </div>
                        <jsp:include page="state_view.jsp"/>
                    </c:forEach>
                </div>
            </c:when>
            <c:otherwise>
                <h4>Active States</h4>
                <div id="accordion" style="display:none;" class="accordion">
                    <c:forEach items="${actionBean.editFiniteStateMachine.activeStates}" var="state">
                        <c:set var="state" value="${state}" scope="request"/>
                        <c:set var="finiteStateMachine" value="${actionBean.editFiniteStateMachine}" scope="request"/>
                        <div style="padding-left: 30px;padding-bottom: 2px">
                            <div id="headerId" class="fourcolumn">
                                <div>State Name: ${state.stateName}</div>
                                <div>Type: <td>${state.class.simpleName}</td></div>
                                <div>
                                    Start Time: <td>${state.startTime}</td>
                                </div>
                            </div>
                        </div>
                        <jsp:include page="state_view.jsp"/>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </stripes:layout-component>
</stripes:layout-render>
