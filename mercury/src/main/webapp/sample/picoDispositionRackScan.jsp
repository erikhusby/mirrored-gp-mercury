<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.PicoDispositionActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.pageTitle}" sectionTitle="${actionBean.pageTitle} from Rack Scan" showCreate="false">
    <stripes:layout-component name="extraHead"/>
    <stripes:layout-component name="content">
        <jsp:include page="${actionBean.rackScanPageJsp}" />
    </stripes:layout-component>
</stripes:layout-render>
