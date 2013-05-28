<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.UploadQuantsActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Upload Quants" sectionTitle="Upload Quants">
    <stripes:layout-component name="extraHead">
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="uploadForm" class="form-horizontal">
            <div class="form-horizontal">
                <div class="control-group">
                    <stripes:label for="quantType" class="control-label">Quant Type</stripes:label>
                    <div class="controls">
                        <stripes:select name="quantType">
                            <stripes:options-collection collection="${actionBean.uploadEnabledMetricTypes}"
                                                        label="displayName"/>
                        </stripes:select>
                    </div>
                    <stripes:label for="quantFile" class="control-label">
                        Quant Spreadsheet
                    </stripes:label>
                    <div class="controls">
                        <stripes:file name="quantSpreadsheet" id="quantFile"/>
                    </div>
                </div>
                <div class="control-group">
                    <div class="controls">
                        <stripes:submit name="uploadQuant" value="Upload Quants" class="btn btn-primary"/>
                    </div>
                </div>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>