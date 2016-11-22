<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean" beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.PooledTubeUploadActionBean"/>

<script type="text/javascript">
    $j(document).ready(function () {
        $(".control-group").removeClass("control-group");
        $(".control-label").removeClass("control-label");
        $(".controls").removeClass("controls");
        $j("#vesselBarcode").attr("value", $("#vesselLabel").val());
        $j("#accordion").accordion({ collapsible:true, active:false, heightStyle:"content", autoHeight:false});
    });
</script>

<stripes:layout-render name="/layout.jsp" pageTitle="Upload Pooled Tubes" sectionTitle="Upload Pooled Tubes">
    <stripes:layout-component name="extraHead">
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="uploadForm">
                <div></br>
                </div>
                <div>
                <stripes:file name="pooledTubesSpreadsheet" id="pooledTubesSpreadsheet"/>
                </div>
                <div>
                    <div class="controls">
                        <stripes:submit name="uploadpooledTubes" value="Upload Pooled Tubes" class="btn btn-primary"/>
                    </div>
                </div>
            <div style="float: left; width: 50%;">
                <br/>
                <stripes:label for="overWriteFlag">Overwrite previous upload</stripes:label>
                <stripes:checkbox id="overWriteFlag" name="overWriteFlag"/>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>