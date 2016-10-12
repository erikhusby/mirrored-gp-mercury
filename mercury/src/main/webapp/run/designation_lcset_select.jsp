<%-- Prompts user to select the correct lcset for a loading tube, as part of the process of creating designation tubes. --%>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationActionBean"/>

<%@ include file="designation_lcset_select_include.jsp" %>
