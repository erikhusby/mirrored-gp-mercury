<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean"/>

<stripes:form beanclass="${actionBean.class.name}">

    <stripes:select id="suggestedValueList" name="suggestedValueSelections" multiple="multiple">
        <stripes:option value="">Select value(s) for "${actionBean.criteriaLabel}"...</stripes:option>
        <stripes:options-collection collection="${actionBean.criteriaSelectionValues}" label="" value=""/>

    </stripes:select>

    <script>
        var criteriaOperatorValue = "${actionBean.criteriaOp}";
        console.log("Operator is " + criteriaOperatorValue);
        if(criteriaOperatorValue === "equals") {
            console.log("Found the operator to be is in");
            $j("#suggestedValueList").removeAttr("multiple");
        }

    </script>

</stripes:form>