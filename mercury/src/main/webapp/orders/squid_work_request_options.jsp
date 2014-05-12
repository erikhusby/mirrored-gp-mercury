<%@ include file="/resources/layout/taglibs.jsp" %>
<%--<stripes:layout-definition>--%>
    <stripes:useActionBean var="actionBean" beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.SquidComponentActionBean"/>

    <stripes:form beanclass="${actionBean.class.name}" partial="true">
        <div class="control-group">
            <stripes:label for="workRequestTypeSelect" class="control-label">
                Work request type
            </stripes:label>
            <div class="controls">
                <stripes:select name="autoSquidDto.workRequestType" id="workRequestTypeSelect">
                    <stripes:option label="Select a work request type.." value="-1" selected="selected"
                                    disabled="true"/>
                    <stripes:options-collection collection="${actionBean.workRequestOptions.workRequestTypes}"
                                                value="id" label="name"/>
                </stripes:select>
            </div>
        </div>

        <div class="control-group">
            <stripes:label for="analysisTypeSelect" class="control-label">
                Analysis type
            </stripes:label>
            <div class="controls">
                <stripes:select name="autoSquidDto.analysisType" id="analysisTypeSelect">
                    <stripes:option label="Select an analysis type.." value="-1" selected="selected" disabled="true"/>
                    <stripes:options-collection collection="${actionBean.workRequestOptions.analysisTypes}" value="id"
                                                label="name"/>
                </stripes:select>
            </div>
        </div>

        <div class="control-group">
            <stripes:label for="referenceSeqSelect" class="control-label">
                Reference sequence
            </stripes:label>
            <div class="controls">
                <stripes:select name="autoSquidDto.referenceSequence" id="referenceSeqSeelct">
                    <stripes:option label="Select a reference sequence.." value="-1" selected="selected"
                                    disabled="true"/>
                    <stripes:options-collection collection="${actionBean.workRequestOptions.referenceSequences}"
                                                value="id" label="name"/>
                </stripes:select>
            </div>
        </div>

        <div class="control-group">
            <stripes:label for="pairedSequenceSelect" class="control-label">
                Paired sequencing
            </stripes:label>
            <div class="controls">
                <stripes:select name="pairedSequence" id="pairedSequenceSelect">
                    <stripes:option value="-1" label="Select your intention for paired sequencing" disabled="true"
                                    selected="selected"/>
                    <stripes:option value="YES" label="Yes"/>
                    <stripes:option value="NO" label="No"/>
                </stripes:select>
            </div>
        </div>
    </stripes:form>
<%--</stripes:layout-definition>--%>