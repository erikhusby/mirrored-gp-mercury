<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.entity.products.Product" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.submitString}: ${actionBean.editProduct.productName}"
                       sectionTitle="${actionBean.submitString}: ${actionBean.editProduct.productName}">

    <stripes:layout-component name="extraHead">

        <script type="text/javascript">

            var booleanTypes = [];
            var defaultValues = [];
            var suggestedValues = [];

            // The jsp loads the criteria types into an associative array by type and then operators
            var criteriaTypeToOperatorList = [];
            <c:forEach items="${actionBean.criteriaTypes}" var="criteriaType">
                <c:if test="${criteriaType.getDisplayed(actionBean.editProduct)}">
                    <c:if test="${not empty criteriaType.suggestedValues}">
                        suggestedValues["${criteriaType.label}"] = [];
                            suggestedValues["${criteriaType.label}"] = "Yes";
                    </c:if>
                    <c:choose>
                        <c:when test="${criteriaType.operators[0].type == 'BOOLEAN'}">
                            defaultValues['${criteriaType.label}'] = 'true';
                        </c:when>
                        <c:otherwise>
                            defaultValues['${criteriaType.label}'] = '';
                        </c:otherwise>
                    </c:choose>
                    criteriaTypeToOperatorList['${criteriaType.label}'] = [];
                    <c:forEach items="${criteriaType.operators}" var="operator" varStatus="j">
                        criteriaTypeToOperatorList['${criteriaType.label}'][${j.index}] = '${operator.label}';

                        <c:if test="${operator.type == 'BOOLEAN'}">
                            booleanTypes['${criteriaType.label}'] = true;
                        </c:if>
                    </c:forEach>
                </c:if>
            </c:forEach>

            // Builds a map of chipTechnology to array of chipNames.
            var genotypingTechAndChipNames = {
                <c:forEach items="${actionBean.availableChipTechnologyAndChipNames.keySet()}" var="keyItem" varStatus="keyItemStatus">
                    '${keyItem}': [
                        <c:forEach items="${actionBean.availableChipTechnologyAndChipNames[keyItem]}" var="item" varStatus="itemStatus">
                            '${item}'
                            <c:if test="${!itemStatus.last}">,</c:if>
                        </c:forEach>
                    ]
                    <c:if test="${!keyItemStatus.last}">,</c:if>
                </c:forEach>
            };

            var criteriaCount = 0;
            var genotypingChipCount = 0;

            $j(document).ready(
                function () {
                    $j("#primaryPriceItem").tokenInput(
                        "${ctxpath}/products/product.action?priceItemAutocomplete=&product=${actionBean.editProduct.businessKey}", {
                            hintText: "Type a Price Item name",
                            <enhance:out escapeXml="false">
                            prePopulate: ${actionBean.ensureStringResult(actionBean.priceItemTokenInput.completeData)},
                            </enhance:out>
                            resultsFormatter: formatInput,
                            tokenLimit: 1,
                            tokenDelimiter: "${actionBean.priceItemTokenInput.separator}",
                            preventDuplicates: true,
                            autoSelectFirstResult: true
                        }
                    );

                    $j("#externalPriceItem").tokenInput(
                        "${ctxpath}/products/product.action?externalPriceItemAutocomplete=&product=${actionBean.editProduct.businessKey}", {
                            hintText: "Type an External Price Item name",
                            <enhance:out escapeXml="false">
                            prePopulate: ${actionBean.ensureStringResult(actionBean.externalPriceItemTokenInput.completeData)},
                            </enhance:out>
                            resultsFormatter: formatInput,
                            tokenLimit: 1,
                            tokenDelimiter: "${actionBean.externalPriceItemTokenInput.separator}",
                            preventDuplicates: true,
                            autoSelectFirstResult: true
                        }
                    );

                    $j("#addOns").tokenInput(
                        "${ctxpath}/products/product.action?addOnsAutocomplete=&product=${actionBean.editProduct.businessKey}", {
                            hintText: "Type a Product name",
                            <enhance:out escapeXml="false">
                            prePopulate: ${actionBean.ensureStringResult(actionBean.addOnTokenInput.completeData)},
                            </enhance:out>
                            resultsFormatter: formatInput,
                            tokenDelimiter: "${actionBean.addOnTokenInput.separator}",
                            preventDuplicates: true,
                            autoSelectFirstResult: true
                        }
                    );

                    $j("#suggestedValuesDialog").dialog({
                        modal: true,
                        autoOpen: false,
                        buttons: [
                            {
                                id: "chooseSuggestion",
                                text: "Assign the Chosen Suggestion(s)",
                                click: function() {
                                    var selectedValues = [];
                                    $j("#suggestedValueList").find(":selected").each(function() {
                                        selectedValues.push($(this).val());
                                    });
                                    var index = $j("#criteriaSuggestionIndex").val();
                                    $j("#valueText-" + index ).val(selectedValues.join(', '));
                                    $j(this).dialog("close");
                                }
                            },
                            {
                                id: "cancel",
                                text: "Cancel",
                                click: function () {
                                    $j(this).dialog("close");
                                }
                            }
                        ]
                    });

                    $j("#availabilityDate").datepicker();
                    $j("#discontinuedDate").datepicker();

                    updateBillingRules();

                    <c:forEach items="${actionBean.editProduct.riskCriteria}" var="criterion">
                        addCriterion('${criterion.type.label}', '${criterion.operator.label}', '${criterion.value}');
                    </c:forEach>

                    <c:forEach items="${actionBean.genotypingChipInfo}" var="iterator" varStatus="iteratorStatus">
                        addGenotypingChip('${iterator.left}', '${iterator.middle}', '${iterator.right}');
                    </c:forEach>

                    $j("#reagentDesignKey").prop('disabled', ${not actionBean.editProduct.baitLocked});
                    $j('#baitLocked').change(function() {
                        var locked = ($j(this).val() === 'true');
                        $j("#reagentDesignKey").prop( "disabled", !locked);
                    });
                    $j("#createForm").submit(function() {
                        // We want to keep the old value if available and not set to null, so re-enable before submit.
                        $j("#reagentDesignKey").prop('disabled', false);
                        return true;
                    });
                }
            );

            function formatInput(item) {
                var extraCount = (item.extraCount == undefined) ? "" : item.extraCount;
                return "<li>" + item.dropdownItem + extraCount + '</li>';
            }

            function updateBillingRules() {
                if ($j('#useAutomatedBilling').prop('checked')) {
                    $j('#billingRules').show();
                } else {
                    $j('#billingRules').hide();
                }
            }

            function removeCriterion(itemNumber) {
                $j('#criterion-' + itemNumber).remove();
            }

            function addCriterion(criteria, operator, value) {
                var newCriteria = '<div id="criterion-' + criteriaCount + '" style="margin-bottom:3px;" class="criterionPanel">\n';

                if (criteria == undefined) {
                    criteria = 'Concentration'
                }

                if (value == undefined) {
                    value = defaultValues[criteria];
                }

                // remove button for this item
                newCriteria += '    <a class="btn btn-mini" style="font-size:14pt;text-decoration: none;" onclick="removeCriterion(' + criteriaCount + ')">-</a>\n';


                // the criteria list
                newCriteria += '    <select id="criteriaSelect-' + criteriaCount + '" onchange="criteriaSelectChange(' + criteriaCount + ')" style="width:auto;" name="criteria">';

                var operatorsLabel;

                for (var criteriaLabel in criteriaTypeToOperatorList) {

                    var selectedString = '';
                    if (criteriaLabel == criteria) {
                        selectedString = 'selected="selected"';
                        operatorsLabel = criteriaLabel;
                    }

                    newCriteria += '        <option value="' + criteriaLabel + '" ' + selectedString + '>' + criteriaLabel + '</option>\n';
                }

                newCriteria += '    </select>\n';

                // the operator for the selected item
                newCriteria += '    <select style="display:none" id="operatorSelect-' + criteriaCount + '" style="padding-left:4px;padding-right:4px;width:auto;" name="operators">\n';
                newCriteria += operatorOptions(criteriaCount, operatorsLabel, operator);
                newCriteria += '    </select>\n';

                newCriteria += '    <input style="display:none" id="valueText-' + criteriaCount + '" type="text" name="values" value="' + value + '"/>\n';

                if(criteria in suggestedValues) {
                    newCriteria += '    <div id="suggestionClick-' + criteriaCount + '">';
                } else {
                    newCriteria += '    <div id="suggestionClick-' + criteriaCount + '" style=display:none >';
                }
                newCriteria += '    <a onclick="viewSuggestionPopup('+criteriaCount+',\''+criteriaLabel+'\')" id="suggestionLink-' + criteriaCount + '">Click here</a> for suggested values';
                newCriteria += '    </div>';

                newCriteria += '</div>\n';

                $j('#riskCriterion').append(newCriteria);

                updateValueView(criteria, criteriaCount);

                criteriaCount++;
            }

            function criteriaSelectChange(indexedCriteria) {
                updateOperatorOptions(indexedCriteria);
                toggleSuggestionClick(indexedCriteria);
            }

            function toggleSuggestionClick(indexedCriteria) {
                var selectedCriterion = $('#criteriaSelect-'+indexedCriteria).find(":selected").text();

                if(selectedCriterion in suggestedValues) {
                    $j("#suggestionClick-" + indexedCriteria).show();
                } else {
                    $j("#suggestionClick-" + indexedCriteria).hide();
                }
            }

            function viewSuggestionPopup(criteriaIndex, criteriaLabel) {

                $j("#criteriaSuggestionIndex").val(criteriaIndex);
                $j("#suggestedValuesDialog").html('');

                var criteriaOp = $j("#operatorSelect-"+criteriaIndex+" option:selected").val();
                var currentCriteriaChoices = $j("#valueText-"+criteriaIndex).val();
                $j.ajax({
                    url: "${ctxpath}/products/product.action?openRiskSuggestedValues=",
                    data: {
                        'criteriaIndex': criteriaIndex,
                        'criteriaLabel': criteriaLabel,
                        'criteriaOp': criteriaOp,
                        'currentCriteriaChoices': currentCriteriaChoices
                    },
                    datatype: 'html',
                    success: function (html) {
                        $j("#suggestedValuesDialog").html(html).dialog("open");
                    }
                });
                return false;
            }

            function updateOperatorOptions(criteriaCount) {
                var criteriaLabel = $j('#criteriaSelect-' + criteriaCount + ' option:selected').text();

                $j('#operatorSelect-' + criteriaCount).html(operatorOptions(criteriaCount, criteriaLabel, criteriaLabel));

                // Set the value text
                $j('#valueText-' + criteriaCount).attr("value", defaultValues[criteriaLabel]);

                updateValueView(criteriaLabel, criteriaCount);
            }

            function updateValueView(criteriaLabel, criteriaCount) {
                if (booleanTypes[criteriaLabel]) {
                    $j('#operatorSelect-' + criteriaCount).hide();
                    $j('#valueText-' + criteriaCount).hide();
                } else {
                    $j('#operatorSelect-' + criteriaCount).show();
                    $j('#valueText-' + criteriaCount).show();
                }
            }

            function operatorOptions(criteriaCount, criteriaLabel, selectedOperator) {
                var options = '';

                var operators = criteriaTypeToOperatorList[criteriaLabel];
                for (var i= 0, max = operators.length; i < max; i++) {
                    var currentOperator = operators[i];

                    var selectedString = '';
                    if (currentOperator == selectedOperator) {
                        selectedString = 'selected="selected"';
                    }

                    options += '        <option value="' + currentOperator + '" ' + selectedString + '>' + currentOperator + '</option>\n';
                }

                return options;
            }

            function removeGenotypingChip(itemNumber) {
                $j('#genotypingChip-' + itemNumber).remove();
            }

            function updateGenotypingTechnology(itemNumber) {
                var selectedTechnology = $j('#chipTechnology-' + itemNumber).val();
                var selectedChipName = $j('#chipName-' + itemNumber).val();

                // Replaces the options in the corresponding chip name dropdown select.
                $j('#chipName-' + itemNumber).empty();

                for (var optionName of genotypingTechAndChipNames[selectedTechnology]) {
                    var option = $('<option></option>').attr("value", optionName).text(optionName);
                    if (optionName == selectedChipName) {
                        option.attr("selected", "selected");
                    }
                    $j('#chipName-' + itemNumber).append(option);
                }
            }

            function updateGenotypingPdoSubstring(itemNumber) {
                // Prefixes the pdoSubstring passed to the action bean with "item" delimited by a space.
                var nakedPdoSubstring = $j('#visiblePdoSubstring-' + itemNumber).val();
                $j('#pdoSubstring-' + itemNumber).attr("value", "item " + nakedPdoSubstring);
            }

            function addGenotypingChip(chipTechnology, chipName, pdoSubstring) {
                var newGenotypingChip = '<div id="genotypingChip-' + genotypingChipCount +
                        '" style="margin-bottom:3px;" class="genotypingChipPanel">\n';

                // Adds a button to remove this item
                newGenotypingChip += '    <a class="btn btn-mini" style="font-size:14pt;text-decoration: none;"' +
                        ' onclick="removeGenotypingChip(' + genotypingChipCount + ')">-</a>\n';

                // Populates the chip technology dropdown
                newGenotypingChip += '    <select id="chipTechnology-' + genotypingChipCount + '" ' +
                        '" onchange="updateGenotypingTechnology(' + genotypingChipCount + ')" ' +
                        'style="width:auto;" name="genotypingChipTechnologies" value="' + chipTechnology + '">\n';
                for (var optionName of Object.keys(genotypingTechAndChipNames)) {
                    var selectedString = '';
                    if (optionName == chipTechnology) {
                        selectedString = ' selected="selected"';
                    }
                    newGenotypingChip += '      <option' + selectedString + '>' + optionName + '</option>';
                }
                newGenotypingChip += '    </select>\n';

                // Populates the chip name dropdown
                newGenotypingChip += '    <select id="chipName-' + genotypingChipCount +
                        '" style="width:auto;" name="genotypingChipNames" value="' + chipName + '">\n';
                if (genotypingTechAndChipNames[chipTechnology]) {
                    for (var optionName of genotypingTechAndChipNames[chipTechnology]) {
                        var selectedString = '';
                        if (optionName == chipName) {
                            selectedString = ' selected="selected"';
                        }
                        newGenotypingChip += '      <option' + selectedString + '>' + optionName + '</option>';
                    }
                }
                newGenotypingChip += '    </select>\n';

                // The text box for pdo name restriction
                newGenotypingChip += '    <br/>Restrict to Product Orders whose names contain:';
                newGenotypingChip += '    <br/><input style="width:80%" id="visiblePdoSubstring-' + genotypingChipCount + '" type="text" ' +
                        'title="Enter the distinctive portion of a Product Order name. This chip will only apply when Product Order name has the distinctive portion. ' +
                        'Leave blank when this genotyping chip should apply to any Product Order when name is not otherwise matched." ' +
                        '" onchange="updateGenotypingPdoSubstring(' + genotypingChipCount + ')" name="visiblePdoSubstrings" value="' + pdoSubstring + '"/>\n';

                <!-- Uses hidden fields to set the arrays in the action bean since null strings don't get sent in the array and then the array indexes can't line up. -->
                newGenotypingChip += '    <input style="display:none" id="pdoSubstring-' + genotypingChipCount + '" type="text" name="genotypingChipPdoSubstrings"' +
                        ' value="item' + genotypingChipCount + ' ' + pdoSubstring + '"/>\n';
                newGenotypingChip += '</div>\n';

                $j('#genotypingChips').append(newGenotypingChip);

                genotypingChipCount++;
            }

        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <div id="suggestedValuesDialog"  title="Select from suggested values for risk criteria"  style="...">

        </div>

        <stripes:form beanclass="${actionBean.class.name}" id="createForm" class="form-horizontal">
            <stripes:hidden name="submitString"/>

            <div class="row">
                <div class="form-horizontal span7" >
                <stripes:hidden name="product"/>
                    <stripes:hidden name="criteriaSuggestionIndex" id="criteriaSuggestionIndex" />

                    <security:authorizeBlock roles="<%= roles(PDM, Developer) %>">
                        <div class="control-group">
                            <stripes:label for="externalOrderOnly" class="control-label">
                                Only offered as Commercial Product
                            </stripes:label>
                            <div class="controls">
                                <stripes:checkbox id="externalOrderOnly" name="editProduct.externalOnlyProduct" style="margin-top: 10px;" disabled="${actionBean.productUsedInOrders}"/>
                                <c:if test="${actionBean.productUsedInOrders}">
                                    <stripes:hidden name="editProduct.externalOnlyProduct" value="${actionBean.editProduct.externalOnlyProduct}" />
                                </c:if>
                            </div>
                        </div>
                        <div class="control-group">
                            <stripes:label for="clinicalProduct" class="control-label">
                                Clinical Product
                            </stripes:label>
                            <div class="controls">
                                <stripes:checkbox id="clinicalProduct" name="editProduct.clinicalProduct" style="margin-top: 10px;" disabled="${actionBean.productUsedInOrders}"/>
                                <c:if test="${actionBean.productUsedInOrders}">
                                    <stripes:hidden name="editProduct.clinicalProduct" value="${actionBean.editProduct.clinicalProduct}" />
                                </c:if>
                            </div>
                        </div>
                    </security:authorizeBlock>

                    <div class="control-group">
                    <stripes:label for="productFamily" class="control-label">
                        Product Family *
                    </stripes:label>
                    <div class="controls">
                        <stripes:select name="productFamilyId" id="productFamily">
                            <stripes:option value="">Select a Product Family</stripes:option>
                            <stripes:options-collection collection="${actionBean.productFamilies}" label="name"
                                                        value="productFamilyId"/>
                        </stripes:select>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="productName" class="control-label">
                        Primary Product Name *
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="productName" name="editProduct.productName" class="defaultText input-xxlarge"
                            title="Enter the name of the new product"/>
                    </div>
                </div>

                        <%--Saving this implementation for the final 2.0 SAP/GP release of Mercury--%>
                <%--<div class="control-group">--%>
                    <%--<stripes:label for="alternateExternalName" class="control-label">--%>
                        <%--Alternate (External) Product Name--%>
                    <%--</stripes:label>--%>
                    <%--<div class="controls">--%>
                        <%--<stripes:text id="alternateExternalName" name="editProduct.alternateExternalName" class="defaultText input-xxlarge"--%>
                            <%--title="Enter the Commercial/Clinical (External) name of the new product"/>--%>
                    <%--</div>--%>
                <%--</div>--%>

                <div class="control-group">
                    <stripes:label for="description" class="control-label">
                        Product Description *
                    </stripes:label>
                    <div class="controls">
                        <stripes:textarea id="description" name="editProduct.description" class="defaultText input-xxlarge textarea"
                            title="Enter the description of the new product" cols="50" rows="3"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="partNumber" class="control-label">
                        Part Number *
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="partNumber" name="editProduct.partNumber" class="defaultText input-xxlarge"
                            title="Enter the part number of the new product"
                                      readonly="${actionBean.productInSAP(actionBean.editProduct.partNumber,
                                                                          actionBean.editProduct.determineCompanyConfiguration())}"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="availabilityDate" class="control-label">
                        Availability Date *
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="availabilityDate" name="editProduct.availabilityDate" class="defaultText"
                            title="Enter date (MM/dd/yyyy)" formatPattern="MM/dd/yyyy" />
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="discontinuedDate" class="control-label">
                        Discontinued Date
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="discontinuedDate" name="editProduct.discontinuedDate" class="defaultText" title="Enter date (MM/dd/yyyy)"
                                      formatPattern="MM/dd/yyyy" />

                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="deliverables" class="control-label">
                        Deliverables
                    </stripes:label>
                    <div class="controls">
                        <stripes:textarea id="deliverables" name="editProduct.deliverables" class="defaultText input-xxlarge textarea"
                            title="Enter the deliverables for this product" cols="50" rows="3"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="inputRequirements" class="control-label">
                        Input Requirements
                    </stripes:label>
                    <div class="controls">
                        <stripes:textarea id="inputRequirements" name="editProduct.inputRequirements" class="defaultText input-xxlarge textarea"
                            title="Enter the input requirements for this product" cols="50" rows="3"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="expectedCycleTimeDays" class="control-label">
                        Expected Cycle Time (Days)
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="expectedCycleTimeDays" name="editProduct.expectedCycleTimeDays"
                            class="defaultText" title="Enter the expected cycle time in days"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="guaranteedCycleTimeDays" class="control-label">
                        Guaranteed Cycle Time (Days)
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="guaranteedCycleTimeDays" name="editProduct.guaranteedCycleTimeDays"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="samplesPerWeek" class="control-label">
                        Samples Per Week
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="samplesPerWeek" name="editProduct.samplesPerWeek"
                            class="defaultText" title="Enter the number of samples"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="minimumOrderSize" class="control-label">
                        Minimum Order Size
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="minimumOrderSize" name="editProduct.minimumOrderSize"
                            class="defaultText" title="Enter the minimum order size"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="pdmOrderableOnly" class="control-label">
                        PDM Orderable Only
                    </stripes:label>
                    <div class="controls">
                        <stripes:checkbox id="pdmOrderableOnly" name="editProduct.pdmOrderableOnly" class="defaultText" style="margin-top: 10px;"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="primaryPriceItem" class="control-label">
                        Primary Price Item *
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="primaryPriceItem" name="priceItemTokenInput.listOfKeys"
                            class="defaultText" title="Type to search for matching price items"/>
                    </div>
                </div>

                        <%--Saving this implementation for the final 2.0 SAP/GP release of Mercury--%>
                    <%--<div class="control-group">--%>
                        <%--<stripes:label for="externalPriceItem" class="control-label">--%>
                            <%--Alternate (External) Price Item--%>
                        <%--</stripes:label>--%>
                        <%--<div class="controls">--%>
                            <%--<stripes:text id="externalPriceItem" name="externalPriceItemTokenInput.listOfKeys"--%>
                                          <%--class="defaultText" title="Type to search for matching price items"/>--%>
                        <%--</div>--%>
                    <%--</div>--%>


                    <div class="control-group">
                    <stripes:label for="addOns" class="control-label">
                        Add-ons
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="addOns" name="addOnTokenInput.listOfKeys"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="riskCriterion" name="RiskCriteria" class="control-label"/>
                    <div id="riskCriterion" class="controls" style="margin-top: 5px;">
                        A sample is on risk if:
                        <a id="addRiskCriteria" class="btn btn-mini" style="margin-bottom: 3px;text-decoration: none;" onclick="addCriterion()">+</a>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="genotypingChips" name="GenotypingChip" class="control-label"/>
                    <div id="genotypingChips" class="controls" style="margin-top: 5px;">
                        <a id="addGenotypingChip" class="btn btn-mini" style="margin-bottom: 3px;text-decoration: none;"
                           onclick="addGenotypingChip(Object.keys(genotypingTechAndChipNames)[0], '', '')">+</a>
                    </div>
                </div>

                <security:authorizeBlock roles="<%= roles(Developer, PDM)%>">
                    <div class="control-group">
                        <stripes:label for="useAutomatedBilling" class="control-label">
                            Billing
                        </stripes:label>
                        <div class="controls">
                            <stripes:checkbox id="useAutomatedBilling" name="editProduct.useAutomatedBilling" onchange="updateBillingRules()" style="margin-top: 10px;"/>
                            <stripes:label for="useAutomatedBilling" class="control-label" style="width:auto;">
                                Automated
                            </stripes:label>
                        </div>

                        <div id="billingRules" style="clear:both;" class="controls">
                            <stripes:label for="requirementsAttribute" class="control-label" style="width: auto; margin-right:5px;">
                                Bill When
                            </stripes:label>

                            <stripes:text id="requirementsAttribute" name="editProduct.requirement.attribute"
                                          class="defaultText" title="Attribute to compare"/>
                            &#160;

                            <stripes:select style="width:50px;" name="editProduct.requirement.operator">
                                <stripes:options-collection collection="${actionBean.getRequirementOperators}" label="label"/>
                            </stripes:select>
                            &#160;

                            <stripes:text id="requirementsValue" name="editProduct.requirement.value"
                                          class="defaultText" title="Value to compare"/>
                        </div>
                    </div>
                </security:authorizeBlock>

                <security:authorizeBlock roles="<%= roles(PDM, Developer) %>">
                    <div class="control-group">
                        <stripes:label for="expectInitialQuantInMercury" class="control-label">
                            Expect Initial Quant in Mercury
                        </stripes:label>
                        <div class="controls">
                            <stripes:checkbox id="expectInitialQuantInMercury" name="editProduct.expectInitialQuantInMercury" style="margin-top: 10px;"/>
                        </div>
                    </div>
                </security:authorizeBlock>

                <security:authorizeBlock roles="<%= roles(PDM, Developer) %>">
                    <div class="control-group">
                        <stripes:label for="workflow" class="control-label">
                            Workflow
                        </stripes:label>
                        <div class="controls">
                            <stripes:select name="editProduct.workflowName" id="workflow">
                                <stripes:option value="">None</stripes:option>
                                <stripes:options-collection collection="${actionBean.availableWorkflows}" />
                            </stripes:select>
                        </div>
                    </div>
                </security:authorizeBlock>

            </div>

                <div class="form-horizontal span5">
                    <fieldset>
                        <legend><h4>Pipeline Analysis</h4></legend>

                        <div class="control-group">
                            <stripes:label for="aggregationDataType" name="AggregationDataType" class="control-label"/>
                            <div class="controls">
                                <stripes:text id="aggregationDataType" name="editProduct.aggregationDataType"
                                              class="defaultText" title="Enter data type to use for aggregation"/>
                            </div>
                        </div>

                        <security:authorizeBlock roles="<%= roles(Developer, PDM) %>">
                            <div class="control-group">
                                <stripes:label for="aggregationParticle" name="customAggregationParticle"
                                               class="control-label"/>
                                <div class="controls">
                                    <stripes:select style="width: auto;" id="customAggregationParticle"
                                                    name="editProduct.defaultAggregationParticle"
                                                    title="Select the custom aggregation particle which the pipleine will appended to their default aggregation. By default the pipeline aggregates on the research project.">
                                        <stripes:option value=""><%=Product.AggregationParticle.DEFAULT_LABEL%></stripes:option>
                                        <stripes:options-enumeration label="displayName"
                                                                     enum="org.broadinstitute.gpinformatics.athena.entity.products.Product.AggregationParticle"/>
                                    </stripes:select>
                                </div>
                            </div>
                        </security:authorizeBlock>

                        <div class="control-group">
                            <stripes:label for="analysisTypeKey" name="Analysis Type" class="control-label"/>
                            <div class="controls">
                                <stripes:select id="analysisTypeKey" name="editProduct.analysisTypeKey">
                                    <stripes:option value="">Select One</stripes:option>
                                    <stripes:options-collection collection="${actionBean.analysisTypes}" label="displayName" value="businessKey"/>
                                </stripes:select>
                            </div>
                        </div>

                        <div class="control-group">
                            <stripes:label for="baitLocked" class="control-label">
                                Bait Locked
                            </stripes:label>
                            <div class="controls">
                                <stripes:select id="baitLocked" name="editProduct.baitLocked">
                                    <stripes:option value="true">True</stripes:option>
                                    <stripes:option value="false">False</stripes:option>
                                </stripes:select>
                            </div>
                        </div>
                        <div class="control-group" id="reagentDesignGroup">
                            <stripes:label for="reagentDesignKey" class="control-label"><abbr title="aka Reagent Design">Bait Design</abbr></stripes:label>
                            <div class="controls">
                                <stripes:select id="reagentDesignKey" name="editProduct.reagentDesignKey">
                                    <stripes:option value="">Select One</stripes:option>
                                    <stripes:options-collection collection="${actionBean.reagentDesigns}" label="displayName" value="businessKey"/>
                                </stripes:select>
                            </div>
                        </div>

                        <div class="control-group">
                            <stripes:label for="controlsProject" name="Positive Controls Project" class="control-label"/>
                            <div class="controls">
                                <stripes:select id="controlsProject" name="controlsProject">
                                    <stripes:option value="">Select One</stripes:option>
                                    <stripes:options-collection collection="${actionBean.controlsProjects}" label="displayName" value="businessKey"/>
                                </stripes:select>
                            </div>
                        </div>

                        <div class="control-group">
                            <stripes:label for="negativeControlsProject" name="Negative Controls Project" class="control-label"/>
                            <div class="controls">
                                <stripes:select id="negativeControlsProject" name="negativeControlsProject">
                                    <stripes:option value="">Select One</stripes:option>
                                    <stripes:options-collection collection="${actionBean.controlsProjects}" label="displayName" value="businessKey"/>
                                </stripes:select>
                            </div>
                        </div>

                        <div class="control-group">
                            <stripes:label for="readLength" name="ReadLength" class="control-label"/>
                            <div class="controls">
                                <stripes:text id="readLength" name="editProduct.readLength"
                                        class="defaultText" title="Enter length of template read"/>
                            </div>
                        </div>

                        <div class="control-group">
                            <stripes:label for="insertSize" name="InsertSize" class="control-label"/>
                            <div class="controls">
                                <stripes:text id="insertSize" name="editProduct.insertSize"
                                        class="defaultText" title="Enter size of insert"/>
                            </div>
                        </div>

                        <div class="control-group">
                            <stripes:label for="loadingConcentration" name="LoadingConcentration" class="control-label"/>
                            <div class="controls">
                                <stripes:text id="loadingConcentration" name="editProduct.loadingConcentration"
                                        class="defaultText" title="Sets the default loading concentration."/>
                            </div>
                        </div>

                        <div class="control-group">
                            <stripes:label for="pairedEndRead" class="control-label">
                                Paired End Read
                            </stripes:label>
                            <div class="controls">
                                <stripes:checkbox id="pairedEndRead" name="editProduct.pairedEndRead" style="margin-top: 10px;"/>
                            </div>
                        </div>

                        <div class="control-group">
                            <stripes:label for="indexType" class="control-label">Index Type</stripes:label>
                            <div class="controls">
                                <stripes:select name="editProduct.indexType">
                                    <stripes:options-enumeration
                                            enum="org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation.IndexType"
                                            label="displayName"/>
                                </stripes:select>
                            </div>
                        </div>

                        <div class="control-group">
                            <stripes:label for="analyzeUmi" class="control-label">
                                Analyze UMIs
                            </stripes:label>
                            <div class="controls">
                                <stripes:checkbox id="analyzeUmi" name="editProduct.analyzeUmi" style="margin-top: 10px;"/>
                            </div>
                        </div>

                        <div class="control-group">
                            <stripes:label for="coverageTypeKey" name="Coverage" class="control-label"/>
                            <div class="controls">
                                <stripes:select id="coverageTypeKey" name="editProduct.coverageTypeKey">
                                    <stripes:option value="">Select One</stripes:option>
                                    <stripes:options-collection collection="${actionBean.coverageTypes}" label="displayName" value="businessKey"/>
                                </stripes:select>
                            </div>
                        </div>
                    </fieldset>
                </div>
            </div>

            <div class="row">
                <div class="form-horizontal span12">
                    <div class="control-group">
                        <div class="controls">
                            <div class="row-fluid">
                                <div class="span1">
                                    <stripes:submit name="save" value="Save" class="btn btn-primary"/>
                                </div>
                                <div class="span1">
                                    <c:choose>
                                        <c:when test="${actionBean.creating}">
                                            <stripes:link beanclass="${actionBean.class.name}" event="list">Cancel</stripes:link>
                                        </c:when>
                                        <c:otherwise>
                                            <stripes:link beanclass="${actionBean.class.name}" event="view">
                                                <stripes:param name="product" value="${actionBean.product}"/>
                                                Cancel
                                            </stripes:link>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
