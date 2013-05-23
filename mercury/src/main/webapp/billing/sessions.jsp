<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Billing Sessions" sectionTitle="List Billing Sessions">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#sessionList').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[2,'desc']],
                    "aoColumns": [
                        {"bSortable": true, "sType": "title-jira"}, // ID
                        {"bSortable": true},                        // Created By
                        {"bSortable": true, "sType": "date"},       // Created Date
                        {"bSortable": true, "sType": "date"}]       // Billed Date
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form action="/orders/order.action" id="createForm" class="form-horizontal">

            <table id="sessionList" class="table simple">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Created By</th>
                        <th>Created Date</th>
                        <th>Billed Date</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${actionBean.billingSessions}" var="session">
                        <tr>
                            <td>
                                <stripes:link href="/billing/session.action" event="view" title="${session.businessKey}">
                                    <stripes:param name="sessionKey" value="${session.businessKey}"/>
                                    ${session.businessKey}
                                </stripes:link>
                            </td>
                            <td>${actionBean.getUserFullName(session.createdBy)}</td>
                            <td>
                                <fmt:formatDate value="${session.createdDate}" pattern="${actionBean.datePattern}"/>
                            </td>
                            <td>
                                <fmt:formatDate value="${session.billedDate}" pattern="${actionBean.datePattern}"/>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
