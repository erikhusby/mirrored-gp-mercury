<%@ page import="org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme" %>
<%--
  This page allows upload of text file containing molecular index schemes.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.reagent.MolecularIndexSchemeActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Molecular Index Schemes Upload"
                       sectionTitle="Molecular Index Schemes Upload" showCreate="true">

    <stripes:layout-component name="extraHead">
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <p>Upload a tab-delimited text file. The first line contains headers: the first header may be an optional
            NAME, to specify names for grand-fathered schemes (e.g. tagged_100) that were created before the
            auto-generated names came into effect; subsequent headers are from this list:</p>
        <c:set var="enumValues" value="<%=MolecularIndexingScheme.IndexPosition.values()%>"/>
        <c:forEach items="${enumValues}" var="enumValue">
            <c:out value="${enumValue}"/>
        </c:forEach>
        <p>Subsequent lines contain an optional name, and index sequences in the same order as the headers.</p>
        <stripes:form beanclass="${actionBean.class.name}" id="schemeForm">
            <div class="form-horizontal">
                <div class="control-group">
                    <stripes:label for="schemesTextFile" class="control-label">
                        Molecular Index Schemes Text File
                    </stripes:label>
                    <div class="controls">
                        <stripes:file name="schemesTextFile" id="schemesTextFile"/>
                    </div>
                </div>
                <div class="control-group">
                    <div class="controls">
                        <stripes:submit name="upload" value="Upload Schemes" class="btn btn-primary"/>
                    </div>
                </div>
            </div>
        </stripes:form>

        <c:forEach items="${actionBean.molecularIndexingSchemes}" var="molIndScheme">
            <c:out value="${molIndScheme.name}"/>
            <br/>
        </c:forEach>
    </stripes:layout-component>
</stripes:layout-render>
