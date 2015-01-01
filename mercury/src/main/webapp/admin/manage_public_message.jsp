<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.admin.PublicMessageAdminActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Mercury Public Message"
                       sectionTitle="Manage Public Message">
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}">
            <div class="control-group">
                <stripes:label for="message" class="control-label">Public Message Text:</stripes:label>
                <ul>
                    <li>Message can contain html markup.</li>
                </ul>
                <div class="controls">
                    <stripes:textarea style="width: 300px"  rows="5" name="messageText"/>
                </div>
            </div>
            <div class="actionButtons">
                <stripes:submit name="setMessage" value="Set Message" class="btn"/>
                <stripes:submit name="clearMessage" value="Clear Message" class="btn"/>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
