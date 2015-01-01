
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<%@ page import="org.broadinstitute.bsp.client.rackscan.RackScanner" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.sample.PicoDispositionActionBean" %>

<%-- Allows selecting a rack scan lab, rack scanner device, and user-input simulated rack scan file. --%>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.PicoDispositionActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Confirm Rearray via Rack Scan"
                       sectionTitle="Confirm Rearray via Rack Scan" showCreate="false">

    <stripes:layout-component name="extraHead">
        <%@ include file="/vessel/rack_scanner_list_with_sim_part1.jsp" %>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="selectionForm" class="form-horizontal">
            <div class="form-horizontal">

                <!-- User must declare which Next Step value all the scanned tubes should have. -->
                <div class="control-group">
                    <stripes:label for="nextStepSelect" class="control-label">Expected Next Step</stripes:label>
                    <div class="controls">
                        <stripes:select name="nextStepSelect" id="nextStepSelect" value="${actionBean.nextStepSelect}">
                            <stripes:option value="" label="Select One" />
                            <stripes:options-collection collection="${actionBean.confirmableNextSteps}"
                                                        label="stepName" value="stepName"/>
                        </stripes:select>
                    </div>
                </div>
                <!-- Adds the dropdowns for lab and scanner, and possibly a file chooser. -->
                <stripes:layout-render name="/vessel/rack_scanner_list_with_sim_part2.jsp" bean="${actionBean}"/>

                <div class="controls">
                    <stripes:submit value="Scan" id="scanBtn" class="btn btn-primary"
                                    name="<%= PicoDispositionActionBean.CONFIRM_REARRAY_SCAN_EVENT %>"/>
                </div>
            </div>

        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
