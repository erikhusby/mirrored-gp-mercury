<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.security.AuthorizePageActionBean" />

<stripes:layout-render name="/layout.jsp" pageTitle="Page Authorization" sectionTitle="${actionBean.submitString}">

    <stripes:layout-component name="content">

        <div class="help-block">
            Enter the page that you wish to protect and then choose the roles that are allowed to use the page.
            THIS PAGE WILL NOT FUNCTION UNTIL THERE ARE REAL PAGE IDS AND THESE ARE STORED IN THE DB
        </div>

        <stripes:form beanclass="${actionBean.class.name}" id="createForm" class="form-horizontal">
            <div style="float: left; margin-right: 40px; margin-top: 5px;">
                <stripes:hidden id="authId" name="pageAuthorization.authorizationId" value="${actionBean.pageAuthorization.authorizationId}"/>

                <div class="control-group">
                    <stripes:label for="pathName" name="Path Name" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="pathName" name="pageAuthorization.pagePath" class="defaultText"
                                      title="Enter the path name" value="${actionBean.pageAuthorization.pagePath}"/>
                    </div>
                </div>

                <div class="control-group">
                    <div class="control-label">
                        Roles
                    </div>
                    <div class="controls">
                        <c:forEach items="${actionBean.pageAuthorization.roleList}" var="role" varStatus="i">
                            <stripes:checkbox id="role-${i.step}" name="pageAuthorization.roleList" checked="${actionBean.pageAuthorization.roleAccess}"/>
                            <stripes:label for="role-${i.step}" name="${role}"/>
                        </c:forEach>
                    </div>
                </div>

                <div class="control-group">
                    <div class="controls">
                        <div class="row-fluid">
                            <div class="span2">
                                <stripes:submit name="save" value="Save"/>
                            </div>
                            <div class="span1">
                                <c:choose>
                                    <c:when test="${actionBean.creating}">
                                        <stripes:link beanclass="${actionBean.class.name}" event="list">Cancel</stripes:link>
                                    </c:when>
                                    <c:otherwise>
                                        <stripes:link beanclass="${actionBean.class.name}" event="view">
                                            <stripes:param name="pageAuthorization.authorizationId" value="${actionBean.pageAuthorization.authorizationId}"/>
                                            Cancel
                                        </stripes:link>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>