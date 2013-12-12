<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.entity.products.Product" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.submitString}: ${actionBean.editOrder.title}"
                       sectionTitle="${actionBean.submitString}: ${actionBean.editOrder.title}">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(
                function () {
                    $j('#productList').dataTable( {
                        "oTableTools": ttExportDefines,
                        "aaSorting": [[1,'asc']],
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
                            {"bSortable": false}]
                    });

                    $j("#owner").tokenInput(
                        "${ctxpath}/projects/project.action?usersAutocomplete=", {
                            hintText: "Type a name",
                            prePopulate: ${actionBean.ensureStringResult(actionBean.owner.completeData)},
                            tokenLimit: 1,
                            tokenDelimiter: "${actionBean.owner.separator}",
                            resultsFormatter: formatInput
                        }
                    );

                    $j("#researchProject").tokenInput(
                        "${ctxpath}/orders/order.action?projectAutocomplete=", {
                            hintText: "Type a project name",
                            prePopulate: ${actionBean.ensureStringResult(actionBean.projectTokenInput.completeData)},
                            resultsFormatter: formatInput,
                            tokenDelimiter: "${actionBean.projectTokenInput.separator}",
                            tokenLimit: 1
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
                            tokenLimit: 1
                        }
                    );
                    $j("#kitCollection").tokenInput(
                            "${ctxpath}/orders/order.action?groupCollectionAutocomplete=", {
                                hintText: "Search for group and collection",
                                prePopulate: ${actionBean.ensureStringResult(actionBean.bspGroupCollectionTokenInput.getCompleteData(!actionBean.editOrder.draft))
                            },
                                onAdd: updateUIForCollectionChoice,
                                onDelete: updateUIForCollectionChoice,
                                resultsFormatter: formatInput,
                                tokenDelimiter: "${actionBean.bspGroupCollectionTokenInput.separator}",
                                tokenLimit: 1
                            }
                    );

                    $j("#shippingLocation").tokenInput(
                            getShippingLocationURL, {
                                hintText: "Search for shipping location",
                                prePopulate: ${actionBean.ensureStringResult(actionBean.bspShippingLocationTokenInput.getCompleteData(!actionBean.editOrder.draft))},
                                resultsFormatter: formatInput,
                                tokenDelimiter: "${actionBean.bspShippingLocationTokenInput.separator}",
                                tokenLimit: 1
                            }
                    );

                    $j("#notificationList").tokenInput(
                            "${ctxpath}/orders/order.action?anyUsersAutocomplete=", {
                                hintText: "Enter a user name",
                                prePopulate: ${actionBean.ensureStringResult(actionBean.notificationListTokenInput.getCompleteData(!actionBean.editOrder.draft))},
                                tokenDelimiter: "${actionBean.notificationListTokenInput.separator}",
                                preventDuplicates: true,
                                resultsFormatter: formatInput
                            }
                    );

                    $j("#fundingDeadline").datepicker();
                    $j("#publicationDeadline").datepicker();
                    $j("#sampleListEdit").hide();

                    updateUIForProductChoice();
                    updateFundsRemaining();
                    updateUIForCollectionChoice();
                }
            );

            function formatInput(item) {
                var extraCount = (item.extraCount == undefined) ? "" : item.extraCount;
                return "<li>" + item.dropdownItem + extraCount + '</li>';
            }

            var addOn = [];
            <c:forEach items="${actionBean.addOnKeys}" var="addOnProduct">
                addOn['${addOnProduct}'] = true;
            </c:forEach>


            function updateUIForProductChoice() {

                var productKey = $j("#product").val();
                if ((productKey == null) || (productKey == "")) {
                    $j("#addOnCheckboxes").text('If you select a product, its Add-ons will show up here');
                    $j("#sampleInitiationKitRequestEdit").hide();
                } else {
                    if (productKey == '<%= Product.SAMPLE_INITIATION_PART_NUMBER %>')  {
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
                        success: setupCheckboxes
                    });

                    $j.ajax({
                        url: "${ctxpath}/orders/order.action?getSupportsNumberOfLanes=&product=" + productKey,
                        dataType: 'json',
                        success: updateNumberOfLanesVisibility
                    });
                }
            }

            function updateUIForCollectionChoice() {
                var collectionKey = $j("#kitCollection").val();
                if ((collectionKey == null) || (collectionKey == "") || (collectionKey == "Search for collection and group")) {
                    $j("#selectedOrganism").html('<div class="controls-text">Choose a collection to show related organisms</div>');

                    $j("#shippingLocationSelection").parent().append('<div class="controls" id="sitePrompt"><div class="controls-text">Choose a collection to show related shipping locations</div></div>');
                    $j("#shippingLocationSelection").hide();

                    // This is not null safe, so we must make a check to ensure the UI is not affected.
                    if ($j("#shippingLocation").val() != null) {
                        $j("#shippingLocation").tokenInput("clear");
                    }
                } else {
                    $j.ajax({
                        url: "${ctxpath}/orders/order.action?collectionOrganisms=&bspGroupCollectionTokenInput.listOfKeys=" + $j("#kitCollection").val(),
                        dataType: 'json',
                        success: setupOrganismMenu
                    });

                    $j("#sitePrompt").remove();
                    $j("#shippingLocationSelection").show();
                }
            }

            function setupOrganismMenu(data) {
                var collection = data.collectionName;

                var organisms = data.organisms;
                if ((organisms == null) || (organisms.length == 0)) {
                    $j("#selectedOrganism").text("The collection '" + collection + "' has no organisms");
                    return;
                }

                // Even though the id is a long, if there is no value then this needs something empty, so using a string
                // for the long or empty will work in the later comparison.
                var selectedOrganismId = ${actionBean.ensureStringResult(actionBean.editOrder.productOrderKit.organismId)};

                var organismSelect =
                <c:choose>
                    <c:when test="${actionBean.editOrder.draft}">
                        '<select name="editOrder.productOrderKit.organismId">';
                    </c:when>
                    <c:otherwise>
                        '<select disabled="true" name="editOrder.productOrderKit.organismId">';
                    </c:otherwise>
                </c:choose>

                $j.each(organisms, function(index, organism) {
                    var selectedString = (organism.id == selectedOrganismId) ? 'selected="selected"' : '';
                    organismSelect += '  <option value="' + organism.id + '" ' + selectedString + '>' + organism.name + '</option>';
                });
                organismSelect += '</select>';

                var duration = {'duration' : 800};
                $j("#selectedOrganism").hide();
                $j("#selectedOrganism").html(organismSelect);
                $j("#selectedOrganism").fadeIn(duration);
            }

            // This function allows the shippingLocation input token to be able to automatically pass the selected
            // collection id to filter the available shipping sites to only ones in that collection.
            function getShippingLocationURL() {
                return "${ctxpath}/orders/order.action?shippingLocationAutocomplete=&bspGroupCollectionTokenInput.listOfKeys="
                        + $j("#kitCollection").val();
            }

            function updateNumberOfLanesVisibility(data) {
                var numberOfLanesDiv = $j("#numberOfLanesDiv");

                var duration = {'duration' : 800};

                data.supportsNumberOfLanes ? numberOfLanesDiv.fadeIn(duration) : numberOfLanesDiv.fadeOut(duration);
            }


            function setupCheckboxes(data) {
                var productTitle = $j("#product").val();

                if (data.length == 0) {
                    $j("#addOnCheckboxes").text("The product '" + productTitle + "' has no Add-ons");
                    return;
                }

                var checkboxText = "";
                $j.each(data, function(index, val) {
                    // if this value is in the add on list, then check the checkbox
                    checked = '';
                    if (addOn[val.key]) {
                        checked = ' checked="checked" ';
                    }

                    var addOnId = "addOnCheckbox-" + index;
                    checkboxText += '  <input id="' + addOnId + '" type="checkbox"' + checked + ' name="addOnKeys" value="' + val.key + '"/>';
                    checkboxText += '  <label style="font-size: x-small;" for="' + addOnId + '">' + val.value +' [' + val.key + ']</label>';
                });

                var duration = {'duration' : 800};

                var checkboxes = $j("#addOnCheckboxes");
                checkboxes.hide();
                checkboxes.html(checkboxText);
                checkboxes.fadeIn(duration);
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
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="createForm">
            <div class="form-horizontal span6">
                <stripes:hidden name="productOrder"/>
                <stripes:hidden name="submitString"/>
                <div class="control-group">
                    <stripes:label for="orderName" class="control-label">
                        Name <c:if test="${actionBean.editOrder.draft}">*</c:if>
                    </stripes:label>
                    <div class="controls">
                        <stripes:text readonly="${!actionBean.editOrder.draft}" id="orderName" name="editOrder.title" class="defaultText input-xlarge"
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
                                    <stripes:hidden name="projectTokenInput.listOfKeys"
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
                    </c:otherwise></c:choose>

                <div class="control-group">
                    <stripes:label for="fundingDeadline" class="control-label">
                        Funding Deadline
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="fundingDeadline" name="editOrder.fundingDeadline" class="defaultText"
                                      title="Enter date (MM/dd/yyyy)" formatPattern="MM/dd/yyyy"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="publicationDeadline" class="control-label">
                        Publication Deadline
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="publicationDeadline" name="editOrder.publicationDeadline" class="defaultText"
                                      title="Enter date (MM/dd/yyyy)" formatPattern="MM/dd/yyyy"/>
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

                <div class="control-group">
                    <stripes:label for="quote" class="control-label">
                        Quote <c:if test="${not actionBean.editOrder.draft}">*</c:if>
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="quote" name="editOrder.quoteId" class="defaultText"
                                      onchange="updateFundsRemaining()"
                                      title="Enter the Quote ID for this order"/>
                        <div id="fundsRemaining"> </div>
                    </div>
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
                        <stripes:label for="numberOfSamples" class="control-label">
                            Number of Samples
                        </stripes:label>
                        <div class="controls">
                            <stripes:text readonly="${!actionBean.editOrder.draft}" id="numberOfSamples" name="editOrder.productOrderKit.numberOfSamples"
                                          class="defaultText" title="Enter the number of samples"/>
                        </div>
                    </div>

                    <div class="control-group">
                        <stripes:label for="kitType" class="control-label">
                            Kit Type
                        </stripes:label>
                        <div class="controls">
                            <stripes:select disabled="${!actionBean.editOrder.draft}" id="kitType" name="editOrder.productOrderKit.kitType">
                                <stripes:options-enumeration label="displayName"
                                                             enum="org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType"/>
                            </stripes:select>
                            <c:if test="${!actionBean.editOrder.draft}">
                                <stripes:hidden name="editOrder.productOrderKit.kitType"/>
                            </c:if>
                        </div>
                    </div>

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
                        <stripes:label for="selectedOrganism" class="control-label">
                            Organism
                        </stripes:label>
                        <div id="selectedOrganism" class="controls">
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
                        <stripes:label for="materialInfo" class="control-label">
                            Material Information
                        </stripes:label>
                        <div class="controls">
                            <stripes:select disabled="${!actionBean.editOrder.draft}" name="editOrder.productOrderKit.bspMaterialName">
                                <stripes:option label="Choose..." value=""/>
                                <stripes:options-collection value="bspName"
                                                            collection="${actionBean.dnaMatrixMaterialTypes}" label="bspName"/>
                            </stripes:select>
                            <c:if test="${!actionBean.editOrder.draft}">
                                <stripes:hidden name="editOrder.productOrderKit.bspMaterialName"/>
                            </c:if>
                        </div>
                    </div>
                    <div class="control-group">
                        <stripes:label for="notificationList" class="control-label">Notification List</stripes:label>
                        <div class="controls">
                            <stripes:text readonly="${!actionBean.editOrder.draft}" id="notificationList" name="notificationListTokenInput.listOfKeys"/>
                        </div>
                    </div>
                </fieldset>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
