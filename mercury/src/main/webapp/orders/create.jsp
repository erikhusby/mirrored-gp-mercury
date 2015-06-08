<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.entity.products.Product" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.submitString}: ${actionBean.editOrder.title}"
                       sectionTitle="${actionBean.submitString}: ${actionBean.editOrder.title}">

    <stripes:layout-component name="extraHead">
    <style type="text/css">
        .multiselect {
            width: 397px;
            height:7em;
            border:solid 1px #c0c0c0;
            overflow:auto;
        }

        .multiselect label {
            display:block;
            margin: 0px;
            padding-left: 15px;
        }

        .multiselect label.group {
            padding-left: 5px;
            color:#555555;
            font-weight: bold;
            margin-left: 5px;
        }

        .multiselect-on {
            color:#ffffff;
            background-color:#0076da;
        }

        /* Override Bootstrap's block display of labels for add-ons to constrain the hit box. */
        #addOnCheckboxes label {
            display: inline;
            vertical-align: bottom;
        }
    </style>
        <script type="text/javascript">

        var duration = {'duration' : 400};
        var kitDefinitionCount = 0;
        var readOnlyOrder = ${!actionBean.editOrder.draft};
        var disabledText = '';
        var readonlyText = '';
        if (readOnlyOrder) {
            disabledText = ' disabled="disabled" ';
            readonlyText = ' readonly="readonly" ';
        }

        $j(document).ready(

                function () {
                    jQuery.fn.multiselect = function() {
                        $j(this).each(function() {
                            var checkboxes = $j(this).find("input:checkbox");
                            checkboxes.each(function() {
                                var checkbox = $j(this);
                                // Highlight pre-selected checkboxes
                                if (checkbox.prop("checked"))
                                    checkbox.parent().addClass("multiselect-on");

                                // Highlight checkboxes that the user selects
                                checkbox.click(function() {
                                    if (checkbox.prop("checked"))
                                        checkbox.parent().addClass("multiselect-on");
                                    else
                                        checkbox.parent().removeClass("multiselect-on");
                                });
                            });
                        });
                    };

                    $j('#productList').dataTable({
                        "oTableTools": ttExportDefines,
                        "aaSorting": [
                            [1, 'asc']
                        ],
                        "aoColumns": [
                            {"bSortable": true},
                            {"bSortable": true},
                            {"bSortable": true},
                            {"bSortable": true},
                            {"bSortable": true},
                            {"bSortable": true},
                            {"bSortable": true},
                            {"bSortable": true},
                            {"bSortable": true},
                            {"bSortable": true},
                            {"bSortable": true},
                            {"bSortable": false}
                        ]
                    });

                    $j("#owner").tokenInput(
                            "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                hintText: "Type a name",
                                prePopulate: ${actionBean.ensureStringResult(actionBean.owner.completeData)},
                                tokenLimit: 1,
                                tokenDelimiter: "${actionBean.owner.separator}",
                                resultsFormatter: formatInput,
                                autoSelectFirstResult: true
                            }
                    );

                    $j("#researchProject").tokenInput(
                            "${ctxpath}/projects/project.action?projectAutocomplete=", {
                                hintText: "Type a Research Project key or title",
                                onAdd: updateUIForProjectChoice,
                                onDelete: updateUIForProjectChoice,
                                prePopulate: ${actionBean.ensureStringResult(actionBean.projectTokenInput.completeData)},
                                resultsFormatter: formatInput,
                                tokenDelimiter: "${actionBean.projectTokenInput.separator}",
                                tokenLimit: 1,
                                autoSelectFirstResult: true
                            }
                    );

                    $j("#product").tokenInput(
                            "${ctxpath}/orders/order.action?productAutocomplete=", {
                                hintText: "Type a Product name or Part Number   ",
                                onAdd: updateUIForProductChoice,
                                onDelete: updateUIForProductChoice,
                                resultsFormatter: formatInput,
                                prePopulate: ${actionBean.ensureStringResult(actionBean.productTokenInput.completeData)},
                                tokenDelimiter: "${actionBean.productTokenInput.separator}",
                                tokenLimit: 1,
                                autoSelectFirstResult: true
                            }
                    );
                    $j("#kitCollection").tokenInput(
                            "${ctxpath}/orders/order.action?groupCollectionAutocomplete=", {
                                hintText: "Search for group and collection",
                                prePopulate: ${actionBean.ensureStringResult(actionBean.bspGroupCollectionTokenInput.getCompleteData(!actionBean.editOrder.draft)) },
                                onAdd: updateUIForCollectionChoice,
                                onDelete: updateUIForCollectionChoice,
                                resultsFormatter: formatInput,
                                tokenDelimiter: "${actionBean.bspGroupCollectionTokenInput.separator}",
                                tokenLimit: 1,
                                autoSelectFirstResult: true
                            }
                    );

                    $j("#shippingLocation").tokenInput(
                            getShippingLocationURL, {
                                hintText: "Search for shipping location",
                                prePopulate: ${actionBean.ensureStringResult(actionBean.bspShippingLocationTokenInput.getCompleteData(!actionBean.editOrder.draft))},
                                resultsFormatter: formatInput,
                                tokenDelimiter: "${actionBean.bspShippingLocationTokenInput.separator}",
                                tokenLimit: 1,
                                autoSelectFirstResult: true
                            }
                    );

                    $j("#notificationList").tokenInput(
                            "${ctxpath}/orders/order.action?anyUsersAutocomplete=", {
                                hintText: "Enter a user name",
                                prePopulate: ${actionBean.ensureStringResult(actionBean.notificationListTokenInput.getCompleteData(!actionBean.editOrder.draft))},
                                tokenDelimiter: "${actionBean.notificationListTokenInput.separator}",
                                preventDuplicates: true,
                                resultsFormatter: formatInput,
                                autoSelectFirstResult: true
                            }
                    );

                    $j("#fundingDeadline").datepicker();
                    $j("#publicationDeadline").datepicker();
                    $j("#sampleListEdit").hide();

                    $j("#duplicateKitInfoDialog").dialog({
                        modal: true,
                        autoOpen: false,
                        buttons: [
                            {
                                id: "createDuplicates",
                                text: "create",
                                click: function() {
                                    $j(this).dialog("close");
                                    var chosenId = $j("#idToDuplicate").val();
                                    var idsToDuplicate = $j("#numDuplicatesId").val();
                                    $j("#idToDuplicate").val('');
                                    $j("#numDuplicatesId").val('')
                                    for(var i=0;i<idsToDuplicate ;i++) {
                                        cloneKitDefinition(chosenId );
                                    }
                                }
                            },
                            {
                                id: "cancelCreateDuplicates",
                                text: "Cancel",
                                click: function () {
                                    $j(this).dialog("close");
                                }
                            }
                        ]
                    });

                    // Initializes the previously chosen sample kit detail info in the sample kit display section
                    <c:forEach items="${actionBean.kitDetails}" var="initKitDetail" varStatus="kitDetailStatus">
                    addKitDefinitionInfo('${initKitDetail.productOrderKitDetailId}', '${initKitDetail.numberOfSamples}',
                            '${initKitDetail.organismId}', '${initKitDetail.bspMaterialName}',
                            '${initKitDetail.kitType.name}');
                    </c:forEach>
                    var kitDetailLength = ${fn:length(actionBean.kitDetails)};

                    // if there are no sample kit details, just show one empty detail section
                    if (kitDetailLength < 1) {
                        addKitDefinitionInfo('');
                    }

                    // add an on change event for each material info drop down on the screen.  There may be more than
                    // one so we need to have a change method for each and have that change only affect the post
                    // processing options in that block.
                    $j("[id^=materialInfo]").change(function (e) {

                        var chosenId = $j(this).attr("id");
                        var index = chosenId.substr("materialInfo".length, chosenId.length);

                        updateUIForMaterialInfoChoice(index, getSelectedPostReceiveOptions(index));
                    });
                    $j("#skipQuoteDiv").hide();
                    updateUIForProductChoice();
                    updateUIForProjectChoice();
                    updateFundsRemaining();
                    updateUIForCollectionChoice();
                    initializeQuoteOptions();

                    $j("#skipQuote").on("change", toggleSkipQuote);
                    $j("#skipRegulatoryInfoCheckbox").on("change", toggleSkipRegulatory);
                    $j("#regulatorySelect").change(function () {
                        $j("#attestationConfirmed").attr("checked", false)
                    });
                }

        );

        function formatInput(item) {
            var extraCount = (item.extraCount == undefined) ? "" : item.extraCount;
            return "<li>" + item.dropdownItem + extraCount + '</li>';
        }

        var fadeDuration = {'duration': 800};

        var addOn = [];
        <c:forEach items="${actionBean.addOnKeys}" var="addOnProduct">
        addOn['${addOnProduct}'] = true;
        </c:forEach>

        var quoteBeforeSkipping;
        function toggleSkipRegulatory() {
            var skipRegulatoryChecked = $j("#skipRegulatoryInfoCheckbox").prop("checked");
            $j("#attestationConfirmed").attr("checked", false);
            handleUpdateRegulatory(skipRegulatoryChecked);
        }

        function handleUpdateRegulatory(skipRegulatoryChecked){
            if (skipRegulatoryChecked) {
                $j("#regulatorySelect :selected").prop("selected", false)
                $j("#regulatorySelect").hide();
                $j("#skipRegulatoryDiv").show();
            } else {
                $j("#skipRegulatoryInfoReason").val("");
                $j("#skipRegulatoryDiv").hide();
                $j("#regulatorySelect").show();
                populateRegulatorySelect();
            }
        }

        function toggleSkipQuote() {
            skipQuote = $j("#skipQuote").prop('checked');

            if (skipQuote) {
                quoteBeforeSkipping = $j("#quote").val();
                $j("#quote").val('');
            } else {
                $j("#skipQuoteReason").val('');
                $j("#quote").val(quoteBeforeSkipping);
            }
            updateQuoteOptions();
        }

        function initializeQuoteOptions() {
            <c:if test="${actionBean.editOrder.canSkipQuote()}">
                skipQuote = $j("#skipQuoteReason").is(":empty");
                $j("#skipQuote").prop('checked', skipQuote);
            </c:if>
            updateQuoteOptions();
            quoteBeforeSkipping = "${actionBean.editOrder.quoteId}";
        }

        function updateQuoteOptions() {
            skipQuote = $j("#skipQuote").prop('checked');
            $j("#skipQuote").prop('checked', skipQuote);
            if (skipQuote) {
                $j("#skipQuoteReasonDiv").show();
                $j("#quote").hide();
                $j("#fundsRemaining").hide();
            } else {
                $j("#skipQuoteReasonDiv").hide();
                updateFundsRemaining();
                $j("#quote").show();
                $j("#fundsRemaining").show();
            }
        }

        var postReceiveOption = [
            {}
        ];
        <c:forEach items="${actionBean.postReceiveOptionKeys}" var="kitOption" varStatus="stat" >
            postReceiveOption[${kitOption.key}] = {};
            <c:forEach items="${kitOption.value}" var="option" varStatus="optionStat">

                postReceiveOption[${kitOption.key}]["${option}"] = true;
            </c:forEach>
            <%--postReceiveOption[${kitOption.key}].length =${fn:length(kitOption.value)};--%>
        </c:forEach>

        <%--postReceiveOption.length = ${fn:length(actionBean.postReceiveOptionKeys)};--%>

        function updateUIForProjectChoice(){
            var projectKey = $j("#researchProject").val();
            var skipRegulatory = false;
            skipRegulatory = ${actionBean.editOrder.canSkipRegulatoryRequirements()};
            $j("#skipRegulatoryInfoCheckbox").prop('checked', skipRegulatory);

            if (projectKey == null || projectKey == "") {
                $j("#regulatorySelect").text('When you select a project, its regulatory options will show up here');
                $j("#regulatoryActive").hide();
                $j("#attestationDiv").hide();
                $j("#skipRegulatoryDiv").hide();
                $j("#regulatoryInfo").hide();
                skipRegulatory = false;
            }else{
                $j("#regulatorySelect").contents().filter(function () {
                    return this.nodeType == Node.TEXT_NODE;
                  }).remove();
                 if (!skipRegulatory) {
                    populateRegulatorySelect();
                 }
                $j("#regulatoryInfo").show();
                $j("#regulatoryActive").show();
                $j("#attestationDiv").show();

            }
            handleUpdateRegulatory(skipRegulatory);
        }

        function populateRegulatorySelect() {
            var projectKey = $j("#researchProject").val();
            var pdoId = "${actionBean.editOrder.productOrderId}";

            if (projectKey) {
            $j.ajax({
                url: "${ctxpath}/orders/order.action?getRegulatoryInfo=&researchProjectKey=" + projectKey + "&pdoId=" + pdoId,
                dataType: 'json',
                success: setupRegulatoryInfoSelect
            });
            }

        }

        function updateUIForProductChoice() {

            var productKey = $j("#product").val();
            if ((productKey == null) || (productKey == "")) {
                $j("#addOnCheckboxes").text('If you select a product, its Add-ons will show up here');
                $j("#sampleInitiationKitRequestEdit").hide();
                $j("#numberOfLanesDiv").fadeOut(duration);
                $j("#skipQuoteDiv").hide();
                $j("#quote").show();

            } else {
                if (productKey == '<%= Product.SAMPLE_INITIATION_PART_NUMBER %>') {
                    // Product is Sample Initiation "P-ESH-0001".
                    $j("#samplesToAdd").val('');
                    $j("#sampleListEdit").hide();
                    $j("#sampleInitiationKitRequestEdit").show();
                } else {
                    $j("#sampleListEdit").show();
                    $j("#sampleInitiationKitRequestEdit").hide();
                }
                $j.ajax({
                    url: "${ctxpath}/orders/order.action?getAddOns=&product=" + productKey,
                    dataType: 'json',
                    success: setupAddonCheckboxes
                });

                $j.ajax({
                    url: "${ctxpath}/orders/order.action?getSupportsNumberOfLanes=&product=" + productKey,
                    dataType: 'json',
                    success: updateNumberOfLanesVisibility
                });

                $j.ajax({
                    url: "${ctxpath}/orders/order.action?getSupportsSkippingQuote=&product=" + productKey,
                    dataType: 'json',
                    success: updateSkipQuoteVisibility
                });
            }
        }

        function updateUIForMaterialInfoChoice(kitCount, postReceivePrePopulates) {
            var pdoId = "${actionBean.editOrder.productOrderId}";
            var materialKey = $j("#materialInfo" + kitCount).val();

            if ((materialKey == null) || (materialKey == "")) {
                $j("#postReceiveCheckboxGroup" + kitCount).hide();
            } else {
                $j.ajax({
                    url: "${ctxpath}/orders/order.action?getPostReceiveOptions=&materialInfo=" + materialKey + "&productOrder=" + pdoId + "&${actionBean.kitDefinitionIndexIdentifier}=" + kitCount +"&prepopulatePostReceiveOptions="+postReceivePrePopulates,
                    dataType: 'json',
                    success: setupPostReceiveCheckboxes
                });
                $j("#postReceiveCheckboxGroup" + kitCount).show();
            }
        }

        function updateOrganism(index, presetOrganism) {

            $j.ajax({
                url: "${ctxpath}/orders/order.action?collectionOrganisms=&bspGroupCollectionTokenInput.listOfKeys=" + $j("#kitCollection").val() + "&${actionBean.kitDefinitionIndexIdentifier}=" + index + "&prePopulatedOrganismId=" + presetOrganism,
                dataType: 'json',
                success: setupOrganismMenu
            });
        }
        function updateUIForCollectionChoice() {
            var collectionKey = $j("#kitCollection").val();
            if ((collectionKey == null) || (collectionKey == "") || (collectionKey == "Search for collection and group")) {
                $j("[id^=selectedOrganism]").html('<div class="controls-text">Choose a collection to show related organisms</div>');

                $j("#shippingLocationSelection").parent().append('<div class="controls" id="sitePrompt"><div class="controls-text">Choose a collection to show related shipping locations</div></div>');
                $j("#shippingLocationSelection").hide();

                // This is not null safe, so we must make a check to ensure the UI is not affected.
                if ($j("#shippingLocation").val() != null) {
                    $j("#shippingLocation").tokenInput("clear");
                }
            } else {
                $j("[id^=selectedOrganism]").each(function () {

                    var organismId = $j(this).attr("id");
                    var index = organismId.substr("selectedOrganism".length, organismId.length);
                    var selectedOrganismId = $j(this).val();
                    updateOrganism(index, selectedOrganismId);
                });

                $j("#sitePrompt").remove();
                $j("#shippingLocationSelection").show();
            }
        }

        function setupOrganismMenu(data) {
            var collection = data.collectionName;

            var kitDefinitionIndex = data.kitDefinitionQueryIndex;

            var organisms = data.organisms;
            if ((organisms == null) || (organisms.length == 0)) {
                $j("#selectedOrganism" + kitDefinitionIndex).text("The collection '" + collection + "' has no organisms");
                return;
            }

            // Even though the id is a long, if there is no value then this needs something empty, so using a string
            // for the long or empty will work in the later comparison.
            var selectedOrganismId = data.chosenOrganism;

            var organismSelect = '<select id="organismOption' + kitDefinitionIndex +
                    '" '+
                    disabledText;

            organismSelect += 'name="kitDetails[' + kitDefinitionIndex + '].organismId">';

            $j.each(organisms, function (index, organism) {
                var selectedString = (organism.id == selectedOrganismId) ? 'selected="selected"' : '';
                organismSelect += '  <option value="' + organism.id + '" ' + selectedString + '>' + organism.name + '</option>';
            });
            organismSelect += '</select>';

            $j("#selectedOrganism" + kitDefinitionIndex).hide();
            $j("#selectedOrganism" + kitDefinitionIndex).html(organismSelect);
            $j("#selectedOrganism" + kitDefinitionIndex).fadeIn(fadeDuration);
        }

        // This function allows the shippingLocation input token to be able to automatically pass the selected
        // collection id to filter the available shipping sites to only ones in that collection.
        function getShippingLocationURL() {
            return "${ctxpath}/orders/order.action?shippingLocationAutocomplete=&bspGroupCollectionTokenInput.listOfKeys="
                    + $j("#kitCollection").val();
        }

        function updateNumberOfLanesVisibility(data) {
            var numberOfLanesDiv = $j("#numberOfLanesDiv");

            data.supportsNumberOfLanes ? numberOfLanesDiv.fadeIn(fadeDuration) : numberOfLanesDiv.fadeOut(fadeDuration);
        }

        function updateSkipQuoteVisibility(data) {
            var skipQuoteDiv = $j("#skipQuoteDiv");
            var quoteDiv = $j("#quote");
            if (data.supportsSkippingQuote) {
                skipQuoteDiv.show();
                quoteDiv.hide();
            }
            else {
                $j("#skipQuote").prop('checked', false);
                skipQuoteDiv.hide();
                quoteDiv.show();
            }
            updateQuoteOptions();
        }

        function setupRegulatoryInfoSelect(data){
            $j("#regulatorySelect").empty();
            if (data.length == 0) {
                $j("#regulatorySelect").text('No options have been set up in the research project. ');
                var researchProject = $j("#researchProject").val();
                if (researchProject != null && researchProject!="") {
                    var link = $j('<a/>');
                    $j(link).attr('href', '${ctxpath}/projects/project.action?view='.concat('&researchProject=' + researchProject));
                    $j(link).append("(".concat(researchProject).concat(")"))
                    $j("#regulatorySelect").append(link);
                }
            } else {
                var maxSize = 5;
                var size = 0;
                var multiSelectDiv = $j('<div class="multiselect"></div>"');
                $j.each(data, function (index, val) {
                    size++;
                    var projectName = val.group;
                    var regulatoryList = val.value;
                    $j(multiSelectDiv).append('<label class = "group">' + projectName + '</label>');
                    for (var index in regulatoryList) {
                        size++;
                        var row = $j('<label></label>');
                        var input = $j('<input type = "checkbox" name="selectedRegulatoryIds" />');
                        if (regulatoryList[index].selected) {
                            $j(input).attr("checked", "");
                        }

                        $j(input).attr('value', regulatoryList[index].key);
                        $j(row).append(input);
                        $j(row).append(regulatoryList[index].value);
                        $j(multiSelectDiv).append(row);
                    }
                });

                var selectDiv = $j("#regulatorySelect");
                selectDiv.hide();
                selectDiv.append(multiSelectDiv);

                if (size < maxSize) {
                    size=maxSize;
                }
                $j(".multiselect").attr('style', 'height: ' + size + "em");
                $j(function () {
                    $j(".multiselect").multiselect();
                });
                selectDiv.fadeIn();
            }
        }

        function setupAddonCheckboxes(data) {
            var productTitle = $j("#product").val();

            if (data.length == 0) {
                $j("#addOnCheckboxes").text("The product '" + productTitle + "' has no Add-ons");
                return;
            }

            var checkboxText = "";
            var checked;

            $j.each(data, function (index, val) {
                // if this value is in the add on list, then check the checkbox
                checked = '';
                if (addOn[val.key]) {
                    checked = ' checked="checked" ';
                }

                var addOnId = "addOnCheckbox-" + index;
                checkboxText += '  <input id="' + addOnId + '" type="checkbox"' + checked + ' name="addOnKeys" value="' + val.key + '"/>';
                checkboxText += '  <label style="font-size: x-small;" for="' + addOnId + '">' + val.value + ' [' + val.key + ']</label>';
                checkboxText += '  <br>';
            });

            var checkboxes = $j("#addOnCheckboxes");
            checkboxes.hide();
            checkboxes.html(checkboxText);
            checkboxes.fadeIn(fadeDuration);
        }

        function setupPostReceiveCheckboxes(data) {
            var kitIndex = data.kitDefinitionQueryIndex;

            var materialInfo = $j("#materialinfo" + kitIndex).val();

            if (data.dataList.length == 1) {
                $j("#postReceiveCheckboxes" + kitIndex).text("The Material Type '" + materialInfo + "' has no Post-receive options.");
                return;
            }

            var checkboxText = "";
            var checked;
            $j.each(data.dataList, function (index, val) {
                // if this value is in the add on list, then check the checkbox
                var optionValue = val.key;
                var defaultChecked = val.checked;
                var optionLabel = val.label;
                checked = '';

                if (!postReceiveOption[kitIndex] || postReceiveOption[kitIndex].length == 0) {
                    if (defaultChecked) {
                        checked = ' checked="checked" ';
                    }
                } else if (postReceiveOption[kitIndex][val.key]) {
                    checked = ' checked="checked" ';
                }

                var postReceiveId = "postReceiveCheckbox-" + index;
                checkboxText += '  <input type="checkbox"' + checked + disabledText +
                        ' name="postReceiveOptionKeys[' + kitIndex + ']" value="' + optionValue + '"/>' +
                        '  <label style="font-size: x-small;" for="' + postReceiveId + '">' +
                        optionLabel  + '</label>';
            });
            var checkboxes = $j("#postReceiveCheckboxes" + kitIndex);
            checkboxes.hide();
            checkboxes.html(checkboxText);

            if(data.prepopulatePostReceiveOptions) {

                var prePopPostReceives = data.prepopulatePostReceiveOptions.split("/,/");

                if (prePopPostReceives) {

                    $j("input[name='postReceiveOptionKeys[" + kitIndex + "]']").each(function () {
                        var checkedValue = '';
                        if ($j.inArray($j(this).val(), prePopPostReceives) != -1) {
                            checkedValue = 'checked';
                        }
                        $j(this).prop("checked", checkedValue);
                    });
                }
            }

            checkboxes.fadeIn(fadeDuration);
        }

        function updateFundsRemaining() {
            var quoteIdentifier = $j("#quote").val();
            if ($j.trim(quoteIdentifier)) {
                $j.ajax({
                    url: "${ctxpath}/orders/order.action?getQuoteFunding=&quoteIdentifier=" + quoteIdentifier,
                    dataType: 'json',
                    success: updateFunds
                });
            } else {
                $j("#fundsRemaining").text('');
            }
        }

        function updateFunds(data) {
            if (data.fundsRemaining) {
                $j("#fundsRemaining").text('Funds Remaining: ' + data.fundsRemaining);
            } else {
                $j("#fundsRemaining").text('Error: ' + data.error);
            }
        }

        function formatUser(item) {
            return "<li><div class=\"ac-dropdown-text\">" + item.name + "</div>" +
                    "<div class=\"ac-dropdown-subtext\">" + item.username + " " + item.email + "</div>" +
                    item.extraCount + '</li>'
        }

        /**
         *
         * @param id    kit index ID fo the block of sample kit detail info that is being removed
         */
        function removeKitDefinition(id) {

            var kitRefID = $j("#kitIdReference" + id).attr("value");

            if(kitRefID) {
                var deleteString = '<input type="hidden" name="deletedKits" value="' + kitRefID +'" />';
                $j("#kitDefinitions").append(deleteString);
            }

            $j("#kitDefinitionDetail" + id).remove();
        }

        /**
         *
         * When called, cloneKitDefinition will create a new sample kit detail block of form fields.  The function will
         * also, grab sample kit detail info defined in a previous form block (referenced by the id parameter) and
         * prepopulate the new fields with the values from the previous block
         *
         * @param id    kit index ID of the block of sample kit detail info that is being cloned
         */
        function cloneKitDefinition(id) {

            var kitDefinitionId = '';
            var numberOfSamples = $j("#numberOfSamples" + id).val();
            var materialInfo = $j("#materialInfo" + id).val();
            var kitType = $j("#kitType" + id).val();
            var organism = $j("#organismOption" + id).val();

            addKitDefinitionInfo(kitDefinitionId, numberOfSamples, organism, materialInfo, kitType, id)
        }

        function showDuplicateKitInfoDialog(id) {
            $j("#duplicateKitInfoDialog").dialog("open").dialog("option", "width", 300);
            $j("#idToDuplicate").val(id);
        }

        /**
         * addKitDefinitionInfo builds the template form fields for displaying the details for sample kits.  A sample
         * initiation request can have multiple sample kit details so this function can be called multiple times
         * to set up each one.
         *
         * @param kitDefinitionId   The Database ID associated with the sample kit detail entity, if there is one.
         *                          Will be stored in a hidden field to associate the kit details with the previously
         *                          saved entity
         * @param samples           The number of samples for the kit defined for the sample kit detail info being
         *                          rendered.  If set, this will be used to pre-populate the number of samples input
         *                          field
         * @param organism          The reference ID for the organism chosen for the sample kit detail info being
         *                          rendered.  If set, this will be used to pre-select the correct organism in the
         *                          organism dropdown
         * @param material          The name of the source material chosen for the sample kit detail info being
         *                          rendered.  If set, this will be used to pre-select the correct source material in
         *                          the material dropdown
         * @param kitType           The reference name for the kit type (receptacle type) fo the sample kit detail info
         *                          being rendered.  If set, this will be used to pre-select the correct kit type in
         *                          the kit type drop down
         */
        function addKitDefinitionInfo(kitDefinitionId, samples, organism, material, kitType, cloneId) {

            var newDefinition = '<div id="kitDefinitionDetail' + kitDefinitionCount + '">';

            newDefinition += '<input type="hidden" id="kitIdReference' + kitDefinitionCount +
                    '" value="' + kitDefinitionId +
                    '" name="kitDetails[' + kitDefinitionCount + '].productOrderKitDetailId" />';
            newDefinition += '<div class="control-group">\n ';
            if(!readOnlyOrder && kitDefinitionCount>0) {

                newDefinition += '<a onclick="removeKitDefinition(' + kitDefinitionCount + ')" id="remove'+kitDefinitionCount+'">Remove kit Definition</a>\n ';
            }
            newDefinition += '<h5 >Kit Definition Info</h5>';
            newDefinition += '<div class="controls">\n';
            newDefinition += '</div>\n</div>';

            newDefinition += '<div class="control-group">\n ';

            // Number of Samples
            newDefinition += '<label for="numberOfSamples' + kitDefinitionCount + '" class="control-label">Number of Samples </label>\n';
            newDefinition += '<div class="controls">\n' +
                    '<input type=text ' + readonlyText + ' id="numberOfSamples' + kitDefinitionCount +
                    '" name="kitDetails[' + kitDefinitionCount + '].numberOfSamples"\n' +
                    'class="defaultText" title="Enter the number of samples"/>\n' +
                    '</div>\n</div>';

            // Kit type
            newDefinition += '<div class="control-group">\n<label for="kitType' + kitDefinitionCount +
                    '" class="control-label">' +
                    'Kit Type</label>' +
                    '<div class="controls">' +
                    '<select '+disabledText+' id="kitType' + kitDefinitionCount +
                    '" name="kitDetails[' + kitDefinitionCount + '].kitType">';
            <%-- FIXME SGM (GPLIM-2463): Bad fill.  Replace with jQuery Call to get tube to matrix kit combo, until a
            better solution for that relationship is found --%>
            newDefinition += '<option value="DNA_MATRIX" >Matrix Tube [0.75mL]</option>' +
                    '</select>' +
                    <c:if test="${!actionBean.editOrder.draft}">
                    '<input type=hidden name="kitDetails[' + kitDefinitionCount + '].kitType" id="kitTypeHidden'+kitDefinitionCount+'"/>' +
                    </c:if>
                    '</div></div>';

            // Material Info
            newDefinition += '<div class="control-group">\n' +
                    '<label for="materialInfo' + kitDefinitionCount + '" class="control-label">\n' +
                    'Material Information' +
                    '</label>\n' +
                    '<div class="controls">\n' +
                    '<select id="materialInfo' + kitDefinitionCount + '" '+disabledText+
                    ' name="kitDetails[' + kitDefinitionCount + '].bspMaterialName">' +
                    '<option value="" selected="selected">Choose...</option>';
            var checked = '';
            <c:forEach items="${actionBean.dnaMatrixMaterialTypes}" var="materialType">

            newDefinition += '<option value="${materialType.text}" >${materialType.text}</option>';
            checked = '';
            </c:forEach>
            newDefinition += '</select>';

            if (readOnlyOrder) {
                newDefinition += '<input type=hidden name="kitDetails[' + kitDefinitionCount + '].bspMaterialName"/>';
            }
            newDefinition += '</div></div>';
            // Organism

            newDefinition += ' <div class="control-group">' +
                    '<label for="selectedOrganism' + kitDefinitionCount + '" class="control-label">Organism' +
                    '</label>\n' +
                    '<div id="selectedOrganism' + kitDefinitionCount + '" class="controls"></div>' +
                    '</div>';

            // Post Receive Options
            newDefinition += '<div id="postReceiveCheckboxGroup' + kitDefinitionCount + '" class="control-group">' +
                    '<label for="selectedPostReceiveOptions' + kitDefinitionCount + '" class="control-label">' +
                    'Post-Receive Options' +
                    '</label>' +
                    '<div id="postReceiveCheckboxes' + kitDefinitionCount + '" class="controls controls-text"></div>' +
                    '</div>';

            if(!readOnlyOrder) {

                newDefinition += ' <div class="control-group">' +
                        '<div class="controls">\n' +
                        '<a onclick="showDuplicateKitInfoDialog(' + kitDefinitionCount + ')" id="clone'+kitDefinitionCount+'" >Duplicate kit Definition</a>\n ' +
                        '</div></div>';
            }

            newDefinition += '</div>';
            $j('#kitDefinitions').append(newDefinition);

            if(samples) {
                $j('#numberOfSamples' + kitDefinitionCount).val(samples);
            }
            if(material) {
                $j('#materialInfo' + kitDefinitionCount).val(material);
            }
            if(kitType) {
                $j('#kitType' + kitDefinitionCount).val(kitType);
                $j('#kitTypeHidden' + kitDefinitionCount).val(kitType);
            }

            updateOrganism(kitDefinitionCount, organism);

            var postReceivePrePopulates = getSelectedPostReceiveOptions(cloneId);

            updateUIForMaterialInfoChoice(kitDefinitionCount, postReceivePrePopulates);

            kitDefinitionCount++;
        }

        function getSelectedPostReceiveOptions(kitIndex) {

            var postReceivePrePopulates = '';

            var first = true;

            if(kitIndex) {

                $j("input[name='postReceiveOptionKeys["+kitIndex+"]']:checked").each(function() {
                    if(!first) {
                        postReceivePrePopulates += "/,/";
                    }
                    postReceivePrePopulates += $j(this).val();

                    first = false;
                });
            }
            return postReceivePrePopulates;
        }

        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

    <div id="duplicateKitInfoDialog" style="width:500px;display:none;">
        <p><label style="float:left;width:60px;" for="numDuplicatesId"># of kit duplicates to create?</label>
            <input type="hidden" id="idToDuplicate" />
            <input type="text" id="numDuplicatesId" name="numDuplicates" style="float:left;margin-right: 5px;"/>
        </p>
    </div>

        <stripes:form beanclass="${actionBean.class.name}" id="createForm">
            <div class="form-horizontal span6">
                <stripes:hidden name="productOrder"/>
                <stripes:hidden name="submitString"/>
                <div class="control-group">
                    <stripes:label for="orderName" class="control-label">
                        Name <c:if test="${actionBean.editOrder.draft}">*</c:if>
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="orderName" name="editOrder.title" class="defaultText input-xlarge"
                            maxlength="255" title="Enter the name of the new order"/>
                    </div>
                </div>

                <div class="view-control-group control-group" style="margin-bottom: 20px;">
                    <label class="control-label">ID</label>
                    <div class="controls">
                        <div class="form-value">
                            <c:choose>
                                <c:when test="${actionBean.editOrder == null || actionBean.editOrder.draft}">
                                    DRAFT
                                </c:when>
                                <c:otherwise>
                                    <a target="JIRA" href="${actionBean.jiraUrl(actionBean.editOrder.jiraTicketKey)}" class="external" target="JIRA">
                                            ${actionBean.editOrder.jiraTicketKey}
                                    </a>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="owner" class="control-label">
                        Owner *
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="owner" name="owner.listOfKeys" />
                    </div>
                </div>

                <c:choose>
                    <c:when test="${actionBean.editOrder.draft}">
                        <div class="control-group">
                            <stripes:label for="researchProject" class="control-label">
                                Research Project
                            </stripes:label>
                            <div class="controls">
                                <stripes:text
                                        readonly="${not actionBean.editOrder.draft}"
                                        id="researchProject" name="projectTokenInput.listOfKeys"
                                        class="defaultText"
                                        title="Enter the research project for this order"/>
                            </div>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="view-control-group control-group" style="margin-bottom: 20px;">
                            <label class="control-label">Research Project</label>

                            <div class="controls">
                                <div class="form-value">
                                    <stripes:hidden id="researchProject" name="projectTokenInput.listOfKeys"
                                                    value="${actionBean.editOrder.researchProject.jiraTicketKey}"/>
                                    <stripes:link title="Research Project"
                                                  beanclass="<%=ResearchProjectActionBean.class.getName()%>"
                                                  event="view">
                                        <stripes:param name="<%=ResearchProjectActionBean.RESEARCH_PROJECT_PARAMETER%>"
                                                       value="${actionBean.editOrder.researchProject.businessKey}"/>
                                        ${actionBean.editOrder.researchProject.title}
                                    </stripes:link>
                                    (<a target="JIRA"
                                        href="${actionBean.jiraUrl(actionBean.editOrder.researchProject.jiraTicketKey)}"
                                        class="external" target="JIRA">
                                        ${actionBean.editOrder.researchProject.jiraTicketKey}
                                </a>)
                                </div>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${actionBean.editOrder.regulatoryInfoEditAllowed}">
                        <div class="control-group">
                            <stripes:label for="regulatoryInfo" class="control-label">
                                Regulatory Information
                            </stripes:label>


                            <div id="regulatoryActive" class="controls">
                                <stripes:checkbox name="skipRegulatoryInfo" id="skipRegulatoryInfoCheckbox"
                                       title="Click if no IRB/ORSP review is required."/>No IRB/ORSP Review Required
                            </div>
                            <div id="skipRegulatoryDiv" class="controls controls-text">
                                Please enter a reason for not including regulatory information<br/>(eg: mouse
                                samples)<br/>
                                <stripes:text id="skipRegulatoryInfoReason" name="editOrder.skipRegulatoryReason"
                                              maxlength="255"/>
                            </div>
                            <div id="regulatorySelect" class="controls controls-text"></div>
                            <div id="attestationDiv" class="controls controls-text">

                                <stripes:checkbox name="editOrder.attestationConfirmed" id="attestationConfirmed"/>
                                ${actionBean.attestationMessage}
                            </div>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="view-control-group control-group">
                            <label class="control-label">Regulatory Information</label>

                            <div class="controls">
                                <div class="form-value">
                                    <c:choose>
                                        <c:when test="${fn:length(actionBean.editOrder.regulatoryInfos) ne 0}">
                                            <c:forEach var="regulatoryInfo" items="${actionBean.editOrder.regulatoryInfos}">
                                                ${regulatoryInfo.displayText}<br/>
                                            </c:forEach>
                                        </c:when>

                                        <c:otherwise>
                                            <c:choose><c:when test="${actionBean.editOrder.canSkipRegulatoryRequirements()}">
                                                Regulatory information not entered because: ${actionBean.editOrder.skipRegulatoryReason}
                                            </c:when>
                                                <c:otherwise>
                                                    No regulatory information entered.
                                                </c:otherwise></c:choose>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>


                <div class="control-group">
                    <stripes:label for="fundingDeadline" class="control-label">
                        Funding Deadline
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="fundingDeadline" name="editOrder.fundingDeadline" class="defaultText"
                                      title="Enter date (MM/dd/yyyy)" formatPattern="${actionBean.datePattern}"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="publicationDeadline" class="control-label">
                        Publication Deadline
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="publicationDeadline" name="editOrder.publicationDeadline" class="defaultText"
                                      title="Enter date (MM/dd/yyyy)" formatPattern="${actionBean.datePattern}"/>
                    </div>
                </div>


                <div class="control-group">
                    <stripes:label for="product" class="control-label">
                        Product <c:if test="${not actionBean.editOrder.draft}">*</c:if>
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="product" name="productTokenInput.listOfKeys" class="defaultText"
                            title="Enter the product name for this order"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="selectedAddOns" class="control-label">
                        Add-ons
                    </stripes:label>
                    <div id="addOnCheckboxes" class="controls controls-text"> </div>
                </div>

                <div id="numberOfLanesDiv" class="control-group" style="display: ${actionBean.editOrder.product.supportsNumberOfLanes ? 'block' : 'none'};">
                    <stripes:label for="numberOfLanes" class="control-label">
                        Number of Lanes Per Sample
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="numberOfLanes" name="editOrder.laneCount" class="defaultText"
                                      title="Enter Number of Lanes"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="quote" class="control-label">
                        Quote <c:if test="${not actionBean.editOrder.draft}">*</c:if>
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="quote" name="editOrder.quoteId" class="defaultText"
                                      onchange="updateFundsRemaining()"
                                      title="Enter the Quote ID for this order"/>
                        <div id="fundsRemaining"> </div>
                        <div id="skipQuoteDiv">
                            <input type="checkbox" id="skipQuote" name="skipQuote" value="${actionBean.editOrder.canSkipQuote()}" title="Click to start a PDO without a quote" />No quote required
                            <div id="skipQuoteReasonDiv">
                                Please enter a reason for skipping the quote *
                                <stripes:text id="skipQuoteReason" name="editOrder.skipQuoteReason" title="Fill in a reason for skipping the quote" maxlength="255"/>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="comments" class="control-label">
                        Description
                    </stripes:label>
                    <div class="controls">
                        <stripes:textarea id="comments" name="editOrder.comments" class="defaultText input-xlarge textarea"
                            title="Enter a description here, including any existing GAP or SQUID Initiative/Project/Experiment." cols="50" rows="3"/>
                    </div>
                </div>

                <div class="control-group">
                    <div class="control-label">&nbsp;</div>
                    <div class="controls actionButtons">
                        <stripes:submit name="save" value="${actionBean.saveButtonText}"
                                        disabled="${!actionBean.canSave}"
                                        style="margin-right: 10px;" class="btn btn-primary"/>
                        <c:choose>
                            <c:when test="${actionBean.creating}">
                                <stripes:link beanclass="${actionBean.class.name}" event="list">Cancel</stripes:link>
                            </c:when>
                            <c:otherwise>
                                <stripes:link beanclass="${actionBean.class.name}" event="view">
                                    <stripes:param name="productOrder" value="${actionBean.productOrder}"/>
                                    Cancel
                                </stripes:link>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>

            <div id="sampleListEdit" class="help-block span4">
                <c:choose>
                    <c:when test="${actionBean.editOrder.draft}">
                        Enter sample names in this box, one per line. When you save the order, the view page will show
                        all sample details.
                    </c:when>
                    <c:otherwise>
                        Sample list replacement is disabled for non-DRAFT orders.
                    </c:otherwise>
                </c:choose>
                <br/>
                <br/>
                <stripes:textarea readonly="${!actionBean.editOrder.draft}" class="controlledText" id="samplesToAdd" name="sampleList" rows="15" cols="120"/>
            </div>
            <div id="sampleInitiationKitRequestEdit" class="help-block span4" style="display: none">
            <div class="form-horizontal span5">
                <fieldset>
                    <legend>
                        <h4>
                            Sample Kit Request
                            <c:if test="${!actionBean.editOrder.draft}">
                                - <a href="${actionBean.workRequestUrl}" target="BSP">
                                        ${actionBean.editOrder.productOrderKit.workRequestId}
                                </a>
                            </c:if>
                        </h4>
                    </legend>

                    <div class="control-group">
                        <stripes:label for="kitCollection" class="control-label">
                            Group and Collection
                        </stripes:label>
                        <div class="controls" id="kitCollectionSelection">
                            <stripes:text readonly="${!actionBean.editOrder.draft}"
                                    id="kitCollection" name="bspGroupCollectionTokenInput.listOfKeys"
                                    class="defaultText"
                                    title="Search for collection and group"/>
                        </div>
                    </div>

                    <div class="control-group">
                        <stripes:label for="shippingLocation" class="control-label">
                            Shipping Location
                        </stripes:label>
                        <div class="controls" id="shippingLocationSelection">
                            <stripes:text readonly="${!actionBean.editOrder.draft}"
                                    id="shippingLocation" name="bspShippingLocationTokenInput.listOfKeys"
                                    class="defaultText"
                                    title="Search for shipping location"/>
                        </div>
                    </div>
                    <div class="control-group">
                        <stripes:label for="transferMethod" class="control-label">
                            Transfer Method
                        </stripes:label>
                        <div class="controls">
                            <c:choose>
                                <c:when test="${actionBean.editOrder.draft}">

                                    <stripes:select name="editOrder.productOrderKit.transferMethod" id="transferMethod">
                                        <stripes:options-enumeration
                                                enum="org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest.TransferMethod"
                                                label="value"/>
                                    </stripes:select>
                                </c:when>
                                <c:otherwise>
                                    <div class="form-value">${actionBean.editOrder.productOrderKit.transferMethod.value}</div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>

                    <div class="control-group">
                        <stripes:label for="isExomeExpress" class="control-label">
                            Exome Express
                        </stripes:label>
                        <div class="controls">
                            <c:choose>
                                <c:when test="${actionBean.editOrder.draft}">
                                    <stripes:checkbox name="editOrder.productOrderKit.exomeExpress" id="isExomeExpress"/>
                                    <div class="form-value">This is an Exome Express Kit</div>
                                </c:when>
                                <c:otherwise>
                                    <div class="form-value">${actionBean.editOrder.productOrderKit.exomeExpress}</div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                    <div class="control-group">
                        <stripes:label for="notificationList" class="control-label">Notification List</stripes:label>
                        <div class="controls">
                            <stripes:text readonly="${!actionBean.editOrder.draft}" id="notificationList"
                                          name="notificationListTokenInput.listOfKeys"/>
                        </div>
                    </div>
                    <div class="control-group">
                        <stripes:label for="kitComments" class="control-label">Comments</stripes:label>
                        <div class="controls">
                            <stripes:textarea style="box-sizing: border-box; width: 100%;"
                                              readonly="${!actionBean.editOrder.draft}"
                                              id="kitComments" name="editOrder.productOrderKit.comments"/>
                        </div>
                    </div>

                    <div class="control-group">
                        <div id="kitDefinitions" style="margin-top: 5px;"></div>
                    </div>
                </fieldset>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
