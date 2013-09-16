<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ReceiveSamplesActionBean"
                       var="actionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.pageTitle}" sectionTitle="${actionBean.pageTitle} By ID">
    <stripes:layout-component name="extraHead">
    </stripes:layout-component>
    <stripes:layout-component name="content">

        <stripes:form beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ReceiveSamplesActionBean"
                      id="bspSampleReceiptForm">
            <stripes:hidden name="" />

            <div class="form-horizontal">
                <div class="control-group">
                    <stripes:label for="labToFilterBy" class="control-label">BSP Sample IDs</stripes:label>
                    <div class="controls">
                        <stripes:textarea name="sampleIds" /><br />
                        <stripes:submit name="${actionBean.receiveSamplesEvent}" id="${actionBean.receiveSamplesEvent}"
                                        value="Receive BSP Samples" class="btn" />
                    </div>
                </div>
            </div>

        </stripes:form>

        <p>&#160;</p>

        <div class="page-header">
            <h3 style="display:inline;">${actionBean.pageTitle} by Rack Scan</h3>
        </div>

        <jsp:include page="${actionBean.rackScanPageJsp}" />

    </stripes:layout-component>
</stripes:layout-render>
