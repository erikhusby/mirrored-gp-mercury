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
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.hsa.DragenActionBean"/>

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
                        {"bSortable": true}
                    ]
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <table id="machineList" class="table simple">
            <thead>
            <tr>
                <th>Machine Name</th>
                <th>Status</th>
                <th>State</th>
                <th>Start Date</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${actionBean.allActiveMachines}" var="machine">
                <tr>
                    <td>
                        <stripes:link beanclass="${actionBean.class.name}" event="edit">
                            <stripes:param name="finiteStateMachineKey" value="${machine.finiteStateMachineId}"/>
                            ${machine.stateMachineName}
                        </stripes:link>
                    </td>
                    <td>${machine.activeStates}</td>
                    <td>${machine.status.name}</td>
                    <td>${machine.dateStarted}</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </stripes:layout-component>
</stripes:layout-render>
