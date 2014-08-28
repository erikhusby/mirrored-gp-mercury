<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%-- This page allows the user to construct a user-defined search.  It is also used to display
 pre-constructed, read-only searches like Cancer Phenotype Search. --%>

<stripes:layout-render name="/layout.jsp" pageTitle="User-Defined Search" sectionTitle="User-Defined Search">

<stripes:layout-component name="extraHead">


    <%-- Need to stomp some global layout settings:
         label html5 tag in search terms consumes entire horizontal layout area (Chrome only?), remove padding
         select element are padded in mercury.css resulting in bad vertical layout
     --%>
    <style>
        label {display:inline; margin-left: 5px;}
        input.displayTerm, input.termoperator, input.termvalue { margin: 3px; }

    </style>
</stripes:layout-component>

<stripes:layout-component name="content">


<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.ConfigurableSearchActionBean"--%>
<stripes:form action="/search/ConfigurableSearch.action" onsubmit="return validateAndSubmit(this)"
              id="searchForm">
    <h4>Search For Entity Type: </h4>

                <stripes:select name="entityName" id="entityName">
                    <c:forEach items="${actionBean.availableEntityTypes}" var="entry">
                        <option value="${entry.entityName}">${entry.displayName}</option>
                    </c:forEach>
                </stripes:select>
    <stripes:submit name="queryPage" value="Search" class="btn btn-primary"/>
                
</stripes:form>

</stripes:layout-component>
</stripes:layout-render>