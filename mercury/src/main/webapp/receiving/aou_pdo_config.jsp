<%@ include file="/resources/layout/taglibs.jsp" %>

<%--
  ~ The Broad Institute
  ~ SOFTWARE COPYRIGHT NOTICE AGREEMENT
  ~ This software and its documentation are copyright 2020 by the
  ~ Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support
  ~ whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  ~ use, misuse, or functionality.
  --%>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.AouPdoConfigActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="AoU PDO Parameters" sectionTitle="AoU PDO Parameters">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#reagentList').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aaSorting": [0,'asc'],
                    "aoColumns": [
                        {"bSortable": true},   // parameter name
                        {"bSortable": false},  // wgs value
                        {"bSortable": false},  // array value
                    ]
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="theForm">
            <table id="attributeList" class="table simple">
                <thead>
                <tr>
                    <th width="20%">Parameter</th>
                    <th>WGS orders</th>
                    <th>Array orders</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.dtos}" var="dto" varStatus="item">
                    <tr>
                        <td>${dto.paramName}</td>
                        <td>
                            <input style="width: 98%" type="text" name="dtos[${item.index}].wgsValue" value="${dto.wgsValue}"/>
                        </td>
                        <td>
                            <input style="width: 98%" type="text" name="dtos[${item.index}].arrayValue" value="${dto.arrayValue}"/>
                        </td>
                    </tr>
                    <input type="hidden" name="dtos[${item.index}].paramName" value="${dto.paramName}"/>
                </c:forEach>
                </tbody>
            </table>
            <div class="form-horizontal span24">
                <div class="control-group">
                    <stripes:label for="billingTriggerMapping" name="Billing Trigger" class="control-label"/>
                    <div class="controls">
                        <stripes:select multiple="true" id="billingTriggerMapping" name="billingTriggerMapping"
                                        value="${actionBean.billingTrigger.billingTriggerAttribute.name()}">
                            <stripes:options-enumeration label="displayName"
                                                         enum="org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject.BillingTrigger"/>
                        </stripes:select>
                    </div>
                </div>

                <div class="control-group">
                <div class="control-label">&#160;</div>
                <div class="controls actionButtons">
                    <stripes:submit name="validate" value="Validate" title="Validates all values."/>
                    <stripes:submit name="save" value="Save" title="Saves the values. If there are validation errors the checkbox must be checked in order to do the save."/>
                    &MediumSpace;<input type="checkbox" name="saveDespiteErrors"/>&MediumSpace;Ignore validation errors.
                </div>
            </div>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
