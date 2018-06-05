<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean"/>

<stripes:form beanclass="${actionBean.class.name}">

<stripes:select id="suggestedValueList" name="" multiple="multiple">
    <stripes:option value="">Select values for "${actionBean.criteriaLabel}"...</stripes:option>
    <stripes:options-collection collection="${actionBean.criteriaSelectionValues}" label="" value=""/>

</stripes:select>

</stripes:form>