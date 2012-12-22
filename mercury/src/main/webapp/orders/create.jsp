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
                            minChars: 2,
                            <c:if test="${actionBean.projectCompleteData != null}">
                                prePopulate: ${actionBean.projectCompleteData},
                            </c:if>
                            tokenLimit: 1
                        }
                    );

                    $j("#product").tokenInput(
                        "${ctxpath}/products/product.action?autocomplete=", {
                            searchDelay: 2000,
                            minChars: 2,
                            onAdd: updateAddOnCheckboxes,
                            onDelete: updateAddOnCheckboxes,
                            <c:if test="${actionBean.productCompleteData != null}">
                                prePopulate: ${actionBean.productCompleteData},
                            </c:if>
                            tokenLimit: 1
                        }
                    );

                    updateAddOnCheckboxes();
                }
            );

            var addOn = new Array();
            <c:forEach items="${actionBean.editOrder.addOns}" var="addOnProduct">
                addOn['${addOnProduct.addOn.businessKey}'] = true;
            </c:forEach>

            function updateAddOnCheckboxes() {
                var productKey = $j("#product").val();
                if ((productKey == null) || (productKey == "")) {
                    $j("#addOnCheckboxes").text('If you select a product, its Add-ons will show up here');
                }

                $j.ajax({
                    url: "${ctxpath}/orders/order.action?getAddOns=&productKey=" + productKey,
                    dataType: 'json',
                    success: setupCheckboxes
                });
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
                    url: "${ctxpath}/orders/order.action?getQuoteFunding&quoteIdentifier=" + quoteIdentifier,
                    dataType: 'json',
                    data: data,
                    success: updateFunds
                });
            }

            function updateFunds(data) {
                $j("#fundsRemaining").text(data.fundsRemaining);
            }
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="createForm" class="form-horizontal">
            <div style="float: left; margin-right: 40px; margin-top: 5px;">
                <stripes:hidden name="businessKey" value="${actionBean.businessKey}"/>
                <div class="control-group">
                    <stripes:label for="orderName" name="Name" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="orderName" name="editOrder.title" class="defaultText"
                            title="Enter the name of the new order" value="${actionBean.editOrder.title}"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="researchProject" name="Research Project" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="researchProject" name="researchProjectList" class="defaultText"
                            title="Enter the research project for this order" value="${actionBean.editOrder.researchProject}"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="product" name="Product" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="product" name="productList" class="defaultText"
                            title="Enter the product name for this order" value="${actionBean.editOrder.product}"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="selectedAddOns" name="Add-ons" class="control-label"/>
                    <div id="addOnCheckboxes" class="controls controls-text"> </div>
                </div>

                <div class="control-group">
                    <stripes:label for="quote" name="Quote" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="quote" name="editOrder.quoteId" class="defaultText"
                                      onchange="updateFundsRemaining"
                                      title="Enter the Quote ID for this order" value="${actionBean.editOrder.quoteId}"/>
                        <div id="fundsRemaining"> </div>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="numberOfLanes" name="Number of Lanes" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="numberOfLanes" name="editOrder.count" class="defaultText"
                            title="Enter Number of Lanes" value="${actionBean.editOrder.count}"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="comments" name="Comments" class="control-label"/>
                    <div class="controls">
                        <stripes:textarea id="comments" name="editOrder.comments" class="defaultText"
                            title="Enter comments" cols="50" rows="3"
                            value="${actionBean.editOrder.comments}"/>
                    </div>
                </div>

                <div class="control-group">
                    <div class="controls">
                        <div class="row-fluid">
                            <div class="span2">
                                <stripes:submit name="save" value="Save"/>
                            </div>
                            <div class="span1">
                                <c:choose>
                                    <c:when test="${actionBean.creating}">
                                        <stripes:link beanclass="${actionBean.class.name}" event="list">Cancel</stripes:link>
                                    </c:when>
                                    <c:otherwise>
                                        <stripes:link beanclass="${actionBean.class.name}" event="view">
                                            <stripes:param name="businessKey" value="${actionBean.editOrder.businessKey}"/>
                                            Cancel
                                        </stripes:link>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div style="float: left; width: 600px;" class="help-block">
                Enter samples names in this box, one per line. When you save the order, the view page will show
                all sample details.
                <br/>
                <br/>
                <stripes:textarea class="controlledText" id="samplesToAdd" name="editOrder.sampleList"
                                  rows="15" cols="120" value="${actionBean.editOrder.sampleList}"/>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
