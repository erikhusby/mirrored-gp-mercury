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

<stripes:layout-render name="/layout.jsp" pageTitle="Edit Genotyping Chip" sectionTitle="Edit Genotyping Chip">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#reagentList').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aaSorting": [0,'asc'],
                    "aoColumns": [
                        {"bSortable": true},   // attribute name
                        {"bSortable": false},  // attribute value
                    ]
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="editForm">
            <input type="hidden" name="submitString" value="${actionBean.submitString}"/>
            <input type="hidden" name="chipFamily" value="${actionBean.chipFamily}"/>

            <div class="control-group">
                <stripes:label for="nameId" name="Chip Name" class="control-label"/>
                <stripes:text id="nameId" name="saveChipName" style="width:70%;height:auto"
                              readonly="${actionBean.submitString.startsWith('Edit')}"/>
            </div>

            <table id="attributeList" class="table simple">
                <thead>
                <tr>
                    <th width="20%">Attribute Name</th>
                    <th width="80%">Value</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.attributes}" var="item">
                    <tr>
                        <td width="20%">${item.key}</td>
                        <td width="80%"><input style="width:99%" class="attributeValue" name="attributes[${item.key}]"
                                               value="${item.value}"/></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>

            <div class="control-group">
                <div class="control-label">&#160;</div>
                <div class="controls actionButtons">
                    <stripes:submit name="list" value="Cancel"/>
                    <stripes:submit name="save" value="Save"/>
                </div>
            </div>

        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
