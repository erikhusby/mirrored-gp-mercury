<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.SquidComponentActionBean"/>
<stripes:form beanclass="${actionBean.class.name}">
    <div class="control-group">
        <stripes:label for="autoSquidDto.executionType" class="control-label">
            Project execution types
        </stripes:label>
        <div class="controls">
            <c:forEach items="${actionBean.squidProjectExecutionTypes}" var="executionType">

                <stripes:radio value="${executionType.id}" name="autoSquidDto.executionType"/>${executionType.name}
            </c:forEach>
        </div>
    </div>                                                                               s

</stripes:form>
