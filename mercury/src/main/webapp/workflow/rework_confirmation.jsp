<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Confirm Rework Addition" sectionTitle="Confirm Rework">
    <stripes:layout-component name="extraHead">

    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}">
            <stripes:hidden name="selectedLcset"/>
            <stripes:hidden name="selectedBucket"/>
            <stripes:hidden name="selectedReworks"/>
            <stripes:submit name="reworkConfirmed" id="confirmBtn" value="Confirm" class="btn btn-primary"/>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
