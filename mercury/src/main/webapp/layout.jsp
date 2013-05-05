<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="bean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean"/>

<stripes:layout-definition>

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
        <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1"/>
        <meta http-equiv="Content-Language" content="en"/>

        <script type="text/javascript" src="${ctxpath}/resources/scripts/jquery-1.8.3.min.js"></script>
        <script type="text/javascript"> var $j = jQuery.noConflict(); </script>

        <script type="text/javascript" src="${ctxpath}/resources/scripts/jquery-ui-1.9.2.custom.min.js"></script>

        <script type="text/javascript" src="${ctxpath}/resources/scripts/Bootstrap/bootstrap-dropdown.js"></script>
        <script type="text/javascript" src="${ctxpath}/resources/scripts/Bootstrap/bootstrap-tooltip.js"></script>
        <script type="text/javascript" src="${ctxpath}/resources/scripts/Bootstrap/bootstrap-popover.js"></script>
        <script type="text/javascript" src="${ctxpath}/resources/scripts/Bootstrap/bootstrap-alert.js"></script>

        <script type="text/javascript" src="${ctxpath}/resources/scripts/jquery.dateRangeSelector.js"></script>
        <script type="text/javascript"
                src="${ctxpath}/resources/scripts/DataTables-1.9.4/media/js/jquery.dataTables.min.js"></script>
        <script type="text/javascript"
                src="${ctxpath}/resources/scripts/DataTables-1.9.4/extras/TableTools/media/js/TableTools.min.js"></script>
        <script type="text/javascript" src="${ctxpath}/resources/scripts/bootstrap-dt.js"></script>
        <script type="text/javascript" src="${ctxpath}/resources/scripts/json.js"></script>

        <script type="text/javascript" src="${ctxpath}/resources/scripts/jquery.tokeninput-1.6.0.js"></script>
        <script type="text/javascript" src="${ctxpath}/resources/scripts/jquery.autosize-min.js"></script>


        <link rel="Shortcut Icon" type="image/x-icon" href="${ctxpath}/favicon.ico"/>

        <link rel="stylesheet" type="text/css" href="http://fonts.googleapis.com/css?family=Carrois+Gothic+SC"/>
        <link rel="stylesheet" type="text/css" href="${ctxpath}/resources/css/bootstrap.css"/>
        <link rel="stylesheet" type="text/css"
              href="${ctxpath}/resources/scripts/DataTables-1.9.4/media/css/jquery.dataTables.css"/>
        <link rel="stylesheet" type="text/css"
              href="${ctxpath}/resources/scripts/DataTables-1.9.4/extras/TableTools/media/css/TableTools.css"/>
        <link rel="stylesheet" type="text/css" href="${ctxpath}/resources/css/token-input.css"/>
        <link rel="stylesheet" type="text/css" href="${ctxpath}/resources/css/jquery-ui-1.9.2.custom.min.css"/>
        <link rel="stylesheet" type="text/css" href="${ctxpath}/resources/css/mercury.css"/>

        <script type="text/javascript" src="${ctxpath}/resources/scripts/jquery.gpUseful-1.0.js"></script>
        <script type="text/javascript" src="${ctxpath}/resources/scripts/jquery.heatcolor.0.0.1.js"></script>
        <script type="text/javascript" src="${ctxpath}/resources/scripts/jquery.sparkline.js"></script>

        <script type="text/javascript"
                src="https://gpinfojira.broadinstitute.org:8443/jira/s/en_US-vrke9z/733/4/1.2.5/_/download/batch/com.atlassian.jira.collector.plugin.jira-issue-collector-plugin:issuecollector/com.atlassian.jira.collector.plugin.jira-issue-collector-plugin:issuecollector.js?collectorId=ad2bd4e3"></script>
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
                $j('#userBadge').popover({ trigger:"hover", html:true });
                $j('.shiftCheckbox').enableCheckboxRangeSelection();

                $j(".defaultText").focus(clearOnFocus);
                $j(".defaultText").blur(updateActiveText);
                $j(".defaultText").change(updateActiveText);
                $j(".defaultText").blur();

                // The form submit needs to clear the fields
                $j('.defaultText').closest('form').submit(cleanUpDefaultText);

                // Default date range selector (if there is a dateRangeDiv, the action bean will HAVE to have this
                $j('#dateRangeDiv').dateRangeSelector();

                // add clear box to filter
                $j('.dataTables_filter input').clearable();
            });

            $j(function () {
                $j('.textarea').autosize();
            });

            function clearOnFocus(srcc) {
                if ($j(this).val() == $j(this)[0].title) {
                    $j(this).removeClass("defaultTextActive");
                    $j(this).val("");
                }
            }

            function cleanUpDefaultText() {
                jQuery(this).find('.defaultText').each(function () {
                    if (jQuery(this).val() == jQuery(this).attr('title')) {
                        jQuery(this).val('');
                    }
                });
            }

            function updateActiveText() {
                if ($j(this).val() == "") {
                    $j(this).addClass("defaultTextActive");
                    $j(this).val($j(this)[0].title);
                } else {
                    $j(this).removeClass("defaultTextActive");
                }
            }
        </script>
        <title>Mercury <c:if test="${pageTitle != null}">${pageTitle}</c:if></title>

        <stripes:layout-component name="extraHead"/>
    </head>
    <body>
    <div class="container-fluid">
        <div class="row-fluid">
            <div class="brand" style="display:inline;">
                <img src="${ctxpath}/images/broad_logo.png" alt="Broad Institute"/>
                <stripes:link beanclass="org.broadinstitute.gpinformatics.mercury.presentation.security.SecurityActionBean"
                              style="padding-left: 30px;text-decoration: none; font-family: 'Carrois Gothic SC', sans-serif; font-size: 2.2em;">
                    <img src="${ctxpath}/images/mercury_helmet_${bean.buildInfoBean.deployment}.png"
                         alt="Mercury Helmet" width="40" height="30"/> Mercury</stripes:link>
            </div>
            <div id="navbarForm" class="nav pull-right">
                        <span id="jiraProblem" class="badge" style="cursor: pointer;"
                              title="Click here to send a bug report or feedback">Feedback</span>

                <c:if test="${bean.context.username ne null}">
                    |
                         <span id="userBadge" class="badge ${bean.userBean.badgeClass}" style="cursor: help;"
                               data-original-title="Account Info" rel="popover" data-placement="bottom"
                               data-content="${bean.userBean.bspStatus}<br/>${bean.userBean.jiraStatus}<br/>${bean.userBean.rolesString}">${bean.userBean.loginUserName}</span>

                    &#160;
                    <stripes:link
                            beanclass="org.broadinstitute.gpinformatics.mercury.presentation.security.SecurityActionBean"
                            event="signOut" title="Sign out" class="btn btn-mini"
                            style="text-decoration: none !important">
                        Sign out
                    </stripes:link>
                </c:if>
            </div>
        </div>
    </div>

    <nav class="row-fluid">
        <stripes:layout-component name="menu">
            <c:if test="${bean.context.username ne null}">
                <jsp:include page="/navigation.jsp"/>
            </c:if>
        </stripes:layout-component>
    </nav>


    <div class="row-fluid">

        <section>
            <c:if test="${sectionTitle ne null}">
                <div class="page-header">
                    ${sectionTitle}
                    <c:if test="${not empty createTitle}">
                        <stripes:link beanclass="${actionBean.class.name}" event="create" title="${createTitle}" class="pull-right create">
                            ${createTitle}
                        </stripes:link>
                    </c:if>
                </div>
            </c:if>


            <!-- show messages/errors -->
            <p>
                <stripes:errors/>
                <stripes:messages/>
            </p>

            <div class="page-body">
                <stripes:layout-component name="content"/>
            </div>
        </section>

    </div>
    </body>

    <footer>
        <p>Copyright © 2012-2013 Eli and Edythe L. Broad Institute. All rights reserved. No unauthorized use or
            disclosure is permitted.<br/>
            Genomics Platform. ${bean.buildInfoBean.buildInformation}. Deployment
            - ${bean.buildInfoBean.deployment}.</p>
    </footer>

    </html>

</stripes:layout-definition>