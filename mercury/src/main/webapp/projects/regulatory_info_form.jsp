<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.RegulatoryInfoActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix="d-stripes" uri="http://stripes.sourceforge.net/stripes-dynattr.tld" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.RegulatoryInfoActionBean"/>

<c:if test="${actionBean.regulatoryInformationNew}">
    <p id="addRegInfoInstructions" style="font-weight: bold">
        Please contact the ORSP staff (<a href="mailto:orsp@broadinstitute.org" style="font-size: 12px;">orsp@broadinstitute.org</a>) for assistance with setting up an ORSP project. This will allow Mercury to automatically fetch the details instead of having them manually entered here. In the future, you will not be able to manually add regulatory information in Mercury. For now, you may still fill in the details below to add new regulatory information to Mercury and this research project, however this is not recommended.
    </p>
</c:if>

<stripes:form id="regulatoryInfoCreateForm" beanclass="${actionBean.class.name}" class="form-horizontal">
    <stripes:hidden name="researchProjectKey" value="${actionBean.researchProject.jiraTicketKey}"/>

    <%-- regulatoryInfoId will be set if editing an existing regulatory info --%>
    <stripes:hidden id="editRegulatoryInfoId" name="regulatoryInfoId"/>

    <%-- Identifier --%>
    <div class="control-group view-control-group">
        <label class="control-label">Identifier</label>

        <div class="controls">
            <div id="identifierDisplay" class="form-value">${actionBean.regulatoryInfoIdentifier}</div>
            <c:if test="${actionBean.regulatoryInformationNew}">
                <stripes:hidden id="identifier" name="regulatoryInfoIdentifier" value="${actionBean.regulatoryInfoIdentifier}"/>
            </c:if>
        </div>
    </div>

    <%-- Type --%>
    <c:choose>
        <c:when test="${actionBean.regulatoryInformationNew}">
            <div id="regInfoTypeEdit" class="control-group">
                <stripes:label for="regulatoryInfoType" class="control-label">Type</stripes:label>
                <div class="controls">
                    <stripes:select id="regulatoryInfoType" name="regulatoryInfoType">
                        <c:forEach items="${actionBean.allTypes}" var="type">
                            <stripes:option value="${type}" label="${type.name}" disabled="${actionBean.isTypeInUseForIdentifier(type)}"/>
                        </c:forEach>
                    </stripes:select>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div id="regInfoTypeEdit" class="control-group view-control-group">
                <label class="control-label">Type</label>

                <div class="controls">
                    <div id="typeDisplay" class="form-value">${actionBean.regulatoryInfoType.name}</div>
                </div>
            </div>
        </c:otherwise>
    </c:choose>

    <%-- Protocol Title --%>
    <div class="control-group">
        <stripes:label for="alias" class="control-label">Protocol Title</stripes:label>
        <div class="controls">
            <d-stripes:text id="titleInput" name="regulatoryInfoAlias" required="required"/>
            <span class="help-inline">
                <i class="icon-question-sign" title="Examples" rel="popover" data-trigger="hover" data-placement="right" data-html="true"
                   data-content="<ul><li>NHGRI Medical Sequencing Program</li><li>Exome Sequencing of Vienna Colorectal Cancer Cohort</li><li>Genetics of Epilepsy and Related Disorders</li></ul>"></i>
            </span>
            <p id="titleValidationError"></p>
        </div>
    </div>

    <%-- Submit Buttons --%>
    <div class="control-group">
        <div class="controls">
            <c:choose>
                <c:when test="${actionBean.regulatoryInformationNew}">
                    <stripes:submit id="addNewSubmit" name="<%= RegulatoryInfoActionBean.ADD_NEW_REGULATORY_INFO_ACTION %>" value="Add" class="btn btn-primary"/>
                    <%-- Hidden action is needed because the form is being submitted via AJAX and form serialization doesn't include submit buttons. --%>
                    <input type="hidden" name="<%= RegulatoryInfoActionBean.ADD_NEW_REGULATORY_INFO_ACTION %>">
                </c:when>
                <c:otherwise>
                    <stripes:submit id="editSubmit" name="<%= RegulatoryInfoActionBean.EDIT_REGULATORY_INFO_ACTION %>" value="Save" class="btn btn-primary"/>
                    <%-- Hidden action is needed because the form is being submitted via AJAX and form serialization doesn't include submit buttons. --%>
                    <input type="hidden" name="<%= RegulatoryInfoActionBean.EDIT_REGULATORY_INFO_ACTION %>">
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</stripes:form>

<script type="text/javascript">
    $j('i.icon-question-sign').popover();

    function validateTitle(event) {
        event.preventDefault();
        var $form = $j(this);
        $j.ajax({
            url: '${ctxpath}/projects/regulatoryInfo.action',
            data: {
                '<%= RegulatoryInfoActionBean.VALIDATE_TITLE_ACTION %>': '',
                regulatoryInfoId: $j('#editRegulatoryInfoId').val(),
                regulatoryInfoAlias: $j('#titleInput').val()
            },
            dataType: 'text',
            success: function handleTitleValidation(result) {
                if (result) {
                    $j('#titleValidationError').text(result);
                } else {
                    $j.ajax({
                        url: $form.attr('action'),
                        method: $form.attr('method'),
                        data: $form.serialize(),
                        success: successfulAddCallback,
                        error: handleRegInfoAjaxError
                    })
                }
            }
        });
    }

    $j('#regulatoryInfoCreateForm').submit(validateTitle);
</script>