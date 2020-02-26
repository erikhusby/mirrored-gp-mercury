<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.storage.StorageAllocationActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="SRS Rack Space Finder" sectionTitle="SRS Rack Space Finder" showCreate="false">

    <stripes:layout-component name="extraHead">
        <link rel="stylesheet" href="${ctxpath}/resources/css/jquery-ui-1.9.2.custom.min.css">
        <style>
            .show-loading-icon {
                background: url("${ctxpath}/resources/scripts/jsTree/themes/default/throbber.gif") center center no-repeat;
            }
        </style>
        <script src="${ctxpath}/resources/scripts/jquery-ui-1.9.2.custom.min.js"></script>
        <script type="text/javascript">

            var evtActivateHandler = function( evt, ui ) {
                // Ignore collapse request
                if( ui.newHeader.length == 0 && ui.newPanel.length == 0 ) {
                    return true;
                }
                var storageLocationId = ui.newHeader.data("storageLocationId");
                var contentPane = $j("#freezer_" + storageLocationId );

                // Ignore refresh of content if already present
                if( contentPane.data("isLoaded") ) {
                    return true;
                }

                var requestData = [
                    {name: "evtDrillDown", value: ""},
                    {name: "<csrf:tokenname/>", value: "<csrf:tokenvalue/>"},
                    {name: "storageLocationId", value: storageLocationId}];
                contentPane.addClass("show-loading-icon");
                $j.ajax( "${ctxpath}/storage/allocation.action",
                    {
                        context: contentPane,
                        data: requestData,
                        dataType: "html",
                        method: "POST",
                        success: function (data) {
                            this.removeClass("show-loading-icon");
                            this.html(data);
                            this.data("isLoaded",true)
                        },
                        error: function (data) {
                            this.removeClass("show-loading-icon");
                            this.html(data);
                            this.data("isLoaded",false)
                        }
                    } );
                return true;
            };

            /**
             * Sets up UI
             */
            $j(document).ready(function () {
                $j( "#storageAccordion" ).accordion({
                    active:false,
                    collapsible: true,
                    header: "h5",
                    heightStyle: "content",
                    activate: evtActivateHandler
                } );
            } );
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <div class="container-fluid">
        <div class="row-fluid">
            <div class="span12">
                <div id="storageAccordion">
                    <c:forEach items="${actionBean.rootLocations}" var="freezer">
                        <h5 data-storage-location-id="${freezer.storageLocationId}">${freezer.label}</h5>
                        <div data-is-loaded="false" id="freezer_${freezer.storageLocationId}"></div>
                    </c:forEach>
                </div>
            </div><%--row-fluid--%>
        </div><%--container-fluid--%>
    </stripes:layout-component>

</stripes:layout-render>