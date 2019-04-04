$j(document).ready( function() {

    const hiddenColumns = [ 2, 7 ];

    var availablePositions = [];
    var usedPositions = [];

    $j("#error-dialog").hide();

    function displaySuccess(msg) {
        var msgs = [];
        msgs.push(msg);
        displayNotification(msgs, "alert-success");
    }

    function displayError(msg) {
        var errs = [];
        errs.push(msg);
        displayNotification(errs, "alert-error");
    }

    function displayErrors(errs) {
        displayNotification(errs, "alert-error");
    }

    function hideErrors() {
        $j("#error-dialog").hide();
    }

    function displayNotification(errMsgs, clazz) {
        $j("#error-dialog").removeClass("alert-error");
        $j("#error-dialog").removeClass("alert-success");
        $j("#error-dialog").addClass(clazz);
        $j("#error-messages").empty();
        $j.each(errMsgs, function (idx, value) {
            $j("#error-messages").append($('<li>').text(value));
        });
        $j("#error-dialog").show();
        location.href = "#error-dialog";
    }

    /**
     * Remove first element from Available position and place into 'Next Open Position' Text Field
     */
    function popNextOpenPosition() {
        if (availablePositions.length === 0) {
            $j('#nextOpenPosition').val("");
        } else {
            var nextPos = availablePositions.shift();
            $j('#nextOpenPosition').val(nextPos);
        }
    }

    function changeComboBox(elementId, jsonArr) {
        var $el = $j(elementId);
        $el.empty();
        for (var i in jsonArr) {
            var value = jsonArr[i].value;
            var label = jsonArr[i].label;
            $el.append($('<option/>', { value: value, text: label }));
        }
    }

    function changeComboBoxIdName(elementId, jsonArr) {
        console.log(jsonArr);
        var $el = $j(elementId);
        $el.empty();
        for (var i in jsonArr) {
            var label = jsonArr[i].name;
            $el.append($('<option/>', { value: label, text: label }));
        }
    }

    function validateData(barcode) {
        hideErrors();
        if (barcode.length === 0) {
            displayError("The scan failed. Did not see any value. Please re-scan");
            return false;
        }

        if ($j('#currentRack').val().trim().length === 0 ||
            ($j('#organism').val() != null && $j('#organism').val().length === 0 )||
            $j('#receptacleType').val().length === 0 ||
            $j('#originalMaterialType').text().length === 0 ||
            $j('#materialType').text().length === 0 ||
            ($j('#receptacleType').val().length !== 0 && $j('#labelFormat').val().length === 0) ||
            $j('#formatType').val().length === 0) {

            var errors = [];

            if ($j('#currentRack').val().trim().length === 0) {
                errors.push('There is no container in the container scan box.  Please scan a container.');
            }
            if ($j('#organism').val() == null || $j('#organism').val().length === 0) {
                errors.push('Please select an organism.');
            }
            if ($j('#receptacleType').val().length === 0) {
                errors.push('Please select a receptacle type.');
            }
            if ($j('#formatType').val().length === 0) {
                errors.push('Please select a format type.');
            }
            if ($j('#materialType').text().length === 0) {
                errors.push('Please select a material type.');
            }
            if ($j('#originalMaterialType').text().length === 0) {
                errors.push('Please select an original material type.');
            }
            if ($j('#receptacleType').val().length !== 0 && $j('#labelFormat').val().length === 0) {
                errors.push('Please select a label format.');
            }

            // print error messages and return false
            displayErrors(errors);
            return false;
        }

        return true;
    }

    function addRowToDataTable() {
        var tableLength = table.fnGetData().length;
        const volTag = "<input type='text' name='volume[" + tableLength + "]' class='input-small'>";

        table.fnAddData([
            $j('#idText').val().trim(),
            $j('#barcodeType').val().trim(),
            volTag,
            $j('#currentRack').val().trim(),
            $j('#nextOpenPosition').val().trim(),
            $j('#organism option:selected').text().trim(),
            $j('#organism').val(), // Store Organism Id as well, but hidden
            $j('#receptacleType').val(),
            $j('#labelFormat option:selected').text().trim(),
            $j('#materialType option:selected').text(),
            $j('#originalMaterialType option:selected').text(),
            $j('#formatType').val()
        ]);

        var currPos = availablePositions[0];
        usedPositions.push(currPos);
        console.log(usedPositions);
        popNextOpenPosition();
    }

    function toggleColumnVisibility(bShowHide) {
        $j.each(hiddenColumns, function (i, val) {
            console.log("Toggling col " + (val - 1) + " to " + bShowHide);
            table.fnSetColumnVis(val - 1, bShowHide);
        });
    }

    $j('#saveKit').click(function (e) {
        try {
            e.preventDefault();
            toggleColumnVisibility(true);
            var data = table.fnGetData();

            var kitInfoFormArray = $j('#kitInfoForm').serializeArray();
            var kitInfoObj = objectifyForm(kitInfoFormArray);

            // Replace volume tags in data table with values - convert row
            const volIndx = 1;
            for (var idx = 0; idx < data.length; idx++) {
                var volumeTag = $j("[name='volume[" + idx + "]']");
                data[idx][volIndx] = volumeTag.val();
            }

            // Convert Data Table To objects based on header names
            var heads = ["collaboratorId", "scanType", "volume", "container", "position", "organism", "organismId",
                "receptacleType", "labelFormat", "materialType", "originalMaterialType", "formatType"];
            var rows = [];
            $("tbody tr").each(function () {
                cur = {};
                $(this).find("td").each(function (i, v) {
                    cur[heads[i]] = $(this).text().trim();
                });
                rows.push(cur);
                cur = {};
            });

            console.log(rows);

            kitInfoObj['externalSampleContents'] = rows;

            const tableData = JSON.stringify(kitInfoObj).escapeJson();
            console.log(tableData);
            $j.ajax({
                url: "/Mercury/receiving/receiveByExternal.action?<%= ReceiveExternalActionBean.UPLOAD_KIT %>",
                data: {"tubeData": tableData},
                type: 'POST',
                async: true,
                dataType: 'json',
                success: function (json) {
                    console.log(json);
                    if(json.hasOwnProperty('hasErrors') && json.hasErrors){
                        displayError(json.msg);
                    } else {
                        displaySuccess(json.msg);
                        table.fnClearTable();
                    }
                },
                error: function (results) {
                    displayError(results.responseText);
                    console.log(results);
                    return false;
                }
            });
        } catch (e) {
            console.log(e.Message);
        } finally {
            toggleColumnVisibility(false);
        }
    });

    function objectifyForm(formArray) {//serialize data function

        var returnArray = {};
        for (var i = 0; i < formArray.length; i++){
            returnArray[formArray[i]['name']] = formArray[i]['value'];
        }
        return returnArray;
    }

    function addTube() {
        $j.ajax({
            url: "/Mercury/receiving/receiveByExternal.action?",
            data: "validatePlateTube=&receptacleType=" + $j('#receptacleType').val() +
            "&containerBarcode=" + $j('#currentRack').val().trim(),
            type: 'GET',
            async: true,
            success: function (json) {
                if (json.hasErrors) {
                    displayError(json.msg);
                    return false;
                }
                addRowToDataTable();
                return true;
            },
            error: function(results){
                displayError(results.responseText);
                console.log(results);
                return false;
            }
        });
    }

    $j('#receptacleType').change(function (){
        $j.ajax({
            url: "/Mercury/receiving/receiveByExternal.action?",
            data: "loadLabels=&receptacleType=" + $j('#receptacleType').val(),
            type: 'GET',
            async: true,
            success: function (json) {
                changeComboBoxIdName("#labelFormat", json.idNames)
            },
            error: function(results){
                console.log("Error message");
                console.log(results);
                return false;
            }
        });
    });

    $j('#group').change(function() {
        $j.ajax({
            url: "/Mercury/receiving/receiveByExternal.action?",
            data: "loadCollectionSelect=&groupName=" + $j("#group option:selected").val(),
            type: 'GET',
            async: true,
            success: function (json) {
                changeComboBox('#collection', json);
                $j('#collection').trigger('change');
            },
            error: function(results){
                console.log("Error message");
                console.log(results);
            }
        });
    });

    $j('#collection').change(function() {
        $j.ajax({
            url: "/Mercury/receiving/receiveByExternal.action?",
            data: "loadSiteSelect=&collectionId=" + $j("#collection option:selected").val(),
            type: 'GET',
            async: true,
            success: function (json) {
                changeComboBox('#site', json.sites);
                changeComboBox('#organism', json.organisms);
            },
            error: function (results) {
                console.log("Error message");
                console.log(results);
            }
        });
    });

    $j('#currentRack').change(function() {
        var containerBarcode = $j(this).val();
        if (containerBarcode === "") {
            return;
        }
        console.log("Current Rack Changed: Grab Available Positions " + containerBarcode);
        $j.ajax({
            url: "/Mercury/receiving/receiveByExternal.action?",
            data: "getNextOpenPosition=&containerBarcode=" + containerBarcode,
            type: 'GET',
            async: true,
            success: function (json) {
                console.log(json);
                availablePositions = compareAvailableWithTable(containerBarcode, json.availableWells);
                usedPositions = [];
                popNextOpenPosition();
            },
            error: function (err) {
                displayError("Failed to load plate information for " + containerBarcode);
            }
        });
    });

    $j('#currentRack').keyup(function() {
        if (event.which !== 13 || event.which !== 9) {
            return;
        }
        var containerBarcode = $j(this).val();
        if (containerBarcode === "") {
            return;
        }
        console.log("Current Rack Changed: Grab Available Positions " + containerBarcode);
        $j.ajax({
            url: "/Mercury/receiving/receiveByExternal.action?",
            data: "nextOpenPosition=&containerBarcode=" + containerBarcode,
            type: 'GET',
            async: true,
            success: function (json) {
                console.log(json);
                availablePositions = json.availableWells;
                popNextOpenPosition();
            },
            error: function (results) {
                console.log("Error message");
                console.log(results);
            }
        });
    });

    $j('#idText').keyup(function (event) {
        if (event.which !== 13) {
            return;
        }
        var barcode = $(this).val().trim();
        console.log("Validating; " + barcode);
        if (validateData(barcode)) {
            if (addTube()) {

            }
        }
    });

    $j("#create_container_overlay").dialog({
        title: "Create Container",
        autoOpen: false,
        height: 150,
        width: 300,
        modal: true,
        buttons: {
            "OK": {
                text: "OK",
                id: "okbtnid",
                click: function () {
                    var selectedValue = $j("#rackType").val();
                    var receptacleType = $j("#receptacleType").val();
                    console.log("Attempt to create container Rack: " + selectedValue + " receptacle:" + receptacleType);
                    if (selectedValue.length > 0 && receptacleType.length > 0) {
                        $j.ajax({
                            url: "/Mercury/container/container.action?",
                            data: "createContainer=&inBsp=true&rackType=" + selectedValue + "&receptacleType=" + receptacleType,
                            type: 'GET',
                            async: true,
                            success: function (json) {
                                console.log(json.containerBarcode);
                                $j("#currentRack").val(json.containerBarcode);
                                $j("#create_container_overlay").dialog("close");
                                $j("#currentRack").trigger( "change" );
                            },
                            error: function (results) {
                                console.log("Error message");
                                console.log(results);
                            }
                        });
                    }
                }
            },
            "Cancel": function() {
                $j(this).dialog("close");
            }
        },
        open: function(){

        }
    });

    $j("#createContainer").click(function(e) {
        e.preventDefault();
        $j("#create_container_overlay").dialog("open");
    });

    const table = $j('#samplesTable').DataTable({
        'oTableTools': {
            'aButtons': ['copy', 'csv']
        },
        "aoColumns": [
            {"bSortable": false},
            {"bVisible": false},
            {"bSortable": false},
            {"bSortable": true} ,
            {"bSortable": true} ,
            {"bSortable": true} ,
            {"bVisible": false} ,
            {"bSortable": true},
            {"bSortable": true},
            {"bSortable": true},
            {"bSortable": true},
            {"bSortable": true}
        ]
    });

    function compareAvailableWithTable(currContainer, oAvailableWellsLocal) {
        var data = table.fnGetData();
        for (var i = 0; i < data.length; i++) {
            var row = data[i];
            var rowContainer = row[3];
            var rowPos = row[4];
            if (currContainer === rowContainer) {
                var index = oAvailableWellsLocal.indexOf(rowPos);
                if (index !== -1) {
                    oAvailableWellsLocal.splice(index, 1);
                }
            }
        }
        return oAvailableWellsLocal;
    }

    $('#delete').click( function() {
        // Grab current data in container and next open position
        var data = table.fnGetData();
        var lastRow = data[data.length - 1];
        var pos = lastRow[4];
        var container = lastRow[3];
        console.log("Deleting: " + container + " " + pos);
        var currentRack = $j('#currentRack').val();
        if (currentRack === container) {
            availablePositions.unshift(pos);
            popNextOpenPosition();
        }

        var aTrs = table.fnGetNodes();
        table.fnDeleteRow( aTrs[aTrs.length - 1] );
    } );

    // Load initial labels for default barcode type
    $j('#receptacleType').trigger('change');

    toggleColumnVisibility(false);
});