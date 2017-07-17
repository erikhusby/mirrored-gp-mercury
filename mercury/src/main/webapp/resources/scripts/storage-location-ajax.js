$j(document).ready( function() {

    function displayNotification(errorText, clazz) {
        $j("#error-dialog").removeClass("alert-error");
        $j("#error-dialog").removeClass("alert-success");
        $j("#error-dialog").addClass(clazz);
        $j("#error-text").text(errorText);
        $j("#error-dialog").show();
    }

    function findLocationTrail(storageId) {
        $j.ajax({
            url: "/Mercury/storage/storage.action?findLocationTrail=&storageId=" + storageId,
            type: 'POST',
            async: true,
            success: function (resultObj) {
                console.log(resultObj);
                if( resultObj.hasErrors) {
                    displayNotification(resultObj.errors, "alert-error");
                } else {
                    $j("#storageName").val(resultObj.locationTrail);
                    $j("#storageId").val(storageId);
                    console.log("Set value of storage id to " + $j("#storageId").val());
                }
            },
            error: function(results){
                console.log("Error message");
                console.log(results);
            },
            cache: false,
            datatype: "application/json",
            processData: false,
            contentType: false
        });
    }

    $j("#searchTermAjaxSubmit").click(function (e) {
        e.preventDefault();
        var searchTerm = $j("#searchTermAjax").val();
        var oldPath = $j("#ajax-jstree").jstree(true).settings.core.data.url;
        var newPath = '/Mercury/storage/storage.action?searchNode=&searchTerm=' + searchTerm;
        try {
            $j("#ajax-jstree").jstree(true).settings.core.data.url = newPath;
            $j("#ajax-jstree").jstree(true).refresh();
        } finally {
            $j("#ajax-jstree").jstree(true).settings.core.data.url = oldPath;
        }
    });

    function initializeJsTreeNav() {
        $j('#ajax-jstree').jstree({
            plugins: ["types", "sort", "wholerow"],
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
                },
                SLOT: {
                }
            },
            'core' : {
                'data' : {
                    "url" : "/Mercury/storage/storage.action?loadTreeAjax=",
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
        }).bind("select_node.jstree", function (e, data) {
            var node = data.node;
            console.log(">>>>");
            console.log(node);
            console.log("<<<");
            var typesLabVessels = ['SHELF', 'GAGERACK', 'BOX', 'SLOT'];
            if ($j.inArray(node.type, typesLabVessels) > -1) {
                findLocationTrail(node.data.storageLocationId);
            } else {
                $j("#storageName").val("");
                $j("#storageId").val("");
            }
        }).bind("refresh.jstree", function (){
            $j("#ajax-jstree").jstree(true).select_node("selected_node");
        });
    }

    $j("#storage_location_overlay").dialog({
        title: "Select Storage Location",
        autoOpen: false,
        height: 600,
        width: 450,
        modal: true,
        buttons: {
            "Ok": function() {
                if ($j("#storageId").val().length > 0) {
                    $j(this).dialog("close");
                } else {
                    alert("Please select a valid location before closing.");
                }
            },
            "Cancel": function() {
                $j(this).dialog("close");
            }
        },
        open: function(){
            initializeJsTreeNav();
        }
    });

    $j("#browse").click(function(e) {
        e.preventDefault();
        $j("#storage_location_overlay").dialog("open");
    });
});