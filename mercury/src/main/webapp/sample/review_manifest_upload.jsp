<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="" sectionTitle="" showCreate="false">

    <stripes:layout-component name="extraHead">
    </stripes:layout-component>

    <stripes:layout-component name="content">
        Here is your manifest!
    </stripes:layout-component>
</stripes:layout-render>