<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.login.SecurityActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Mercury" sectionTitle="Sign In">
    <stripes:layout-component name="content">
        <div class="row-fluid">
            <div class="span3">
                <img src="${ctxpath}/images/mercury_logo.png" alt="Broad Institute" style="float: left;"/>
            </div>
            <div class="span9">
                <stripes:form id="form" action="/security/security.action" class="form-horizontal">
                    <div class="control-group">
                        <div class="controls">
                            <p>Please enter your Broad Username and Password.</p>
                        </div>
                    </div>

                    <div class="control-group">

                        <label class="ui-outputlabel control-label" for="username">Username *</label>
                        <div class="controls">
                            <p:focus />
                            <stripes:text id="username" name="username" value="${actionBean.username}" />
                        </div>
                    </div>

                    <div class="control-group">
                        <label class="ui-outputlabel control-label" for="password">Password *</label>
                        <div class="controls">
                            <stripes:password id="password" name="password" value="${actionBean.password}" />
                        </div>
                    </div>

                    <div class="control-group">
                        <div class="controls">
                            <div class="row-fluid">
                                <div class="span1">
                                    <stripes:submit value="Sign in" name="signIn" class="btn btn-primary"/>
                                </div>
                            </div>
                        </div>
                    </div>
                </stripes:form>
            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>

