<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%--@elvariable id="pageTitle" type="java.lang.String"--%>
<%--@elvariable id="sectionTitle" type="java.lang.String"--%>
<%--@elvariable id="businessKeyValue" type="java.lang.String"--%>
<%--@elvariable id="showCreate" type="java.lang.Boolean"--%>
<%--@elvariable id="buildInfoBean" type="org.broadinstitute.gpinformatics.athena.boundary.BuildInfoBean"--%>
<%--@elvariable id="userBean" type="org.broadinstitute.gpinformatics.mercury.presentation.UserBean"--%>
<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean"--%>
<%--@elvariable id="dataTablesVersion" type="java.lang.String"--%>
<%--@elvariable id="withColVis" type="java.lang.Boolean"--%>
<%--@elvariable id="withColReorder" type="java.lang.Boolean"--%>

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

        <script src="${ctxpath}/resources/scripts/jquery-1.10.1.min.js"></script>
        <script type="text/javascript"> var $j = jQuery.noConflict(); </script>
        <script src="${ctxpath}/resources/scripts/jquery-ui-1.9.2.custom.min.js"></script>
        <script src="${ctxpath}/resources/scripts/Bootstrap/bootstrap-dropdown.js"></script>
        <script src="${ctxpath}/resources/scripts/Bootstrap/bootstrap-tooltip.js"></script>
        <script src="${ctxpath}/resources/scripts/Bootstrap/bootstrap-popover.js"></script>
        <script src="${ctxpath}/resources/scripts/Bootstrap/bootstrap-alert.js"></script>

        <script src="${ctxpath}/resources/scripts/jquery.dateRangeSelector.js"></script>

        <c:choose>
            <c:when test="${dataTablesVersion == '1.10'}">
                <script src="${ctxpath}/resources/scripts/DataTables-1.10.12/js/jquery.dataTables.min.js"></script>
                <link rel="stylesheet"
                      href="${ctxpath}/resources/scripts/DataTables-1.10.12/css/jquery.dataTables.min.css"/>
                <link rel="stylesheet"
                      href="${ctxpath}/resources/scripts/DataTables-1.10.12/css/dataTables.bootstrap.min.css"/>
                <script type="text/javascript"
                        src="${ctxpath}/resources/scripts/DataTablesPlugins-1.10/JSZip-2.5.0/jszip.min.js"></script>
                <script type="text/javascript"
                        src="${ctxpath}/resources/scripts/DataTablesPlugins-1.10/pdfmake-0.1.18/build/pdfmake.min.js"></script>
                <script type="text/javascript"
                        src="${ctxpath}/resources/scripts/DataTablesPlugins-1.10/pdfmake-0.1.18/build/vfs_fonts.js"></script>
                <script type="text/javascript"
                        src="${ctxpath}/resources/scripts/DataTablesPlugins-1.10/Buttons-1.2.2/js/dataTables.buttons.min.js"></script>
                <script type="text/javascript"
                        src="${ctxpath}/resources/scripts/DataTablesPlugins-1.10/Buttons-1.2.2/js/buttons.html5.min.js"></script>
                <script type="text/javascript"
                        src="${ctxpath}/resources/scripts/DataTablesPlugins-1.10/Buttons-1.2.2/js/buttons.print.min.js"></script>
                <link rel="stylesheet" type="text/css"
                      href="${ctxpath}/resources/scripts/DataTablesPlugins-1.10/Buttons-1.2.2/css/buttons.dataTables.min.css"/>
                <link rel="stylesheet" type="text/css"
                      href="${ctxpath}/resources/scripts/DataTablesPlugins-1.10/Buttons-1.2.2/css/buttons.bootstrap.min.css"/>
            </c:when>
            <c:otherwise>
                <script src="${ctxpath}/resources/scripts/DataTables-1.9.4/media/js/jquery.dataTables.min.js"></script>
                <link rel="stylesheet"
                      href="${ctxpath}/resources/scripts/DataTables-1.9.4/media/css/jquery.dataTables.css"/>
                <script src="${ctxpath}/resources/scripts/DataTables-1.9.4/media/js/jquery.dataTables.min.js"></script>
                <script src="${ctxpath}/resources/scripts/DataTables-1.9.4/extras/TableTools/media/js/TableTools.min.js"></script>
                <link rel="stylesheet"
                      href="${ctxpath}/resources/scripts/DataTables-1.9.4/extras/TableTools/media/css/TableTools.css"/>
                <script src="${ctxpath}/resources/scripts/DataTables-1.9.4/extras/RowGrouping/media/js/jquery.dataTables.rowGrouping.js"></script>
                <script src="${ctxpath}/resources/scripts/DataTables-1.9.4/extras/dataTables.fnSetFilteringDelay.js"></script>
                <script src="${ctxpath}/resources/scripts/DataTables-1.9.4/extras/fnGetHiddenNodes.js"></script>
            </c:otherwise>
        </c:choose>

        <c:if test="${withColVis}">
            <script src="${ctxpath}/resources/scripts/DataTablesPlugins-1.10/Buttons-1.2.2/js/buttons.colVis.min.js"></script>
            <link rel="stylesheet"  href="${ctxpath}/resources/css/mercury.colvis.css"/>
        </c:if>

        <c:if test="${withColReorder}">
            <script src="${ctxpath}/resources/scripts/DataTablesPlugins-1.10/ColReorder-1.3.2/js/dataTables.colReorder.min.js"></script>
            <link rel="stylesheet"
                  href="${ctxpath}/resources/scripts/DataTablesPlugins-1.10/ColReorder-1.3.2/css/colReorder.dataTables.min.css"/>
        </c:if>

        <script src="${ctxpath}/resources/scripts/bootstrap-dt.js"></script>
        <script src="${ctxpath}/resources/scripts/json2.js"></script>

        <script src="${ctxpath}/resources/scripts/jquery.tokeninput.min.js"></script>
        <script src="${ctxpath}/resources/scripts/jquery.autosize-min.js"></script>

        <link rel="Shortcut Icon" type="image/x-icon" href="${ctxpath}/favicon.ico"/>

        <link rel="stylesheet"  href="${ctxpath}/resources/css/bootstrap.css"/>
        <link rel="stylesheet"  href="${ctxpath}/resources/css/token-input.css"/>
        <link rel="stylesheet"  href="${ctxpath}/resources/css/jquery-ui-1.9.2.custom.min.css"/>
        <link rel="stylesheet"  href="${ctxpath}/resources/css/mercury.css"/>

        <script src="${ctxpath}/resources/scripts/jquery.gpUseful-1.0.js"></script>
        <script src="${ctxpath}/resources/scripts/jquery.heatcolor.0.0.1.js"></script>
        <script src="${ctxpath}/resources/scripts/jquery.sparkline-2.1.2.min.js"></script>
        <script src="${ctxpath}/resources/scripts/jquery.headerPersist.1.0.js"></script>

        <script src="${ctxpath}/Owasp.CsrfGuard/JavaScriptServlet"></script>

        <script src="https://gpinfojira.broadinstitute.org:8443/jira/s/en_US-vrke9z/733/4/1.2.5/_/download/batch/com.atlassian.jira.collector.plugin.jira-issue-collector-plugin:issuecollector/com.atlassian.jira.collector.plugin.jira-issue-collector-plugin:issuecollector.js?collectorId=ad2bd4e3"></script>

        <script>
            $j(document).ready(function () {
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

                // Capture session timeouts in ajax calls
                $j(document).ajaxSuccess(function(evt, request, settings){
                    if (request.responseText.indexOf('timeout_page_flag') != -1)
                        // Force signin to not forward to ajax request
                        location.href = "${ctxpath}/security/security.action?ajax=reset";
                });

                setupMercuryMessage();
                $j("#jiraProblem").click(function () {
                    window.open("https://gpinfojira.broadinstitute.org/jira/servicedesk/customer/portal/41");
                });
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

            function setupMercuryMessage() {
                $j.ajax({url: "${ctxpath}/public/public_message.action", success: function (message) {
                    if (message) {
                        $j("body").prepend('<div style="font-size: 14px; text-align: center; z-index: 10000000;" class="alert alert-danger" id="public-message">'+message+'</div>');
                        $j("body").headerPersist();
                    }
                }});
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
                    <img src="${ctxpath}/images/mercury_helmet_${buildInfoBean.deployment}.png"
                         alt="Mercury Helmet" width="40" height="30"/> Mercury</stripes:link>
            </div>
            <div id="navbarForm" class="nav pull-right">
                        <span id="jiraProblem" class="badge" style="cursor: pointer;"
                              title="Click here to send a bug report or feedback">Feedback</span>

                <c:if test="${userBean.loginUserName ne null}">
                    |
                         <span id="userBadge" class="badge ${userBean.badgeClass}" style="cursor: help;"
                               data-original-title="Account Info" rel="popover" data-placement="bottom"
                               data-content="<b class='${userBean.bspStatusClass}'>${userBean.bspStatus}</b><br/>
                               <b class='${userBean.jiraStatusClass}'>${userBean.jiraStatus}</b><br/>${userBean.rolesString}">${userBean.loginUserName}</span>

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
            <c:if test="${userBean.loginUserName ne null}">
                <jsp:include page="/navigation.jsp"/>
            </c:if>
        </stripes:layout-component>
    </nav>

    <div class="row-fluid">
        <section>
            <c:if test="${sectionTitle ne null}">
                <div class="page-header">
                    <h3 style="display:inline;">${sectionTitle}</h3>
                    <c:if test="${showCreate && actionBean.createAllowed}">
                        <stripes:link beanclass="${actionBean.class.name}" event="${actionBean.createAction}" title="Click to ${actionBean.createTitle}" class="pull-right">
                            <span class="icon-plus"></span>
                            ${actionBean.createTitle}
                        </stripes:link>
                    </c:if>

                    <c:if test="${not empty businessKeyValue && actionBean.editAllowed}">
                        <stripes:link beanclass="${actionBean.class.name}" event="${actionBean.editAction}" title="Click to ${actionBean.editTitle}" class="pull-right">
                            <stripes:param name="${actionBean.editBusinessKeyName}" value="${businessKeyValue}"/>
                            <span class="icon-pencil"></span>
                            ${actionBean.editTitle}
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
        <p>Copyright &copy; 2012<script>document.write("-"+new Date().getFullYear());</script> Broad Institute<br/>
            Genomics Platform. ${buildInfoBean.buildInformation}. Deployment
            - ${buildInfoBean.deployment}.</p>
    </footer>

    </html>

</stripes:layout-definition>
