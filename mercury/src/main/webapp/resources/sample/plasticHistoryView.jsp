<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.PlasticHistoryViewActionBean"/>

<stripes:layout-render name="/search/vessel_list.jsp" vessels="${actionBean.plasticHistory}"
                       bean="${actionBean}" showCheckboxes="false"/>