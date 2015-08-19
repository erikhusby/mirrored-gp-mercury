<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.submitString}: ${actionBean.editProduct.productName}"
                       sectionTitle="${actionBean.submitString}: ${actionBean.editProduct.productName}">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

            var booleanTypes = [];
            var defaultValues = [];

            // The jsp loads the criteria types into an associative array by type and then operators
            var criteriaTypeToOperatorList = [];
            <c:forEach items="${actionBean.criteriaTypes}" var="criteriaType">
                <c:if test="${criteriaType.getDisplayed(actionBean.editProduct)}">
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

            var criteriaCount = 0;

            $j(document).ready(
                function () {
                    $j("#primaryPriceItem").tokenInput(
                        "${ctxpath}/products/product.action?priceItemAutocomplete=&product=${actionBean.editProduct.businessKey}", {
                            hintText: "Type a Price Item name",
                            prePopulate: ${actionBean.ensureStringResult(actionBean.priceItemTokenInput.completeData)},
                            resultsFormatter: formatInput,
                            tokenLimit: 1,
                            tokenDelimiter: "${actionBean.priceItemTokenInput.separator}",
                            preventDuplicates: true,
                            autoSelectFirstResult: true
                        }
                    );

                    $j("#addOns").tokenInput(
                        "${ctxpath}/products/product.action?addOnsAutocomplete=&product=${actionBean.editProduct.businessKey}", {
                            hintText: "Type a Product name",
                            prePopulate: ${actionBean.ensureStringResult(actionBean.addOnTokenInput.completeData)},
                            resultsFormatter: formatInput,
                            tokenDelimiter: "${actionBean.addOnTokenInput.separator}",
                            preventDuplicates: true,
                            autoSelectFirstResult: true
                        }
                    );

                    $j("#availabilityDate").datepicker();
                    $j("#discontinuedDate").datepicker();

                    updateBillingRules();

                    <c:forEach items="${actionBean.editProduct.riskCriteria}" var="criterion">
                        addCriterion('${criterion.type.label}', '${criterion.operator.label}', '${criterion.value}');
                    </c:forEach>
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
                newCriteria += '    <select id="criteriaSelect-' + criteriaCount + '" onchange="updateOperatorOptions(' + criteriaCount + ')" style="width:auto;" name="criteria">';

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

                newCriteria += '</div>\n';

                $j('#riskCriterion').append(newCriteria);

                updateValueView(criteria, criteriaCount);

                criteriaCount++;
            }

            function updateOperatorOptions(criteriaCount) {
                var criteriaLabel = $j('#criteriaSelect-' + criteriaCount + " option:selected").text();

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

        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="createForm" class="form-horizontal">
            <stripes:hidden name="submitString"/>

            <div class="row">
                <div class="form-horizontal span7" >
                <stripes:hidden name="product"/>
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
                        Product Name *
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="productName" name="editProduct.productName" class="defaultText input-xxlarge"
                            title="Enter the name of the new product"/>
                    </div>
                </div>

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
                            title="Enter the part number of the new product"/>
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
                        <stripes:label for="workflow" class="control-label">
                            Workflow
                        </stripes:label>
                        <div class="controls">
                            <stripes:select name="editProduct.workflow" id="workflow">
                                <stripes:option value="${actionBean.workflowNone}">None</stripes:option>
                                <stripes:options-collection collection="${actionBean.visibleWorkflowList}" label="workflowName"/>
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
                            <stripes:label for="reagentDesignKey" class="control-label"><abbr title="aka Reagent Design">Bait Design</abbr></stripes:label>
                            <div class="controls">
                                <stripes:select id="reagentDesignKey" name="editProduct.reagentDesignKey">
                                    <stripes:option value="">Select One</stripes:option>
                                    <stripes:options-collection collection="${actionBean.reagentDesigns}" label="displayName" value="businessKey"/>
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
