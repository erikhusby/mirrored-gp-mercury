<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.receiving.ReceiveExternalActionBean" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.ReceiveExternalActionBean"/>

<stripes:form beanclass="${actionBean.class.name}"  partial="true">
    <div class="control-group">
        <stripes:label for="site" name="Site" class="control-label"/>
        <div class="controls">
            <stripes:select name="siteId" id="site">
                <stripes:options-collection collection="${actionBean.sites}"
                                            label="name" value="id"/>
            </stripes:select>
        </div>
    </div>
</stripes:form>