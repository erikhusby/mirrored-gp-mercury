
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean" %>

<%-- Allows selecting a rack scan lab, rack scanner device, and user-input simulated rack scan file. --%>

<stripes:layout-render name="/layout.jsp" pageTitle="Review Pico Dispositions via Rack Scan"
                       sectionTitle="Review Pico Dispositions via Rack Scan" showCreate="false">

    <stripes:layout-component name="extraHead">
        <%@ include file="/vessel/rack_scanner_list_with_sim_part1.jsp" %>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="selectionForm" class="form-horizontal">
            <div class="form-horizontal">

                <!-- Adds the dropdowns for lab and scanner, and possibly a file chooser. -->
                <stripes:layout-render name="/vessel/rack_scanner_list_with_sim_part2.jsp" bean="${actionBean}"/>

                <div class="controls">
                    <stripes:submit value="Scan" id="scanBtn" class="btn btn-primary"
                                    name="<%= RackScanActionBean.SCAN_EVENT %>"/>
                </div>
            </div>

        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
