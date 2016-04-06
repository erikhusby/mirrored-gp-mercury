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

<stripes:layout-render name="/layout.jsp" pageTitle="Edit Genotyping Chip" sectionTitle="Edit Genotyping Chip">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#reagentList').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aaSorting": [0,'asc'],
                    "aoColumns": [
                        {"bSortable": true},
                        {"bSortable": false},
                    ]
                })
            });
        </script>
        <style type="text/css">
            input, select {
                -webkit-box-sizing: border-box;
                -moz-box-sizing: border-box;
                box-sizing: border-box;
            }
        </style>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="editForm">
            <div class="control-group">
                <stripes:label for="chipName" name="Chip Name" class="control-label"/>
                <div class="controls">
                    <stripes:text id="chipName" name="chipName" style="width:70%;height:auto">
                    ${actionBean.chipName}
                    </stripes:text>
                </div>
            </div>

            <table id="attributeList" class="table simple">
                <thead>
                <tr>
                    <th width="20%">Attribute Name</th>
                    <th width="80%">Value</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.attributes}" var="item" varStatus="itemStatus">
                    <tr>
                        <td width="20%">${item.attributeName}</td>
                        <td width="80%"><input style="width:100%" class="attributeValue"
                                               name="attributes[${itemStatus.index}].attributeValue"
                                               value="${item.attributeValue}"/></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>

            <div class="control-group">
            <div class="controls">
                <span><stripes:label for="overrideId" name="Override earlier versions?" class="control-label"/>
                        <stripes:checkbox id="overrideId" name="overrideEarlier" style="margin-top: 10px;"
                                          title="Check this if earlier versions should be inaccessible and old runs must use this version."/>
                </span>
                <div class="control-label">&#160;</div>
                <div class="controls actionButtons">
                    <stripes:submit name="list" value="Cancel"/>
                    <stripes:submit name="save" value="Save"/>
                </div>
            </div>

        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
