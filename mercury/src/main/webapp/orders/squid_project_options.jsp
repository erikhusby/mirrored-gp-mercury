<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.SquidComponentActionBean"/>
<stripes:form beanclass="${actionBean.class.name}">
    <div class="control-group">
        <stripes:label for="initiativeSelect" class="control-label">
            Initiative
        </stripes:label>
        <div class="controls">
            <stripes:select name="autoSquidDto.initiative" id="initiativeSelect">
                <stripes:option label="Select an initiative.." value="-1"/>
                <stripes:options-collection collection="${actionBean.squidProjectOptions.initiatives}" value="id"
                                            label="name"/>
            </stripes:select>
        </div>
    </div>

    <div class="control-group">
        <stripes:label for="projectTypeSelect" class="control-label">
            Project type
        </stripes:label>
        <div class="controls">
            <stripes:select name="autoSquidDto.projectType" id="projectTypeSelect">
                <stripes:option label="Select a project Type.." value="-1"/>
                <stripes:options-collection collection="${actionBean.squidProjectOptions.projectTypes}" value="id"
                                            label="name"/>
            </stripes:select>
        </div>
    </div>

    <div class="control-group">
        <stripes:label for="fundingSourceSelect" class="control-label">
            Funding source
        </stripes:label>
        <div class="controls">
            <stripes:select name="autoSquidDto.fundingSource" id="fundingSourceSelect">
                <stripes:option label="Select a fundingSource.." value="-1"/>
                <stripes:options-collection collection="${actionBean.squidProjectOptions.fundingSources}" value="id"
                                            label="name"/>
            </stripes:select>
        </div>
    </div>
</stripes:form>
