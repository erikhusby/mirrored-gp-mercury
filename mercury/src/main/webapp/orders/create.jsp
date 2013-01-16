<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.submitString}" sectionTitle="${actionBean.submitString} ${actionBean.editOrder.title}">

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

                    $j("#researchProject").tokenInput(
                        "${ctxpath}/projects/project.action?autocomplete=", {
                            searchDelay: 2000,
                            <c:if test="${actionBean.projectCompleteData != null && actionBean.projectCompleteData != ''}">
                                prePopulate: ${actionBean.projectCompleteData},
                            </c:if>
                            tokenLimit: 1
                        }
                    );

                    $j("#product").tokenInput(
                        "${ctxpath}/products/product.action?autocomplete=", {
                            searchDelay: 2000,
                            onAdd: updateUIForProductChoice,
                            onDelete: updateUIForProductChoice,
                            <c:if test="${actionBean.productCompleteData != null && actionBean.productCompleteData != ''}">
                                prePopulate: ${actionBean.productCompleteData},
                            </c:if>
                            tokenLimit: 1
                        }
                    );

                    <c:if test="${!actionBean.creating}">
                        updateUIForProductChoice();
                        updateFundsRemaining();
                    </c:if>
                }
            );

            var addOn = [];
            <c:forEach items="${actionBean.editOrder.addOns}" var="addOnProduct">
                addOn['${addOnProduct.addOn.businessKey}'] = true;
            </c:forEach>

            function updateUIForProductChoice() {
                var productKey = $j("#product").val();
                if ((productKey == null) || (productKey == "")) {
                    $j("#addOnCheckboxes").text('If you select a product, its Add-ons will show up here');
                }

                $j.ajax({
                    url: "${ctxpath}/orders/order.action?getAddOns=&productOrder=" + productKey,
                    dataType: 'json',
                    success: setupCheckboxes
                });

                $j.ajax({
                    url: "${ctxpath}/orders/order.action?getSupportsNumberOfLanes=&productOrder=" + productKey,
                    dataType: 'json',
                    success: adjustNumberOfLanesVisibility
                });
            }


            function adjustNumberOfLanesVisibility(data) {
                var numberOfLanesDiv = $j("#numberOfLanesDiv")
                if (data["supports"]) {
                    numberOfLanesDiv.fadeIn()
                }
                else {
                    numberOfLanesDiv.fadeOut();
                }
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

                $j("#addOnCheckboxes").html(checkboxText);
            }

            function updateFundsRemaining() {
                var quoteIdentifier = $j("#quote").val();
                $j.ajax({
                    url: "${ctxpath}/orders/order.action?getQuoteFunding=&quoteIdentifier=" + quoteIdentifier,
                    dataType: 'json',
                    success: updateFunds
                });
            }

            function updateFunds(data) {
                $j("#fundsRemaining").text('Funds Remaining: ' + data.fundsRemaining);
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
                        Name *
                    </stripes:label>
                    <div class="controls">
                        <stripes:text readonly="${!actionBean.editOrder.draft}" id="orderName" name="editOrder.title" class="defaultText input-xlarge"
                            title="Enter the name of the new order"/>
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
                                    <a target="JIRA" href="${actionBean.jiraUrl}${actionBean.editOrder.jiraTicketKey}" class="external" target="JIRA">
                                            ${actionBean.editOrder.jiraTicketKey}
                                    </a>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="researchProject" class="control-label">
                        Research Project <c:if test="${not actionBean.editOrder.draft}">*</c:if>
                    </stripes:label>
                    <div class="controls">
                        <stripes:text readonly="${!actionBean.editOrder.draft}" id="researchProject" name="researchProjectList" class="defaultText"
                            title="Enter the research project for this order"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="product" class="control-label">
                        Product <c:if test="${not actionBean.editOrder.draft}">*</c:if>
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="product" name="productList" class="defaultText"
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
                                      onchange="updateFundsRemaining"
                                      title="Enter the Quote ID for this order"/>
                        <div id="fundsRemaining"> </div>
                    </div>
                </div>

                <div id="numberOfLanesDiv" class="control-group">
                    <stripes:label for="numberOfLanes" class="control-label">
                        Number of Lanes
                    </stripes:label>
                    <div class="controls">
                        <stripes:text readonly="${!actionBean.editOrder.draft}" id="numberOfLanes" name="editOrder.count" class="defaultText"
                            title="Enter Number of Lanes"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="comments" class="control-label">
                        Comments
                    </stripes:label>
                    <div class="controls">
                        <stripes:textarea readonly="${!actionBean.editOrder.draft}" id="comments" name="editOrder.comments" class="defaultText input-xlarge textarea"
                            title="Enter comments" cols="50" rows="3"/>
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
                                    <stripes:param name="productOrder" value="${actionBean.editOrder.businessKey}"/>
                                    Cancel
                                </stripes:link>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>

            <div class="help-block span4">
                <c:choose>
                    <c:when test="${actionBean.editOrder.draft}">
                        Enter samples names in this box, one per line. When you save the order, the view page will show
                        all sample details.
                    </c:when>
                    <c:otherwise>
                        This is the list of samples. Since this order has already been placed, the list of samples cannot
                        be changed.
                    </c:otherwise>
                </c:choose>
                <br/>
                <br/>
                <stripes:textarea readonly="${!actionBean.editOrder.draft}" class="controlledText" id="samplesToAdd" name="sampleList" rows="15" cols="120"/>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
