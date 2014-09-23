<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.security.SecurityActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Sign In" sectionTitle="Sign In">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {

                if ($j('#username').val() == '') {
                    $j('#username').focus();
                } else {
                    $j('#password').focus();
                }
            });
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <div class="row-fluid" id="mercury_login">
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

