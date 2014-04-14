<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.admin.BillingSessionAccessActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Manage Billing Session Locks"
                       sectionTitle="Manage Billing Session Locks">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j('#billingSessionLockTable').dataTable({
                    "oTableTools": ttExportDefines,
                    "aaSorting": [
                        [1, 'asc']
                    ],
                    "aoColumns": [
                        {"bSortable": true},
                        {"bSortable": true}
                    ]
                });
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}">
            <div class="actionButtons">
                <stripes:submit name="clearLocks" value="Clear Selected" class="btn"/>
            </div>

            <table id="billingSessionLockTable" class="table simple">
                <thead>
                <tr>
                    <security:authorizeBlock roles="<%= roles(Developer) %>">
                        <th width="40">
                            <input for="count" type="checkbox" class="checkAll"/><span id="count"
                                                                                       class="checkedCount"></span>
                        </th>
                    </security:authorizeBlock>
                    <th>Billing Session</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.lockedSessions}" var="session">
                    <tr>
                        <security:authorizeBlock roles="<%= roles(Developer) %>">
                            <td><stripes:checkbox
                                    class="shiftCheckbox"
                                    value="${session}"
                                    name="sessionsToUnlock"></stripes:checkbox></td>
                        </security:authorizeBlock>
                        <td>${session}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>