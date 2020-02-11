<%--
JSP to allow upload of vessels of reagents, page 2.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.reagent.ReagentActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Reagent Upload"
                       sectionTitle="Reagent Upload" showCreate="true">

    <stripes:layout-component name="extraHead"/>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="reagentForm2">
            <c:forEach items="${actionBean.tubesAndIndexNames}" var="pair" varStatus="item">
                <input type="hidden" name="tubesAndIndexNames[${item.index}]" value="${pair}"/>
            </c:forEach>
            <c:if test="${actionBean.hasWarnings}">
                <p>There are warnings shown above. Click Save only if the warnings can be ignored.</p>
            </c:if>
            <div class="controls">
                <stripes:submit name="save" value="Save" class="btn btn-primary"/>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
