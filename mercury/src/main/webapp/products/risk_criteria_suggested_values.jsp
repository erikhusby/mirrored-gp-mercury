<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean"/>

<stripes:form beanclass="${actionBean.class.name}">

    <div id="suggestionList" ></div>

    <script>
        var criteriaOperatorValue = "${actionBean.criteriaOp}";
        var suggestionListContent = '<select id="suggestedValueList" name="" ';
        console.log("Operator is " + criteriaOperatorValue);
        if(criteriaOperatorValue === "is in") {
            suggestionListContent += 'multiple=""';
            console.log("Found the operator to be is in");
        }
        suggestionListContent += '>\n';
        suggestionListContent += '<option value="">Select value(s) for  "${actionBean.criteriaLabel}"...</option>';
        <c:forEach items="${actionBean.criteriaSelectionValues}" var="selection">
            suggestionListContent += '<option value="${selection}">${selection}</option>';
        </c:forEach>
        suggestionListContent += '</select>';

        $j("#suggestionList").append(suggestionListContent);
    </script>

</stripes:form>