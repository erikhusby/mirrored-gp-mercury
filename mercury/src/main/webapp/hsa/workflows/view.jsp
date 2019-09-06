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
                       sectionTitle="View Workflow: ${actionBean.editFiniteStateMachine.stateMachineName}">

    <stripes:layout-component name="extraHead">
        <style type="text/css">
            .ui-widget-content{
                background:#FFFFFF;
            }
        </style>
        <script type="text/javascript">
            $j(document).ready(function () {

                $j('#taskList').DataTable({
                    "oTableTools": {
                        "sSwfPath": "/Mercury/resources/scripts/DataTables-1.9.4/extras/TableTools/media/swf/copy_csv_xls.swf",
                        "aButtons": [
                            {
                                "sExtends" : "csv",
                                "bHeader" : false,
                                "sFieldBoundary": "",
                                "mColumns": [ 1, 2, 3, 4, 5 ]
                            }
                        ]
                    },
                    "aoColumns": [
                        {"bSortable": false},
                        {"bSortable": true},
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

                $j("#accordion").accordion({ collapsible:true, active:false, heightStyle:"content", autoHeight:false });
                $j("#accordion").show();
                if(${fn:length(actionBean.editFiniteStateMachine.activeStates) == 1}){
                    $j("#accordion").accordion({active: 0})
                }

            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <h4>Active States</h4>
        <div id="accordion" style="display:none;" class="accordion">
            <c:forEach items="${actionBean.editFiniteStateMachine.activeStates}" var="state">
                <c:set var="state" value="${state}" scope="request"/>
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

    </stripes:layout-component>
</stripes:layout-render>
