$(document).ready(function () {

   /****************************************************
    * This script is used for the manual pooling tranfer.
    * It must be included in the manual_transfer.jsp page.
    ****************************************************/

    var workQueue = [];
    var sourceIDs = [];
    var targetIDs = [];
    var direction="";
    var directionArr=[];
    var maxRackSize = $( "[id^=src_G] td").length;
    StoreSessions();

    jsPlumb.ready(function () {

        //Initial jsPlumb setup of line types and endpoints.
        var instance = jsPlumb.getInstance({
            Connector: "StateMachine",
            PaintStyle: {strokeStyle: "red", lineWidth: 3},
            Endpoint: ["Dot", {radius: 5}],
            EndpointStyle: {fillStyle: "blue"},
            Container: "container0"
        });

        $(document).ready(function () {
            $(window).scroll(function () {
                jsPlumb.repaintEverything();
            });
        });

        var endpointOptions = {isSource: true};
        var colorSpace;

        $('#PreviewButton').click(function ()
        {preview(false)});

        //Main handler for the preview feature.
        function preview() {

            instance.deleteEveryEndpoint();
            instance.detachEveryConnection();
            var newQueueObject = {
                sourceIDs: [],
                targetIDs: []
            };

            newQueueObject.targetIDs = targetIDs.slice();
            newQueueObject.sourceIDs = sourceIDs.slice();

            //Delete any missing keys from the queue before it is sent to the server.
            for(key in workQueue) {
                if(workQueue[key].sourceIDs.length == 0 || workQueue[key].targetIDsength == 0) {
                    workQueue.splice(key,1);
                    directionArr.splice(key,1);
                }
                else {
                }
            }
            try {
                if(newQueueObject.sourceIDs[0].length > 0 || newQueueObject.targetIDs[0].length)
                    workQueue.push(newQueueObject);
            }
            catch(err){}

            var edl = document.getElementById("dataDestList");
            edl.value = JSON.stringify(workQueue);
            var index = 0;

            workQueue.forEach(function (queueItem) {
                var itemCount = 0;
                targetIDs = [];
                sourceIDs = [];
                targetIDs = queueItem.targetIDs.slice();
                sourceIDs = queueItem.sourceIDs.slice();
                var es = document.getElementById("dataSrc");
                es.value = sourceIDs.toString();
                var ed = document.getElementById("dataDest");
                ed.value = targetIDs.toString();
                var arrowCommon = {foldback: 0.7, fillStyle: "blue", width: 14},
                    overlays = [
                        ["Arrow", {location: 0.8}, arrowCommon],
                        ["Arrow", {location: 0.3}, arrowCommon]
                    ];

                //One to one mappings
                if (targetIDs.length == sourceIDs.length) {
                    var col = 0;

                    sourceIDs.forEach(function (item) {
                        colorSpace = getRandomColor();
                            var sourcePos = instance.addEndpoint(item,endpointOptions);
                            instance.connect({ source: sourcePos,
                                target: targetIDs[itemCount],
                                overlays: overlays,
                                paintStyle:{ strokeStyle:colorSpace, lineWidth:3 },
                                connector: ["StateMachine", { proximityLimit: -5, curviness: getRandomCurve() }]
                            });
                        itemCount++;
                    });
                }
                //Multiple sources to single destination
                if (sourceIDs.length > targetIDs.length && targetIDs.length == 1) {

                    sourceIDs.forEach(function (item) {
                        colorSpace = getRandomColor();
                            var sourcePos = instance.addEndpoint(item, endpointOptions);
                            instance.connect({
                                source: sourcePos,
                                target: targetIDs,
                                overlays: overlays,
                                paintStyle: {strokeStyle: colorSpace, lineWidth: 3},
                                connector: ["StateMachine", {proximityLimit: -5, curviness: getRandomCurve()}]
                            });
                        col++;
                    });
                }

                //Single source to multiple destinations..
                if (targetIDs.length > sourceIDs.length && sourceIDs.length == 1) {

                    targetIDs.forEach(function (item) {
                        colorSpace = getRandomColor();
                            var sourcePos = instance.addEndpoint(sourceIDs[0], endpointOptions);
                            instance.connect({
                                source: sourcePos,
                                target: item,
                                overlays: overlays,
                                paintStyle: {strokeStyle: colorSpace, lineWidth: 3},
                                connector: ["StateMachine", {proximityLimit: -5, curviness: getRandomCurve()}]
                            });
                        itemCount++;
                    });
                }
                ++index;
            });
            targetIDs = [];
            sourceIDs = [];
        }
    });

    //Create random colos for each line.
    function getRandomColor() {
        var letters = '0123456789ABCDEF'.split('');
        var color = '#';
        for (var i = 0; i < 6; i++) {
            color += letters[Math.floor(Math.random() * 16)];
        }
        return color;
    }

    //Generate random curves for lines.
    function getRandomCurve() {
        curve = Math.floor(Math.random() * 150) + -150;
        return 100; //Hard-Wired to 100 since this seems to work best.
    }

    //Clear connections if user removes an item.
    function removeConnections(cell) {
        var itemCount = 0;
        var queueCount = 0;

        workQueue.forEach(function (queueItem) {

            targetIDs = [];
            sourceIDs = [];
            targetIDs = queueItem.targetIDs.slice();
            sourceIDs = queueItem.sourceIDs.slice();
            sourceIDs.forEach(function () {

                var sourceIndex = workQueue[queueCount].sourceIDs.indexOf(cell);
                var targetIndex = workQueue[queueCount].targetIDs.indexOf(cell);

                if (sourceIndex > -1) {
                    workQueue[queueCount].sourceIDs.splice(sourceIndex, 1);
                }
                if (targetIndex > -1) {
                    workQueue[queueCount].targetIDs.splice(targetIndex, 1);
                    directionArr.splice(sourceIndex,1);
                }
                itemCount++;
            });
            queueCount++;
        });
    }

    //Select rows and cols
    function selectRow(id) {

        id = id.replace("btn_",".");
        var termTableInputIDs = [];
        var count = 0;
        $(id).each(function () {
            var $item_text = $(this).closest("td").find(":input[type='text']").attr('id');
            if( $("#"+$item_text).val().length === 0 ) {
                count++;
            }
        });

        if(count > 0) {
            alert('Barcodes must be present in every field');
            return;
        }

        $(id).each(function () {
            id=$(this).attr('id');
            termTableInputIDs.push(id);
            $("#"+id).trigger('click');
        });
    }

    //Main event handler to process all button clicks
    $('.btn-xs').click(function () {
        var $table = $(this).closest('table').attr('id');

        if ($table.indexOf("src_G") >= 0) {
            direction = "dest";
            sourceCell = $(this).attr("id").replace("btn_", "");
            if (!sourceCell.includes('col') && !sourceCell.includes('row')) {
                sourceIDs.push(sourceCell);
                if ($(this).text() == 'Selected') {
                    removeConnections(sourceCell);
                    sourceIDs.splice($.inArray(sourceCell, sourceIDs));
                }
            }
        }
        if ($table.indexOf("dest_G") >= 0) {
            destCell = $(this).attr("id").replace("btn_", "");
            direction = "src";
            if (!destCell.includes('col') && !destCell.includes('row')) {
                targetIDs.push(destCell);
                if ($(this).text() == 'Selected') {
                    removeConnections(destCell);
                    targetIDs.splice($.inArray(destCell, targetIDs));
                }
            }
        }

        if ($(this).attr("id") == 'btnRow1') {
            $('table tbody tr ').click(function () {
            });
        }

        if ($(this).attr("id") == 'selectAllsrc') {
            if ($(this).text() == 'Selected') {
                $('[id^=src_G] .btn-xs').text("Select");
                $('[id^=src_G] .btn-xs').css('background-color', '#5a86de');
                $('[id^=src_G] .xs-all').css('background-color', '#c266ff');
                $('[id^=src_G] .xs-col').css('background-color', '#33cc00');
                $('[id^=src_G] .xs-row').css('background-color', '#ff8c1a');
            }
            else {
                $('[id^=src_G] .btn-xs').text("Selected");
                $('[id^=src_G] .btn-xs').css('background-color', 'red');
            }
        }
        else {
            $(this).html($(this).text() == 'Select' ? 'Selected' : 'Select');

            if ($(this).text() == 'Selected') {
                $(this).css('background-color', 'red');
            }
            else {
                $(this).css('background-color', '#5a86de');
            }
            if ($("[id^=btn_src_col]")) {
                $("[id^=btn_src_col]").css('background-color', '#33cc00');
            }
            if ($("[id^=btn_src_row_]")) {
                $("[id^=btn_src_row_]").css('background-color', '#ff8c1a');
            }
            if ($("[id^=btn_dest_col]")) {
                $("[id^=btn_dest_col]").css('background-color', '#33cc00');
            }
            if ($("[id^=btn_dest_row_]")) {
                $("[id^=btn_dest_row_]").css('background-color', '#ff8c1a');
            }
        }
    });

    //Handle select all from source.
    $('#selectAllsrc').click(function () {
        for (i = 0; i < maxRackSize; i++) {
            text = "srcRcpBcd0_" + i.toString();
            sourceIDs.push(text);
        }
        sourceIDs.splice($.inArray('selectAllsrc', sourceIDs), 1);
    });

    //Handle select all from destination.
    $('#selectAlldest').click(function () {
        for (i = 0; i < maxRackSize; i++) {
            text = "destRcpBcd0_" + i.toString();
            targetIDs.push(text);
        }
        targetIDs.splice($.inArray('selectAlldest', targetIDs), 1);
    });

    //Edit checking. This handles disabling select keys on empty fields.
    $(':text').keyup(function () {
        var $item_text = $(this).closest("td").find(":input[type='text']").attr('id');
        var $item = $(this).closest("td").find(".btn-primary ").attr('id');
        if ($("#" + $item_text).val() != "") {
            $("#" + $item).css('background-color', '#5a86de');
            $("#" + $item).removeAttr('disabled');
        } else {
            $("#" + $item).css('background-color', '#5a86de');
            $("#" + $item).attr('disabled', true);
        }
    });


    //Row Column button select events.
    $('.xs-col').click(function () {
        var item = $(this).closest("td").find(".btn-primary ").attr('id');
        var index = (parseInt(item.slice(-2))-1) ;
        item = (item.substr(0,item.length-2)+index);
        selectRow(item);
    });

    $('.xs-row').click(function () {
        var item = $(this).closest("td").find(".btn-primary ").attr('id');
        var index = item.slice(-1).charCodeAt(0) - 65; //Convert letters to numbers
        item = item.substr(0,item.length-1) + index;
        selectRow(item);
    });


    //Store session data to keep connections after post-back.
    function StoreSessions()
    {
        var data = localStorage.getItem("targetIDs");
        if (data != null) {
            targetIDs = JSON.parse(data);
            localStorage.removeItem("targetIDs");
        }
        data = localStorage.getItem("sourceIDs");
        if (data != null) {
            sourceIDs = JSON.parse(data);
            localStorage.removeItem("sourceIDs");
        }
        data = localStorage.getItem("workQueue");
        if (data != null) {
            workQueue = JSON.parse(data);
            localStorage.removeItem("workQueue");
        }
        data = localStorage.getItem("directionArr");
        if (data != null) {
            directionArr = JSON.parse(data);
            localStorage.removeItem("directionArr");
        }

    }

    //Check all the fields after a scan and enable they if the contain data
    for (i = 0; i < maxRackSize; i++) {
        enableFields("src", i);
        enableFields("dest", i);
    }

    function enableFields(rackTarget, index)
    {
        if($("#" + rackTarget + "RcpBcd0_" + index.toString()).val() !="") {
            var $button = $("[id$=_" + rackTarget + "_RcpBcd0_" + index.toString()+"]");
            $($button).css('background-color', '#5a86de');
            $($button).removeAttr('disabled');
        }
    }
});



