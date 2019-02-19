<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean"/>
<c:set var="addToBatchText" value="Enter if you are adding to an existing batch"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Bucket View" dataTablesVersion="1.10" withColVis="true"
                       sectionTitle="Search Bucket Entries">
    <stripes:layout-component name="extraHead">
        <style type="text/css">
            .form-horizontal label {
                font-weight: bold
            }

            .form-horizontal input[type='checkbox'] {
                float: right;
                margin-left: 6px;
            }

            ul.token-input-list {
                width: 800px;
                line-height: 0;
            }

            li.token-input-token {
                display: inline-block;
            }

            li.token-input-token span {
                margin-left: 7px;
            }

            div.token-input-clear {
                font-size: smaller;
                text-decoration: underline;
                color: #0088CC;
            }
            div.token-input-dropdown{
                width: auto !important;
                min-width: 395px;
            }

        </style>
        <script type="text/javascript">
            var tokenInputFunctions = {
                ADD_ALL: {
                    id: "Add All",
                    name: "Select all results.",
                    dropdownItem: '<div style="font-weight: bold" class="ac-dropdown-text">Select all results.</div>'
                },
                onResult: function (results) {
                    $j(this).data("data-results", results);

                    if (results.length > 0 && results[0] !== tokenInputFunctions.ADD_ALL) {
                        results.unshift(tokenInputFunctions.ADD_ALL);
                    }

                    return results;
                },
                onSend: function (request) {
                    request.data['selectedBucket'] = $j("#bucketSelect :selected").val();
                },
                onAdd: function (result) {
                    if (result === tokenInputFunctions.ADD_ALL) {
                        var results = $j(this).data("data-results");
                        $j(this).tokenInput("remove", tokenInputFunctions.ADD_ALL);
                        for (let i in results) {
                            var thisResult = results[i];
                            if (thisResult !== tokenInputFunctions.ADD_ALL) {
                                $j(this).tokenInput("add", thisResult);
                            }
                        }
                    }
                    $j(this).closest(".controls").find('.token-input-list').trigger("append");
                }
            };

            function formatInput(item) {
                var extraCount = (item.extraCount === undefined) ? "" : item.extraCount;
                return "<li>" + item.dropdownItem + extraCount + '</li>';
            }

            var $productOrdersInput = $j("#productOrders");
            var $materialTypesInput = $j("#materialTypes");

            var clearChoices = function ($clearChoices, $tokenInput) {
                $clearChoices.on('click', function () {
                    var results = $tokenInput.tokenInput('get');
                    for (let i in results) {
                        $tokenInput.tokenInput('remove', results[i]);
                    }

                    $clearChoices.hide();
                    // $j("input[name='" + $j(this).attr('name') + "']")
                });
                if ($tokenInput.val() !== "") {
                    $clearChoices.show();
                } else {
                    $clearChoices.hide();
                }
            };

            function initMaterialTypeTokenInput() {
                $materialTypesInput.tokenInput(
                    "${ctxpath}/workflow/bucketView.action?materialTypeAutoComplete=", {
                        hintText: "Type a material type",
                        <enhance:out escapeXml="false">
                        prePopulate: ${actionBean.ensureStringResult(actionBean.materialTypeTokenInput.completeData)},
                        </enhance:out>
                        resultsFormatter: formatInput,
                        tokenDelimiter: "${actionBean.materialTypeTokenInput.separator}",
                        autoSelectFirstResult: true,
                        preventDuplicates: true,
                        queryParam: "searchKey",
                        onSend: tokenInputFunctions.onSend,
                        onResult: tokenInputFunctions.onResult,
                        onAdd: tokenInputFunctions.onAdd
                    }
                );
                $materialTypesInput.parents().closest(".controls").on("append", ".token-input-list", function () {
                    clearChoices($j("#clearMaterialChoices"), $materialTypesInput)
                });
                if ($materialTypesInput.closest("div").find(".token-input-token").text() !== "") {
                    clearChoices($j("#clearMaterialChoices"), $materialTypesInput);
                }
            }

            function initPdoTokenInput() {
                $productOrdersInput.tokenInput(
                    "${ctxpath}/workflow/bucketView.action?productOrderAutoComplete=", {
                        hintText: "Type a Product Order",
                        resultsFormatter: formatInput,
                        <enhance:out escapeXml="false">
                        prePopulate: ${actionBean.ensureStringResult(actionBean.productOrderTokenInput.completeData)},
                        </enhance:out>
                        tokenDelimiter: "${actionBean.productOrderTokenInput.separator}",
                        minChars: 3,
                        searchDelay: 500,
                        autoSelectFirstResult: true,
                        preventDuplicates: true,
                        queryParam: "searchKey",
                        onSend: tokenInputFunctions.onSend,
                        onResult: tokenInputFunctions.onResult,
                        onAdd: tokenInputFunctions.onAdd
                    }
                );
                if ($productOrdersInput.closest("div").find(".token-input-token").text() !== "") {
                    clearChoices($j("#clearPdoChoices"), $productOrdersInput);
                }
                $productOrdersInput.parents().closest(".controls").on("append", ".token-input-list", function () {
                    clearChoices($j("#clearPdoChoices"), $productOrdersInput)
                });
            }

            function updateSearchFields() {
                if ($j("#bucketSelect :selected").val() !== "") {
                    $j(".search-controls").show();
                    initPdoTokenInput();
                } else {
                    $j(".search-controls").hide();
                }
            }

            $j(document).ready(function () {
                $productOrdersInput = $j("#productOrders");
                $materialTypesInput = $j("#materialTypes");

                $j("#bucketSelect").on("change", function (event) {
                    if (event.type === 'change') {
                        $productOrdersInput.tokenInput("destroy");
                        clearChoices($j("#clearPdoChoices"), $productOrdersInput);
                    }
                    updateSearchFields();
                    $j.ajax({
                        'url': "${ctxpath}/workflow/bucketView.action?saveSearchData=",
                        'data': $j("#bucketForm").serializeArray(),
                        dataType: 'json',
                        type: 'POST'
                    });
                });
                updateSearchFields();
                initMaterialTypeTokenInput();

                $j("#bucketForm").on('submit',function(){
                    var hasFiter = ($j("[name='productOrderTokenInput.listOfKeys']").val() !== "")
                        || ($j("[name='searchString']").val() !== "")
                    || ($j("[name='materialTypeTokenInput.listOfKeys']").val()!=="");
                    if (!hasFiter) {
                        var selectedBucketName = $j("#bucketSelect :selected").text();
                        if (selectedBucketName !== "") {
                            var begin = "Pico/Plating Bucket (5005 vessels)".indexOf('(') + 1;
                            var end = "Pico/Plating Bucket (5005 vessels)".indexOf(' vessels)');
                            var entryCount = selectedBucketName.slice(begin, end);
                            if (entryCount > 1000) {
                                return confirm("This bucket contains " + entryCount + " entries. Running this search could result in slow page loading and rendering. Continue?");
                            }
                        }
                    }
                });
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form style="margin-bottom: 10px" id="bucketForm" beanclass="${actionBean.class}">
            <stripes:hidden name="<%= BucketViewActionBean.SELECT_NEXT_SIZE %>"/>
            <div class="form-horizontal bucket-choice">
                <div class="control-group">
                    <stripes:label for="bucketSelect" name="Select Bucket" class="control-label"/>
                    <div class="controls">
                        <stripes:select id="bucketSelect" name="selectedBucket">
                            <stripes:option value="">Select a Bucket</stripes:option>
                            <c:forEach items="${actionBean.mapBucketToBucketEntryCount.keySet()}" var="bucketName">
                                <c:set var="bucketCount"
                                       value="${actionBean.mapBucketToBucketEntryCount.get(bucketName)}"/>
                                <stripes:option value="${bucketName}"
                                                label="${bucketName} (${bucketCount.bucketEntryCount + bucketCount.reworkEntryCount} vessels)"/>
                            </c:forEach>
                        </stripes:select>
                        <img id="spinner" src="${ctxpath}/images/spinner.gif" style="display: none;" alt=""/>
                    </div>
                </div>
            </div>
            <div class="form-horizontal search-controls" style="display: none">
                <div class="control-group">
                    <stripes:label for="productOrders" name="Product Orders" class="control-label">
                        Product Orders
                        <div id="clearPdoChoices" class="token-input-clear" style="display:none;">(Clear Selections)
                        </div>
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="productOrders" name="productOrderTokenInput.listOfKeys"
                                      class="defaultText search-input" style="width: 385px;"
                                      title="Enter the product order for this bucket"/>
                    </div>
                </div>
                <div class="control-group">
                    <div class="control-label">
                        <stripes:label for="searchString" name="Sample or Barcode" class="control-label"/>
                    </div>

                    <div class="controls">
                        <stripes:textarea id="searchString" name="searchString"
                                          class="search-input" style="width: 385px; margin: 0px; height: 167px;"/>
                    </div>

                </div>
                <div class="control-group">
                    <stripes:label for="materialTypes" name="Material Types" class="control-label">
                        Material Types
                        <div id="clearMaterialChoices" class="token-input-clear" style="display:none;">(Clear
                            Selections)
                        </div>
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="materialTypes" name="materialTypeTokenInput.listOfKeys"
                                      class="defaultText search-input" style="width: 385px;"
                                      title="Enter the material types for this bucket"/>
                    </div>
                </div>
                <div class="control-group">
                    <div class="controls">
                        <stripes:submit name="<%=BucketViewActionBean.FIND_BUCKET_ENTRIES%>"
                                        value="Find Bucket Entries"
                                        class="btn"/></div>
                </div>

            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
</div>
