<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.LinkDenatureTubeToReagentBlockActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Link Denature Tube to Reagent Block" sectionTitle="Scan Barcodes">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            function showDenatureTubeInfo() {
                $j('#denatureTubeInfo').html("<img src=\"${ctxpath}/images/spinner.gif\"/>");
                var barcode = $j("#dTubeText").val();
                $j.ajax({
                    url:"${ctxpath}/workflow/LinkDenatureTubeToReagentBlock.action?denatureInfo=&denatureTubeBarcode=" + barcode,
                    dataType:'html',
                    success:updateDetails
                });
            }
            function updateDetails(data) {
                $j("#denatureTubeInfo").html(data);
            }
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="scanForm" class="form-horizontal">

            <div class="control-group">
                <stripes:label for="dTubeText" name="Denature Tube Barcode" class="control-label"/>
                <div class="controls">
                    <stripes:text id="dTubeText" name="denatureTubeBarcode" onchange="showDenatureTubeInfo()"/>
                </div>
            </div>
            <div class="control-group">
                <stripes:label for="reagentBlockText" name="Reagent Block Barcode" class="control-label"/>
                <div class="controls">
                    <stripes:text id="reagentBlockText" name="reagentBlockBarcode"/>
                </div>
            </div>

            <div id="denatureTubeInfo"></div>

            <div class="control-group">
                <div class="controls actionButtons">
                    <stripes:submit name="save" value="Submit" class="btn btn-primary"/>
                </div>
            </div>

        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
