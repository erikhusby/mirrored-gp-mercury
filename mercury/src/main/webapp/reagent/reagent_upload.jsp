<%--
JSP to allow upload of vessels of reagents.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.reagent.ReagentActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Reagent Upload"
                       sectionTitle="Reagent Upload" showCreate="true">

    <stripes:layout-component name="extraHead">
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="reagentForm">
            <div class="form-horizontal">
                <div class="control-group">
                    <stripes:label for="reagentFormat" class="control-label">File Format</stripes:label>
                    <div class="controls">
                        <stripes:select name="reagentFormat">
                            <stripes:options-enumeration
                                    enum="org.broadinstitute.gpinformatics.mercury.presentation.reagent.ReagentActionBean.ReagentFormat"
                                    label="displayName"/>
                        </stripes:select>
                    </div>
                    <stripes:label for="reagentsFile" class="control-label">
                        Reagents File
                    </stripes:label>
                    <div class="controls">
                        <stripes:file name="reagentsFile" id="reagentsFile"/>
                    </div>
                </div>
                <div class="control-group">
                    <div class="controls">
                        <stripes:submit name="upload" value="Upload Reagents" class="btn btn-primary"/>
                    </div>
                </div>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
