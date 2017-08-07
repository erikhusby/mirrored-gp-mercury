<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.container.ContainerActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Create Container" sectionTitle="Create Container">

    <stripes:layout-component name="extraHead">
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="createContainerForm" class="form-horizontal">
            <div class="control-group">
                <stripes:label for="containerBarcode" class="control-label"/>
                <div class="controls">
                    <stripes:text id="containerBarcode" name="containerBarcode"/>
                </div>
            </div>
            <div class="control-group">
                <stripes:label for="storageUnitTypeName" name="Unit Type" class="control-label"/>
                <div class="controls">
                    <stripes:select id="containerType" name="rackType">
                        <stripes:options-enumeration
                                enum="org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes.RackType"
                                label="displayName"/>
                    </stripes:select>
                </div>
            </div>
            <div class="control-group">
                <div class="controls">
                    <stripes:submit id="createContainer" name="createContainer" value="Create" class="btn btn-primary"/>
                </div>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>