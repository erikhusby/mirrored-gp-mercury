<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<%-- Specifically not using use action bean, as the class named is abstract and this JSP is meant to be used by multiple
     action beans. --%>
<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean"--%>
<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.pageTitle}" sectionTitle="${actionBean.pageTitle}">
    <stripes:layout-component name="extraHead">

        <style type="text/css">
            .rackScanResults {
                width: 300px;
                height: 150px;
                text-overflow: ellipsis;
                white-space: nowrap;
                overflow-x: auto;
                overflow-y: auto;
            }
        </style>

        <script type="text/javascript">

            $j(document).ready(function () {
                $j( "#labToFilterBy" ).change(function() {

                    var scanUrl = "${ctxpath}${actionBean.rackScanPageUrl}?${actionBean.showScanSelectionEvent}=&labToFilterBy="
                            + $j("select#labToFilterBy option:selected").val();
                    $j.ajax({
                        url: scanUrl,
                        dataType:'html',
                        success:showRackScanForm
                    });
                });
            });
            function showRackScanForm(data) {
                $j("#rackScanForm").html(data);
                $j("#rackScanForm input[type='button']").click(function() {
                    var scanUrl = "${ctxpath}${actionBean.rackScanPageUrl}?${actionBean.scanEvent}=&labToFilterBy="
                            + $j("select#labToFilterBy option:selected").val()
                            + "&rackScanner=" +$j("select#rackScanner option:selected").val();
                    $j.ajax({
                        url: scanUrl,
                        dataType:'html',
                        success:showResults
                    });
                });
            }
            function showResults(data) {
                $j("#results").append(data);
            }
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="labForm" class="form-horizontal" onsubmit="return false;">
            <div class="form-horizontal">
                <div class="control-group">
                    <stripes:label for="labToFilterBy" class="control-label">Lab</stripes:label>
                    <div class="controls">
                        <stripes:select name="labToFilterBy" id="labToFilterBy">
                            <stripes:option value="" label="Select One" />
                            <stripes:options-collection collection="${actionBean.allLabs}" label="labName" value="name"/>
                        </stripes:select>
                    </div>
                </div>
            </div>
        </stripes:form>
        <div id="rackScanForm"></div>
        <div id="results"></div>
    </stripes:layout-component>
</stripes:layout-render>
