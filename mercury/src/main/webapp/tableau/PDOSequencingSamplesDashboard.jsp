<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="action" beanclass="org.broadinstitute.gpinformatics.mercury.presentation.TableauRedirectActionBean"/>

<html lang="en-US">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="refresh" content="1;url=${action.pdoSequencingSamplesDashboardUrl}">
    <script type="text/javascript">
        window.location.href = "${action.pdoSequencingSamplesDashboardUrl}";
    </script>
    <title>Page Redirection</title>
</head>
<body>
Go to <a href="${action.pdoSequencingSamplesDashboardUrl}">Tableau</a> if you are not redirected automatically.
</body>
</html>