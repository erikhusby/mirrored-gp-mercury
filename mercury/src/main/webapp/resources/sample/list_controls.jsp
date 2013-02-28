<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.sample.CollaboratorControlsActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.CollaboratorControlsActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="List Controls" sectionTitle="List Controls">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {

                $j('#positiveControlList').dataTable({
                                                         "oTableTools":ttExportDefines,
                                                         "aaSorting":[
                                                             [1, 'desc']
                                                         ],
                                                         "aoColumns":[
                                                             {"bSortable":true}//Collaborator Sample ID
                                                         ]
                                                     });

                $j('#negativeControlList').dataTable({
                                                         "oTableTools":ttExportDefines,
                                                         "aaSorting":[
                                                             [1, 'desc']
                                                         ],
                                                         "aoColumns":[
                                                             {"bSortable":true}//Collaborator Sample ID
                                                         ]
                                                     });

            });


        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <p>
            <stripes:link title="<%=CollaboratorControlsActionBean.CREATE_CONTROL%>"
                          beanclass="${actionBean.class.name}" event="create" class="pull-right">
                <span class="icon-shopping-cart"></span> <%=CollaboratorControlsActionBean.CREATE_CONTROL%>
            </stripes:link>
        </p>

        <div class="clearfix"></div>

        <div class="borderHeader">
               Positive Controls
        </div>

        <table id="positiveControlList" class="table simple">
            <thead>
            <tr>
                <th>Collaborator Sample ID</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${actionBean.positiveControls}" var="positiveControl">
                <tr>
                    <td>
                        <stripes:link beanclass="${actionBean.class.name}" event="view">
                            <stripes:param name="${actionBean.controlParameter}"
                                           value="${positiveControl.businessKey}"/>
                            ${positiveControl.businessKey}
                        </stripes:link>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>

        <div class="borderHeader">
               Negative Controls
        </div>

        <table id="negativeControlList" class="table simple">
            <thead>
            <tr>
                <th>Collaborator Sample ID</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${actionBean.negativeControls}" var="negativeControl">
                <tr>
                    <td>
                        <stripes:link beanclass="${actionBean.class.name}" event="view">
                            <stripes:param name="${actionBean.controlParameter}"
                                           value="${negativeControl.businessKey}"/>
                            ${negativeControl.businessKey}
                        </stripes:link>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>


    </stripes:layout-component>
</stripes:layout-render>
