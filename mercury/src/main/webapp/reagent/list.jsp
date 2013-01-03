<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.reagent.ReagentDesignActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="List of Designed Reagents" sectionTitle="Designed Reagents">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#reagentList').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[1,'asc']],
                    "aoColumns": [
                        {"bSortable": true, "sType": "html"},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true}]
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <p>
            <stripes:link title="New Reagent Design" beanclass="${actionBean.class.name}" event="create" class="pull-right">
                New reagent design
            </stripes:link>
        </p>

        <div class="clearfix"></div>

        <table id="productList" class="table simple">
            <thead>
                <tr>
                    <th>Design Name</th>
                    <th>Reagent Type</th>
                    <th>Target Set</th>
                    <th>Manufacturer</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${actionBean.allReagentDesigns}" var="design">
                    <tr>
                        <td>
                            <stripes:link beanclass="${actionBean.class.name}" event="edit">
                                <stripes:param name="businessKey" value="${design.businessKey}"/>
                                ${design.designName}
                            </stripes:link>
                        </td>
                        <td>${design.reagentType}</td>
                        <td>${design.targetSetName}</td>
                        <td>${design.manufacturersName}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </stripes:layout-component>
</stripes:layout-render>
