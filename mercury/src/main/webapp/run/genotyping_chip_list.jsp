<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.run.GenotypingChipTypeActionBean" %>
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
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.run.GenotypingChipTypeActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="List Genotyping Chips" sectionTitle="List Genotyping Chips" showCreate="true">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#reagentList').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[0,'asc'],[1,'asc'],[2,'asc']],
                    "aoColumns": [
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true},
                    ]
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <div class="actionButtons">
            <stripes:link title="Add New Genotyping Chip" beanclass="${actionBean.class.name}" event="createChip">
                New
            </stripes:link>
        </div>

        <div class="clearfix"></div>

        <table id="chipList" class="table simple">
            <thead>
            <tr>
                <th>Family</th>
                <th>Chip</th>
                <th>Created Date</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${actionBean.allGenotypingChips}" var="chip">
                <tr>
                    <td>${chip.attributeDef.family}</td>
                    <td>
                        <stripes:link beanclass="${actionBean.class.name}" event="edit">
                            <stripes:param name="chip" value="${chip.name}"/>
                            ${chip.name}
                        </stripes:link>
                    </td>
                    <td>${chip.createdDate}</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </stripes:layout-component>
</stripes:layout-render>
