<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>


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
            <%-- Either appends the scan results using ajax, or replaces the window contents with the results. --%>
            if (${actionBean.appendScanResults}) {
                $j.ajax({
                    url: scanUrl,
                    dataType:'html',
                    success:showResults
                });
            } else {
                document.location.href = scanUrl;
            }
        });
    }
    function showResults(data) {
        $j("#results").append(data);
    }
</script>

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
