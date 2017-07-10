<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.container.ContainerActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="List Storage Locations" sectionTitle="List Storage Locations" showCreate="true">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j("#error-dialog").hide();

                function displaySuccess(errorText) {
                    displayNotification(errorText, "alert-success");
                }

                function displayError(displayErrerrorText) {
                    displayNotification(errorText, "alert-error");
                }

                function displayNotification(errorText, clazz) {
                    $j("#error-dialog").removeClass("alert-error");
                    $j("#error-dialog").removeClass("alert-success");
                    $j("#error-dialog").addClass(clazz);
                    $j("#error-text").text(errorText);
                    $j("#error-dialog").show();
                }

                $j("#searchTermSubmit").click(function (e) {
                    e.preventDefault();
                    var searchTerm = $j("#searchTerm").val();
                    var containerBarcode = $j("#containerBarcode").val();
                    if (searchTerm) {
                        var oldPath = $j("#ajax-jstree").jstree(true).settings.core.data.url;
                        var newPath = '${ctxpath}/storage/storage.action?searchNode=&searchTerm=' + searchTerm;
                        try {
                            $j("#ajax-jstree").jstree(true).settings.core.data.url = newPath;
                            $j("#ajax-jstree").jstree(true).refresh();
                        } finally {
                            $j("#ajax-jstree").jstree(true).settings.core.data.url = oldPath;
                        }
                    }
                });

                $j('#ajax-jstree').jstree({
                    plugins: ["dnd", "types", "sort"],
                    types : {
                        FREEZER: {
                        },
                        SHELF: {
                        },
                        BOX: {
                        },
                        SECTION: {
                        },
                        GAGERACK: {
                            valid_children: ["slot"],
                            "max_depth" : 2
                        },
                        SLOT: {
                            "valid_children" : [ "lab_vessel" ],
                            "max_depth" : 1
                        }
                    },
                    'core' : {
                        "check_callback" :  function (op, node, par, pos, more) {
                            if (node.type && (node.type !== "GAGERACK" || node.type !== "BOX")) {
                                return false;
                            }
                            if((op === "move_node" || op === "copy_node") && more && more.core && !confirm('Are you sure ...')) {
                                return false;
                            }
                            return true;
                        },
                        'data' : {
                            "url" : "${ctxpath}/storage/storage.action?loadTreeAjax=",
                            "data" : function (node) {
                                console.log(node);
                                if (node.id == "#") {
                                    return {"id": node.id};
                                } else {
                                    return {"id": node.data.storageLocationId};
                                }
                            },
                            "dataType" : "json",
                            'error': function (data) {
                                console.log(data);
                                displayError("Failed to load storages.");
                            }
                        }
                    }
                }).bind("move_node.jstree", function (e, data) {
                    // Validate that the move can actually happen, if not then have to force refresh since its out of sync
                    var formData = new FormData();
                    var nodeName = data.node.data.storageLocationId;
                    var newParentName = $j('#ajax-jstree').jstree().get_node(data.parent).data.storageLocationId;
                    formData.append("nodeName", nodeName);
                    formData.append("newParentName", newParentName);
                    formData.append("moveNodeAction", "");
                    $j.ajax({
                        url: "${ctxpath}/storage/storage.action",
                        type: 'POST',
                        data: formData,
                        async: false,
                        success: function (results) {
                            console.log(results);
                            var retObj = JSON.parse(results);
                            console.log(retObj);
                            if (retObj.hasError) {
                                $j('#ajax-jstree').jstree("refresh");
                            } else {
                                displaySuccess("Successfully moved node");
                            }
                        },
                        error: function(results){
                            console.log(results);
                            displayError("A server error occured.");
                            $j('#ajax-jstree').jstree("refresh");
                        },
                        cache: false,
                        datatype: "text",
                        processData: false,
                        contentType: false
                    });
                }).bind("select_node.jstree", function (e, data) {
                    var node = data.node;
                    var typesLabVessels = ['SHELF', 'GAGERACK', 'BOX', 'SLOT'];
                    console.log(node);
                    if ($j.inArray(node.type, typesLabVessels) > -1) {
                        console.log("Acceptable type to perform ajax search on right side")
                    }
                });
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <div class="row">
            <div class="alert" id="error-dialog">
                <button type="button" class="close" data-dismiss="alert">&times;</button>
                <strong>Error!</strong><span id="error-text">defaul error message.</span>
            </div>
        </div>
        <div class="row">
            <stripes:form class="form-inline" beanclass="${actionBean.class.name}">
                <input type="text" id="searchTerm" name="searchTerm" placeholder="storage barcode"/>
                <stripes:submit id="searchTermSubmit" name="findLabVessel" value="Find" class="btn btn-primary"/>
            </stripes:form>
        </div>
        <div class="row-fluid">
            <div class="span6">
                <div class="row">
                    <div id="ajax-jstree" class="span12"></div>
                </div>
            </div>
            <div class="span6">
                <div id="replaceMeWithStorageContents"/>
            </div>
        </div>

    </stripes:layout-component>

</stripes:layout-render>