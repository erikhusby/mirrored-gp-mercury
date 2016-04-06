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

    <script type="text/javascript">
        function familyChanged() {
            // Redisplays the list page for a different chipFamily.
            window.location.assign("${ctxpath}/genotyping/chipType.action?chipFamily=" + $j('#familySelect').val());
        }
    </script>


<stripes:layout-render name="/layout.jsp" pageTitle="List Genotyping Chips" sectionTitle="List Genotyping Chips" showCreate="true">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#reagentList').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[1,'asc'],[2,'asc']],
                    "aoColumns": [
                        {"bSortable": true}, //checkbox
                        {"bSortable": true}, //chip name
                        {"bSortable": true}, //created date
                    ]
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="selectionForm" class="form-horizontal">
            <div class="control-group">
                <stripes:label for="familySelect" name="Chip Family" class="control-label"/>
                <div class="controls">
                    <stripes:select id="familySelect" name="chipFamily" style="width:25%" onchange="familyChanged()">
                        <stripes:option label="Select a chip family..." value="-1" disabled="true" selected="selected"/>
                        <stripes:options-collection collection="${actionBean.chipFamilies}"/>
                    </stripes:select>
                </div>
            </div>

            <table id="chipList" class="table simple">
                <thead>
                <tr>
                    <th/>
                    <th>Chip</th>
                    <th>Created Date</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.chipTypes}" var="chip" varStatus="chipStatus">
                    <tr>
                        <td><stripes:radio name="chipRadio" value="${chip.archetypeName}"
                                   style="float:left;margin-right:5px;"/>
                        </td>
                        <td>${chip.archetypeName}</td>
                        <td>${chip.createdDate}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>

            <div class="control-group">
                <div class="control-label">&#160;</div>
                <div class="controls actionButtons">
                    <stripes:submit name="edit" value="Edit" style="margin-right: 10px;margin-top:10px;"/>
                    <stripes:submit name="create" value="Create" style="margin-right: 10px;margin-top:10px;"/>
                </div>
            </div>

        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
