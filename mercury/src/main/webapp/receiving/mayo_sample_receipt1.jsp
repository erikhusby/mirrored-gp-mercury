<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoSampleReceiptActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Mayo Sample Receipt" sectionTitle="Mayo Sample Receipt">
    <stripes:layout-component name="content">

        <%@ include file="/vessel/rack_scanner_list_with_sim_part1.jsp" %>

        <stripes:form beanclass="${actionBean.class.name}" id="rackScanForm" class="form-horizontal">
            <!-- Captures the rack barcode. -->
            <div class="control-group" style="padding-left: 75px">
                Rack Barcode:
                <span style="padding-left: 10px">
                    <stripes:text id="rackBarcode" name="rackBarcode"/>
                </span>
            </div>

            <!-- Selectors for the rack scan lab & scanner, and the Scan button. -->
            <div style="padding-top: 20px;">
                <stripes:layout-render name="/vessel/rack_scanner_list_with_sim_part2.jsp" bean="${actionBean}"/>

                <div class="controls">
                    <stripes:submit value="Rack Scan" id="scanBtn" name="scanBtn" class="btn btn-primary"/>
                </div>
            </div>

        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>