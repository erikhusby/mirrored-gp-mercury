<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.container.ContainerActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="List Storage Locations" sectionTitle="List Storage Locations" showCreate="true">

    <stripes:layout-component name="extraHead">
        <link rel="stylesheet"
              href="${ctxpath}/resources/scripts/jsTree/themes/default/style.min.css"/>
        <script src="${ctxpath}/resources/scripts/jsTree/jstree.min.js"></script>
        <style>
            .jstree-anchor {
                /*enable wrapping*/
                white-space : normal !important;
                /*ensure lower nodes move down*/
                height : auto !important;
                /*offset icon width*/
                padding-right : 24px;
            }
            .show-loading-icon {
                background: url("${ctxpath}/resources/scripts/jsTree/themes/default/throbber.gif") center center no-repeat;
            }
        </style>
        <script type="text/javascript">
            $j(document).ready(function () {
                $j("#error-dialog").hide();
                var canCreate = ${actionBean.moveAllowed};

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

                function findContainer(containerBarcode, node) {
                    var formData = new FormData();
                    formData.append("viewContainerAjax", "");
                    formData.append("containerBarcode", containerBarcode);
                    var nodeDom;
                    if (node) {
                        $j("#replaceMeWithStorageContents").addClass("show-loading-icon");
                    }
                    $j.ajax({
                        url: "${ctxpath}/container/container.action",
                        type: 'POST',
                        data: formData,
                        async: false,
                        success: function (results) {
                            if (node) {
                            }
                            $j("#replaceMeWithStorageContents").html(results);
                        },
                        error: function(results){
                            console.log(results);
                            displayError("A server error occured.");
                            $j('#jstree').jstree("refresh");
                            if (node) {
                            }
                        },
                        cache: false,
                        datatype: "text",
                        processData: false,
                        contentType: false
                    });
                }

                $j("#searchTermSubmit").click(function (e) {
                    e.preventDefault();
                    var searchTerm = $j("#searchTerm").val();
                    var containerBarcode = $j("#containerBarcode").val();
                    console.log("SearchTerm: " + searchTerm);
                    console.log("container Barcode: " + containerBarcode);
                    if (containerBarcode) {
                        console.log("CO call");
                        findContainer(containerBarcode);
                    }
                    else if (searchTerm) {
                        console.log("Search called");
                        var oldPath = $j("#jstree").jstree(true).settings.core.data.url;
                        var newPath = '${ctxpath}/storage/storage.action?searchNode=&searchTerm=' + searchTerm;
                        try {
                            $j("#jstree").jstree(true).settings.core.data.url = newPath;
                            $j("#jstree").jstree(true).refresh();
                        } finally {
                            $j("#jstree").jstree(true).settings.core.data.url = oldPath;
                        }
                    }
                });

                function customMenu(node) {
                    var items = {};
                    var nodeData = $j(node)[0];
                    var type = nodeData.type;
                    if (type === "StaticPlate" || type === "RackOfTubes") {
                        items.removeFromItem = {
                            label: "Remove",
                            action: function() {
                                removeFromStorage(nodeData);
                            }
                        };
                    } else if (type === "GAUGERACK" && canCreate) {
                        items.renameItem = {
                            label: "Rename",
                            action: function () {
                                var tree = $j("#jstree").jstree(true);
                                tree.edit(node);
                            }
                        }
                    }

                    return items;
                }

                $j('#jstree').jstree({
                    plugins: ["dnd", "types", "sort", "wholerow", "contextmenu"],
                    contextmenu: {items: customMenu},
                    types : {
                        '#': {
                            valid_children: ["SECTION", "SHELF"]
                        },
                        FREEZER: {
                            valid_children: ["SECTION", "SHELF"]
                        },
                        REFRIGERATOR: {
                            valid_children: ["SECTION", "SHELF"]
                        },
                        SHELF: {
                            valid_children: ["GAUGERACK", "BOX"]
                        },
                        BOX: {
                            valid_children: ["GAUGERACK", "BOX"]
                        },
                        SECTION: {
                            valid_children: ["SHELF"]
                        },
                        GAUGERACK: {
                            valid_children: ["slot"],
                            "max_depth" : 2
                        },
                        SLOT: {
                            "valid_children" : [ "lab_vessel" ],
                            "max_depth" : 1
                        },
                        StaticPlate: {

                        },
                        RackOfTubes: {

                        }
                    },
                    'core' : {
                        "check_callback" :  function (op, node, par, pos, more) {
                            //Only allow LabManagers
                            if (!canCreate)
                                    return false;
                            // Only Rename/Move Boxes and only move to shelves
                            var acceptableTypes = ['GAUGERACK','BOX'];
                            if (acceptableTypes.indexOf(node.type) == -1) {
                                return false;
                            }
                            if((op === "move_node") && more && more.core && !confirm("Are you sure...?")) {
                                return false;
                            }
                            if((op === "rename_node" && !confirm("Are you sure...?"))) {
                                return false;
                            }
                            return true;
                        },
                        'data' : {
                            "url" : "${ctxpath}/storage/storage.action?loadTreeAjax=",
                            "data" : function (node) {
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
                    var newParentName = $j('#jstree').jstree().get_node(data.parent).data.storageLocationId;
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
                                $j('#jstree').jstree("refresh");
                            } else {
                                displaySuccess("Successfully moved node");
                            }
                        },
                        error: function(results){
                            console.log(results);
                            displayError("A server error occured.");
                            $j('#jstree').jstree("refresh");
                        },
                        cache: false,
                        datatype: "text",
                        processData: false,
                        contentType: false
                    });
                }).bind("select_node.jstree", function (e, data) {
                    var node = data.node;
                    console.log(node);
                    if (node.type === "StaticPlate" || node.type === "RackOfTubes") {
                        findContainer(node.text, node);
                    }
                }).bind("rename_node.jstree", function (e, data) {
                    var formData = new FormData();
                    formData.append("nodeName", data.node.data.storageLocationId);
                    formData.append("storageName", data.node.text);
                    formData.append("oldName", data.old);
                    formData.append("renameNodeAction","");
                    $j.ajax({
                        url: "${ctxpath}/storage/storage.action",
                        type: 'POST',
                        data: formData,
                        async: false,
                        success: function (results) {
                            console.log(results);
                            if (results.hasError) {
                                $j('#jstree').jstree("refresh");
                            } else {
                                displaySuccess("Successfully renamed node.");
                            }
                        },
                        error: function(results){
                            console.log(results);
                            displayError("A server error occured.");
                            $j('#jstree').jstree("refresh");
                        },
                        cache: false,
                        datatype: "json",
                        processData: false,
                        contentType: false
                    });
                });
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <div class="container-fluid">
            <div class="row">
                <div class="alert" id="error-dialog">
                    <button type="button" class="close" data-dismiss="alert">&times;</button>
                    <span id="error-text">defaul error message.</span>
                </div>
            </div>
            <div class="row">
                <stripes:form class="form-inline" beanclass="${actionBean.class.name}">
                    <input type="text" id="containerBarcode" name="containerBarcode" placeholder="container barcode"/>
                    <input type="text" id="searchTerm" name="searchTerm" placeholder="storage barcode"/>
                    <stripes:submit id="searchTermSubmit" name="findLabVessel" value="Find" class="btn btn-primary"/>
                </stripes:form>
            </div>
            <div class="row-fluid">
                <div id="jstree" class="span3"></div>
                <div id="replaceMeWithStorageContents" class="span9">

                </div>
            </div>
        </div>

    </stripes:layout-component>

</stripes:layout-render>