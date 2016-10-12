<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
        beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.SampleSheetActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Sample Sheet" sectionTitle="Sample Sheet">
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="uploadForm" class="form-horizontal">
            <div class="form-horizontal">
                <div class="control-group">
                    <stripes:label for="pdoBusinessKeys" class="control-label">Product Orders (from same Research Project)</stripes:label>
                    <div class="controls">
                        <stripes:text name="pdoBusinessKeys"/>
                    </div>
                    <stripes:label for="chipWellBarcodes" class="control-label">Chip Well Barcodes</stripes:label>
                    <div class="controls">
                        <stripes:textarea rows="20" name="chipWellBarcodes"/>
                    </div>
                    <div class="controls">
                        <stripes:submit name="download" value="Download" class="btn btn-primary"/>
                    </div>
                </div>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>