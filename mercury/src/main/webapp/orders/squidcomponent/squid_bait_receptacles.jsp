<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.SquidComponentActionBean"/>

<stripes:form beanclass="${actionBean.class.name}" partial="true">
    <div class="control-group">
        <stripes:label for="baitReceptacles" class="control-label">
            Sample receptacles *
        </stripes:label>
        <div class="controls">
            <stripes:select name="selectedBaitReceptacles" id="baitReceptacles" multiple="true">
                <stripes:option label="Select an oligio group.." value="-1" disabled="true" selected="selected"/>
                <c:forEach items="${actionBean.selectedBaits.groupReceptacles}" var="sampleReceptacle">
                    <stripes:option label="${sampleReceptacle.barcode} -- ${sampleReceptacle.externalId} -- ${sampleReceptacle.referenceSequence}"
                                    value="${sampleReceptacle.barcode}" />
                </c:forEach>
            </stripes:select>
        </div>
    </div>
</stripes:form>