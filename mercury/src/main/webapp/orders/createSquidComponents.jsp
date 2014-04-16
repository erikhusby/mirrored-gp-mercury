<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.orders.SquidComponentActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.SquidComponentActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Define Squid Artifacts: ${actionBean.sourceOrder.title}"
                       sectionTitle="Define Squid Artifacts: ${actionBean.sourceOrder.title}">

    <stripes:layout-component name="extraHead">
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="buildSquidComponentForm">
            <div class="form-horizontal span6">
                <stripes:hidden name="<%= ProductOrderActionBean.PRODUCT_ORDER_PARAMETER%>" />
                <stripes:hidden name="<%= SquidComponentActionBean.BUILD_SQUID_COMPONENTS%>" />
                <div class="control-group">
                    <stripes:label for="initiativeSelect" class="control-label">
                        Initiative
                    </stripes:label>
                </div>
                <div class="controls">
                    <stripes:select name="autoSquidDto.initiative" id="initiativeSelect">
                        <stripes:option label="Select an initiative.." value="-1"/>
                    </stripes:select>
                </div>

                <div class="control-group">
                    <stripes:label for="projectTypeSelect" class="control-label">
                        Project type
                    </stripes:label>
                </div>
                <div class="controls">
                    <stripes:select name="autoSquidDto.projectType" id="projectTypeSelect">
                        <stripes:option label="Select a project Type.." value="-1"/>
                    </stripes:select>
                </div>

                <div class="control-group">
                    <stripes:label for="fundingSourceSelect" class="control-label">
                        Funding source
                    </stripes:label>
                </div>
                <div class="controls">
                    <stripes:select name="autoSquidDto.fundingSource" id="fundingSourceSelect">
                        <stripes:option label="Select a fundingSource.." value="-1"/>
                    </stripes:select>
                </div>

                <p/>
                <p/>
                <p/>

                <div class="control-group">
                    <stripes:label for="workRequestTypeSelect" class="control-label">
                        Work request type
                    </stripes:label>
                </div>
                <div class="controls">
                    <stripes:select name="autoSquidDto.workRequestType" id="workRequestTypeSelect">
                        <stripes:option label="Select a work request type.." value="-1" />
                    </stripes:select>
                </div>

                <div class="control-group">
                    <stripes:label for="analysisTypeSelect" class="control-label">
                        Analysis type
                    </stripes:label>
                </div>
                <div class="controls">
                    <stripes:select name="autoSquidDto.analysisType" id="analysisTypeSelect">
                        <stripes:option label="Select an analysis type.." value="-1" />
                    </stripes:select>
                </div>

                <div class="control-group">
                    <stripes:label for="referenceSeqSelect" class="control-label">
                        Reference sequence
                    </stripes:label>
                </div>
                <div class="controls">
                    <stripes:select name="autoSquidDto.referenceSequence" id="referenceSeqSeelct">
                        <stripes:option label="Select a reference sequence.." value="-1" />
                    </stripes:select>
                </div>

                <div class="control-group">
                    <stripes:label for="pairedRadio" class="control-label">
                        Paired sequencing
                    </stripes:label>
                </div>
                <div class="controls">
                    <stripes:radio value="" name=""
                </div>

            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>    