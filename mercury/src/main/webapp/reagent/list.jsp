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
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.reagent.ReagentDesignActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="List of Designed Reagents" sectionTitle="Reagent Designs" showCreate="true">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#reagentList').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[0,'asc']],
                    "aoColumns": [
                        {"bSortable": true, "sType": "html"},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true}
                    ]
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <div class="actionButtons">
            <stripes:link title="Assign Barcodes" beanclass="${actionBean.class.name}" event="assignBarcode">
                Assign Barcodes
            </stripes:link>
        </div>

        <div class="clearfix"></div>

        <table id="reagentList" class="table simple">
            <thead>
            <tr>
                <th>Design Name</th>
                <th>Reagent Type</th>
                <th>Target Set</th>
                <th>Manufacturer</th>
                <%--<th>Barcodes</th>--%>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${actionBean.allReagentDesigns}" var="design">
                <tr>
                    <td>
                        <stripes:link beanclass="${actionBean.class.name}" event="edit">
                            <stripes:param name="reagentDesign" value="${design.businessKey}"/>
                            ${design.designName}
                        </stripes:link>
                    </td>
                    <td>${design.reagentType}</td>
                    <td>${design.targetSetName}</td>
                    <td>${design.manufacturersName}</td>
                        <%--<td>--%>
                        <%--${actionBean.barcodeMap[design.designName]}--%>
                        <%--</td>--%>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </stripes:layout-component>
</stripes:layout-render>
