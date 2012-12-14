<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean"/>

<stripes:layout-definition>

    <?xml version="1.0" encoding="UTF-8"?>
    <!--
    ~ The Broad Institute
    ~ SOFTWARE COPYRIGHT NOTICE AGREEMENT
    ~ This software and its documentation are copyright 2012 by the
    ~ Broad Institute/Massachusetts Institute of Technology. All rights are
    ~ reserved.
    ~
    ~ This software is supplied without any warranty or guaranteed support
    ~ whatsoever. Neither the Broad Institute nor MIT can be responsible for its
    ~ use, misuse, or functionality.
    -->
    <!DOCTYPE html>
    <html lang="en">

    <head>
        <meta http-equiv="content-type" content="text/html; charset=iso-8859-1"/>
        <meta http-equiv="Content-Language" content="en"/>
        <link rel="Shortcut Icon" type="image/x-icon" href="${ctxpath}/favicon.ico"/>
        <link rel="stylesheet" type="text/css" href="${ctxpath}/resources/css/bootstrap.css"/>
        <link rel="stylesheet" type="text/css" href="${ctxpath}/resources/css/styles.css"/>

        <script type="text/javascript" src="${ctxpath}/resources/scripts/jquery-1.8.3.min.js"></script>
        <script type="text/javascript"> var $j = jQuery.noConflict(); </script>

        <script type="text/javascript" src="${ctxpath}/resources/scripts/Bootstrap/bootstrap-dropdown.js"></script>

        <script type="text/javascript" src="${ctxpath}/resources/scripts/jquery.gpUseful-1.0.js"></script>
        <script type="text/javascript" src="${ctxpath}/resources/scripts/DataTables-1.9.4/media/js/jquery.dataTables.min.js"></script>
        <script type="text/javascript" src="${ctxpath}/scripts/json.js"></script>

        <script type="text/javascript"
                src="http://prodinfojira.broadinstitute.org/jira/s/en_US-vrke9z/733/4/1.2.5/_/download/batch/com.atlassian.jira.collector.plugin.jira-issue-collector-plugin:issuecollector/com.atlassian.jira.collector.plugin.jira-issue-collector-plugin:issuecollector.js?collectorId=ad2bd4e3"></script>
        <script type="text/javascript">window.ATL_JQ_PAGE_PROPS = {
            "triggerFunction":function (showCollectorDialog) {
                jQuery("#jiraProblem").click(function (e) {
                    e.preventDefault();
                    showCollectorDialog();
                });
            }};
        </script>

        <script>
            $(document).ready(function () {
                $j('.dropdown-toggle').dropdown();
            });
        </script>
        <title>Mercury | ${pageTitle}</title>
    </head>
    <body>
            <div class="container-fluid">
                <div class="row-fluid">
                    <div class="brand" style="display:inline;">
                        <img src="${ctxpath}/images/broad_logo.png" alt="Broad Institute"/>
                        <a href="/index"
                           style="padding-left: 30px;text-decoration: none; font-variant: small-caps; font-size: 3em">
                            <img src="${ctxpath}/images/mercury_helmet_${actionBean.buildInfoBean.deployment}.png" alt="Mercury Helmet" width="40" height="30"/> Mercury</a>
                    </div>
                    <div id="navbarForm" class="nav pull-right">
                                <span id="jiraProblem" class="badge" style="cursor: pointer;"
                                      title="Click here to send a bug report or feedback">Feedback</span>

                        <!-- security-isLoggedIn" -->
                            |
                             <span id="userBadge" class="badge ${actionBean.userBean.badgeClass}" style="cursor: help;"
                                   title="${actionBean.userBean.bspStatus} ${actionBean.userBean.jiraStatus}  ${actionBean.userBean.rolesString}">${actionBean.userBean.loginUserName}</span>


                            &#160;
                            <a href="${ctxpath}/logout" value="Sign out" class="btn btn-mini">Sign out</a>
                    </div>
                </div>
            </div>

            <nav class="row-fluid">
                <stripes:layout-component name="menu">
                    <jsp:include page="/navigation.jsp"/>
                </stripes:layout-component>
            </nav>

            <div class="row-fluid">
        <!-- show messages/errors -->
        <p>
            <stripes:errors/>
            <stripes:messages/>
        </p>

        <section>
            <stripes:layout-component name="content"/>
        </section>

    </div>
    </div>
    <footer>
        <p>Copyright © 2012 Eli and Edythe L. Broad Institute. All rights reserved. No unauthorized use or
            disclosure is permitted.<br/>
            Genomics Platform. ${actionBean.buildInfoBean.buildInformation}. Deployment
            - ${actionBean.buildInfoBean.deployment}.</p>
    </footer>


    </html>

</stripes:layout-definition>