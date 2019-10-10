<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%--
  This page allows a user to upload plates of molecular indexes.
--%>
<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.reagent.MolecularIndexPlateActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Molecular Index Plates Upload"
                       sectionTitle="Molecular Index Plates Upload" showCreate="true">

    <stripes:layout-component name="extraHead">
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="platesForm">
            <div class="form-horizontal">
                <div class="control-group">
                    <stripes:label for="technologyAndParserType" class="control-label">Technology And Parser</stripes:label>
                    <div class="controls">
                        <stripes:select name="technologyAndParserType">
                            <stripes:options-collection collection="${actionBean.activeTechnologiesAndParsers}"
                                                        label="prettyName"/>
                        </stripes:select>
                    </div>
                    <stripes:label for="spreadsheet" class="control-label">
                        Molecular Index Plates File
                    </stripes:label>
                    <div class="controls">
                        <stripes:file name="spreadsheet" id="spreadsheet"/>
                    </div>
                </div>
                <div class="control-group">
                    <div class="controls">
                        <stripes:submit name="upload" value="Upload Plates" class="btn btn-primary"/>
                    </div>
                </div>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
