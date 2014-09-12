<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.PicoDispositionActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.pageTitle}"
                       sectionTitle="${actionBean.pageTitle} from Rack Scan" showCreate="false">

    <stripes:layout-component name="extraHead">

        <script type="text/javascript">

            $j(document).ready(function () {
                $j( "#labToFilterBy" ).change(function() {
                    var scanUrl = "${ctxpath}${actionBean.rackScanPageUrl}?${actionBean.showScanSelectionEvent}="
                            +"&labToFilterBy=" + $j("select#labToFilterBy option:selected").val();
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
                    var scanUrl = "${ctxpath}${actionBean.rackScanPageUrl}?${actionBean.scanEvent}="
                            + "&labToFilterBy=" + $j("select#labToFilterBy option:selected").val()
                            + "&rackScanner=" +$j("select#rackScanner option:selected").val();
                    document.location.href = scanUrl;
                });
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

        <%-- rackScanForm content is replaced with dropdown select by ajax. --%>
        <div id="rackScanForm"></div>

    </stripes:layout-component>

</stripes:layout-render>
