<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-definition>
    <stripes:useActionBean var="actionBean"
                           beanclass="org.broadinstitute.gpinformatics.athena.presentation.analysis.ManageAnalysisFieldsActionBean"/>

    <div id="alignerId">
        <div id="add">
            <stripes:form beanclass="${actionBean.class.name}" id="addForm" class="form-horizontal">
                <label for="alignerName">Aligner Name:</label><stripes:text name="alignerName"/>
                <label for="alignerVersion">Aligner Version:</label><stripes:text name="alignerVersion"/>
                <stripes:submit style="margin-top:4px;" name="addAligner" id="addAligner"
                                value="Add New Aligner"></stripes:submit>
            </stripes:form>
        </div>

        <table id="alignerData">
            <thead>
            <tr>
                <th></th>
                <th>Aligner</th>
            </tr>
            </thead>
            <tbody>"/>
            <c:forEach items="${actionBean.alignerList}" var="aligner">
                <tr>
                    <td><stripes:link
                            beanclass="org.broadinstitute.gpinformatics.mercury.presentation.analysis.ManageAnalysisFieldsActionBean"
                            event="deleteAligner">
                        <stripes:param name="id" value="${refSeq.id}"/>
                        X</stripes:link></td>
                    <td>${aligner.name}</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
    <script type="text/javascript">
        $j(document).ready(function () {
            // Add in the jquery data tables here.
        });
    </script>
</stripes:layout-definition>

