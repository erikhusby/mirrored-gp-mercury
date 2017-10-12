$(document).ready(function () {

   /****************************************************
    * This script is used for the cherry pick manual transfer.
    * It must be included in the manual_transfer.jsp page.
    ****************************************************/

   /*
    The functions in this file expect a page with the following structure:
    form
        div class=vessel-container data-direction=src data-event-index=0
            input name=...barcode class=container-barcode
            table
                button id=A01_src_RcpBcd0_...  data-position=A01 // for each position in geometry

        div class=vessel-container data-direction=dest data-event-index=0
            input name=...barcode class=container-barcode
            table
                button id=A01_dest_RcpBcd0_...  data-position=A01 // for each position in geometry

        div id=cherryPickSourceElements
            div class=sourceElements // added for each cherry pick source element
                input readonly name=...barcode
                input readonly name=...well
                input readonly name=...destinationBarcode
                input readonly name=...destinationWell
    The user:
        scans barcodes into container (and tube) barcode fields
        clicks buttons on source and destination, for each position, for a column, for a row or for an entire container
        clicks Add Cherry Picks
    The code:
        creates readonly inputs that are later POSTed to the action bean
        draws lines to connect the buttons
    */

    var maxRackSize = $( "[id^=src_TABLE] td").length;
    var sourceIndexRegex = /stationEvents\[[0-9]*].source\[([0-9]*)]/;

    /** @namespace jsPlumb */
    jsPlumb.ready(function () {

        //Initial jsPlumb setup of line types and endpoints.
        var instance = jsPlumb.getInstance({
            Connector: "StateMachine",
            PaintStyle: {strokeStyle: "red", lineWidth: 3},
            Endpoint: ["Dot", {radius: 5}],
            EndpointStyle: {fillStyle: "blue"},
            Container: "container0" // todo jmt container0 is not unique
        });

        $(document).ready(function () {
            $(window).scroll(function () {
                jsPlumb.repaintEverything();
            });
        });

        var endpointOptions = {isSource: true};
        var colorSpace;

        $('#ClearConnectionsButton').click(function () {
            instance.deleteEveryEndpoint();
            instance.detachEveryConnection();
            $j("#cherryPickSourceElements").empty();
        });

        $('#PreviewButton').click(function () {
            addCherryPicks();
            connect();
        });

        // Draw lines for any cherry picks that have been through a server roundtrip.
        connect();

        function getButtons(barcode, well) {
            var button;
            $j(".container-barcode").each(function (index, element) {
                if ($j(element).val() === barcode) {
                    button = $j(element).find("~table button[data-position='" + well + "']");
                }
            });
            return button;
        }

        function connect() {
            var arrowCommon = {foldback: 0.7, fillStyle: "blue", width: 14};
            var overlays = [
                ["Arrow", {location: 0.8}, arrowCommon],
                ["Arrow", {location: 0.3}, arrowCommon]
            ];

            $j(".sourceElements").each(function (index, element) {
                var barcode = $j(element).find("[name$='.barcode']").attr("value");
                var well = $j(element).find("[name$='.well']").attr("value");
                var sourceButton = getButtons(barcode, well);

                var destinationBarcode = $j(this).find("[name$='.destinationBarcode']").attr("value");
                var destinationWell = $j(this).find("[name$='.destinationWell']").attr("value");
                var destinationButton = getButtons(destinationBarcode, destinationWell);

                colorSpace = getRandomColor();
                var buttonDistance = Math.abs(destinationButton.position().top - sourceButton.position().top);
                var sourcePos = instance.addEndpoint(sourceButton, endpointOptions);
                instance.connect({
                    source: sourcePos,
                    target: destinationButton,
                    overlays: overlays,
                    paintStyle:{ strokeStyle:colorSpace, lineWidth:3 },
                    connector: ["StateMachine", { proximityLimit: -5, curviness: buttonDistance / 7 }]
                });
            })
        }

        function getContainerBarcode(element) {
            return $j(element).closest(".vessel-container").find(".container-barcode").val();
        }

        //Main handler for the addCherryPicks feature.
        function addCherryPicks() {
            var sourceContainers = $j(".vessel-container[data-direction='src']");
            var targetContainers = $j(".vessel-container[data-direction='dest']");
            var maxContainers = Math.max(sourceContainers.length, targetContainers.length);

            for (var containerIndex = 0; containerIndex < maxContainers; containerIndex++) {
                var sourceContainer = sourceContainers[Math.min(containerIndex, sourceContainers.length - 1)];
                var targetContainer = targetContainers[Math.min(containerIndex, targetContainers.length - 1)];

                var sourceButtons = $j(sourceContainer).find("button:contains('Selected')[id*='src_RcpBcd']");
                var destButtons = $j(targetContainer).find("button:contains('Selected')[id*='dest_RcpBcd']");
                if (destButtons.length === 0) {
                    continue;
                }
                if (!(sourceButtons.length === destButtons.length ||
                        (sourceButtons.length === 1 && destButtons.length > 1) ||
                        sourceButtons.length > 1 && destButtons.length === 1)) {
                    alert("Cherry pick Source and Destination must be same size, or one to many, or many to one.");
                    return;
                }
                var maxButtons = Math.max(sourceButtons.length, destButtons.length);

                var sourceElementsDiv = $j("#cherryPickSourceElements");
                for(var buttonIndex = 0; buttonIndex < maxButtons; buttonIndex++) {
                    var eventIndex = targetContainer.getAttribute("data-event-index");
                    var lastSourceElementForEvent = $j(".sourceElements input[name^='stationEvents\\[" + eventIndex +
                            "]").last();
                    var sourceElementIndex = 0;
                    if (lastSourceElementForEvent.length > 0) {
                        var match = sourceIndexRegex.exec(lastSourceElementForEvent[0].getAttribute("name"));
                        sourceElementIndex = parseInt(match[1]) + 1;
                    }
                    var namePrefix = "<input type='text' readonly name='stationEvents[" + eventIndex + "].source[" +
                            sourceElementIndex + "]";
                    sourceElementIndex++;
                    var div = $j("<div class='sourceElements'>");
                    var sourceButton = sourceButtons[Math.min(buttonIndex, sourceButtons.length - 1)];
                    var destButton = destButtons[Math.min(buttonIndex, destButtons.length - 1)];

                    var sourceContainerBarcode = getContainerBarcode(sourceButton);
                    var destContainerBarcode = getContainerBarcode(destButton);
                    if (!sourceContainerBarcode || !destContainerBarcode) {
                        alert("You must enter source and destination container barcodes before adding cherry picks.");
                        return;
                    }
                    sourceElementsDiv.append(div);
                    div.append(namePrefix + ".barcode' value='" + sourceContainerBarcode + "'>");
                    div.append(namePrefix + ".well' value='" + sourceButton.getAttribute("data-position") + "'>");
                    div.append("->");
                    div.append(namePrefix + ".destinationBarcode' value='" + destContainerBarcode + "'>");
                    div.append(namePrefix + ".destinationWell' value='" + destButton.getAttribute("data-position") + "'>");
                }
            }
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

    //Select rows and cols
    function selectRow(id) {

        id = id.replace("btn_",".");
        var termTableInputIDs = [];
        var count = 0;
        try {
          $(id).each(function () {
              var $item_text = $(this).closest("td").find(":input[type='text']").attr('id');
              if ($("#" + $item_text).val().length === 0) {
                  count++;
              }
          });
        }
        catch(err){
        }

        if(count > 0) {
            $('#'+$(document.activeElement).attr('id')).html('Select');
            alert('Barcodes must be present in every field');
            return false;
        }

        $(id).each(function () {
            id=$(this).attr('id');
            termTableInputIDs.push(id);
            $("#"+id).trigger('click');
        });
        return true;
    }

    //Main event handler to process all button clicks
    $('.btn-xs').click(function () {

        if ($(this).attr("id") === 'btnRow1') {
            $('table tbody tr ').click(function () {
            });
        }

        // todo jmt replace with css classes
        if ($(this).attr("id") === 'selectAllsrc') {
            if ($(this).text() === 'Selected') {
                $('[id^=src_TABLE] .btn-xs').text("Select");
                $('[id^=src_TABLE] .btn-xs').css('background-color', '#5a86de');
                $('[id^=src_TABLE] .xs-all').css('background-color', '#c266ff');
                $('[id^=src_TABLE] .xs-col').css('background-color', '#33cc00');
                $('[id^=src_TABLE] .xs-row').css('background-color', '#ff8c1a');
            }
            else {
                $('[id^=src_TABLE] .btn-xs').text("Selected");
                $('[id^=src_TABLE] .btn-xs').css('background-color', 'red');
            }
        }
        else {
            $(this).html($(this).text() === 'Select' ? 'Selected' : 'Select');

            if ($(this).text() === 'Selected') {
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

    //Edit checking. This handles disabling select keys on empty fields.
    $(':text').keyup(function () {
        var $item_text = $(this).closest("td").find(":input[type='text']").attr('id');
        var $item = $(this).closest("td").find(".btn-primary ").attr('id');
        if ($("#" + $item_text).val() !== "") {
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


    //Check all the fields after a scan and enable them if they contain data
    $j(".vessel-container[data-direction='src']").each(function (index, element) {
        for (var i = 0; i < maxRackSize; i++) {
            enableFields("src", element.getAttribute("data-event-index"), i);
        }
    });
    $j(".vessel-container[data-direction='dest']").each(function (index, element) {
        for (var i = 0; i < maxRackSize; i++) {
            enableFields("dest", element.getAttribute("data-event-index"), i);
        }
    });

    function enableFields(rackTarget, eventIndex, fieldIndex)
    {
        if($("#" + rackTarget + "RcpBcd" + eventIndex + "_" + fieldIndex.toString()).val() !== "") {
            var $button = $("[id$=_" + rackTarget + "_RcpBcd" + eventIndex + "_" + fieldIndex.toString()+"]");
            $($button).css('background-color', '#5a86de');
            $($button).removeAttr('disabled');
        }
    }
});
