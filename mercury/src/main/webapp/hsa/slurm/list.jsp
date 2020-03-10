<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.hsa.SlurmActionBean" %>
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
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.hsa.SlurmActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="List DRAGEN Nodes" sectionTitle="List DRAGEN Nodes" showCreate="false"
                       dataTablesVersion="1.10" >

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#nodeList').dataTable({
                    renderer: "bootstrap",
                    columns: [
                        {sortable: false},
                        {sortable: true},
                        {sortable: true},
                        {sortable: true},
                        {sortable: true}
                    ],
                });

                $j('.nodeList-checkbox').enableCheckboxRangeSelection({
                    checkAllClass:'nodeList-checkAll',
                    countDisplayClass:'nodeList-checkedCount',
                    checkboxClass:'nodeList-checkbox'});
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="searchForm">
            <table id="nodeList" class="table simple">
                <thead>
                <tr>
                    <th width="30px">
                        <input type="checkbox" class="nodeList-checkAll" title="Check All"/>
                        <span id="count" class="nodeList-checkedCount"></span>
                    </th>
                    <th>Name</th>
                    <th>Available</th>
                    <th>State</th>
                    <th>Version</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.nodeList}" var="node">
                    <tr>
                        <td>
                            <stripes:checkbox name="selectedIds" class="machine-checkbox"
                                              value="${node.name}"/>
                        </td>
                        <td>${node.name}</td>
                        <td>${node.available}</td>
                        <td>${node.state}</td>
                        <td>${node.version}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
