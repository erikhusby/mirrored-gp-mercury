<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>

<%@ taglib uri="http://mercury.broadinstitute.org/Mercury/security" prefix="security" %>
<%@ taglib uri="https://mercury.broadinstitute.org/Mercury/mercuryStatic" prefix="mercuryStatic" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core"          prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"           prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions"     prefix="fn" %>

<%@ taglib prefix="enhance" uri="http://pukkaone.github.com/jsp" %>
<%@ taglib uri="http://www.owasp.org/index.php/Category:OWASP_CSRFGuard_Project/Owasp.CsrfGuard.tld" prefix="csrf" %>

<fmt:setBundle basename="MercuryStripesResources" var="resources"/>

<c:set var="ctxpath" value="${pageContext.request.contextPath}"/>
