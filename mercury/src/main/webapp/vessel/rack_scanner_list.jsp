<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<%-- Specifically not using use action bean, as the class named is abstract and this JSP is meant to be used by multiple
     action beans. --%>
<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean"--%>

<stripes:form beanclass="${actionBean.class.name}" id="scannerForm" class="form-horizontal" onsubmit="return false;">
    <div class="form-horizontal">
        <div class="control-group">
            <stripes:label for="rackScanner" class="control-label">Rack Scanner</stripes:label>
            <div class="controls">
                <stripes:select name="rackScanner" id="rackScanner">
                    <stripes:option value="" label="Select One" />
                    <stripes:options-collection collection="${actionBean.rackScanners}" label="scannerName" value="name"/>
                </stripes:select> &#160; &#160;
                <stripes:button value="Scan" name="scan" class="btn" />
            </div>
        </div>
    </div>
</stripes:form>
