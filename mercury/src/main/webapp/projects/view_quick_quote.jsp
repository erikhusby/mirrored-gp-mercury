<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.QuickQuoteActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Quick Quote"
                       sectionTitle="Quote for Project: ${actionBean.researchProject}">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('.PlatformOverage').each(
                    function() {
                        var fullName = $j(this).attr("name");
                        var firstPart = fullName.split("[")[1];
                        var abbreviation = firstPart.split("]")[0];
                        platformOverages[abbreviation] = $j(this).attr("value");
                    });

                $j('#priceItem').dataTable( {
                    "bStateSave": true,
                    "bPaginate": false,
                    "asStripClasses": [ 'odd', 'even' ],
                    "aaSorting": [[1,'asc']],
                    "aoColumns": [
                        {"bSortable": true, "sType": "html" },
                        {"bSortable": true },
                        {"bSortable": true },
                        {"bSortable": true, "sType": "currency" },
                        {"bSortable": true }
                    ] }
                );

                updateTotals();
            });

            var priceItems = [];
            var countedPriceItems = [];
            var platformOverages = [];

            var itemCount = 0;

            function PriceItem(name, platformAbbreviation, platformName, categoryName, price, unit) {
                this.name = name;
                this.platformName = platformName;
                this.platformAbbreviation = platformAbbreviation;
                this.categoryName = categoryName;
                this.price = price;
                this.unit = unit;
                this.quantity = 0.0;
                this.itemOverage = 0.0;

                this.setQuantity = function(quantity) {
                    this.quantity = quantity;
                };

                this.getQuantity = function() {
                    return this.quantity;
                };

                this.getPrice = function() {
                    return this.price;
                };
            }

            function updateQuantity(input, itemCountIndex) {
                countedPriceItems[itemCountIndex].setQuantity($j(input).val());
                updateTotals();
            }

            function updateTotals() {
                var itemTotal = 0.0;
                var itemOverageTotal = 0.0;
                var platformOverageTotal = 0.0;

                for (var i = 0; i < countedPriceItems.length; i++) {
                    if (countedPriceItems[i] != null) {
                        var currAbbreviation = countedPriceItems[i].platformAbbreviation;

                        var currentItemTotal = countedPriceItems[i].getQuantity() * countedPriceItems[i].getPrice();
                        itemTotal += currentItemTotal;

                        var itemOverage = countedPriceItems[i].itemOverage;
                        var itemMultiplier = 1.0 + itemOverage;
                        itemOverageTotal += currentItemTotal * itemMultiplier;

                        var platformOverage = 0;
                        if (platformOverages[currAbbreviation] != null) {
                            platformOverage = platformOverages[currAbbreviation] / 100.0;
                        }
                        var platformMultiplier = itemMultiplier * (1.0 + platformOverage);
                        platformOverageTotal += currentItemTotal * platformMultiplier;
                    }
                }

                $j('#itemTotal').html(formatPrice(itemTotal));
                $j('#itemOverageTotal').html(formatPrice(itemOverageTotal));
                $j('#platformOverageTotal').html(formatPrice(platformOverageTotal));

                $j('#itemTotalInput').attr("value", formatPrice(itemTotal));
                $j('#itemOverageTotalInput').attr("value", formatPrice(itemOverageTotal));
                $j('#platformOverageTotalInput').attr("value", formatPrice(platformOverageTotal));
            }

            function updatePlatformOverage(input, abbreviation) {
                platformOverages[abbreviation] = $j(input).attr("value");
                updateTotals();
            }

            function updateItemOverage(input, itemCountIndex) {
                countedPriceItems[itemCountIndex].itemOverage = $j(input).attr("value") / 100;
                updateTotals();
            }

            function formatPrice(total) {
                var newPrice = total.toFixed(2);

                var splitDecimal = newPrice.split('.');
                var x1 = splitDecimal[0];
                var x2 = splitDecimal.length > 1 ? '.' + splitDecimal[1] : '.00';
                var rgx = /(\d+)(\d{3})/;
                while (rgx.test(x1)) {
                    x1 = x1.replace(rgx, '$1' + ',' + '$2');
                }

                return '$' + x1 + x2;
            }

            function removeItem(itemCountIndex) {
                var itemToRemove = '#item-' + itemCountIndex;

                $j(itemToRemove).remove();

                var index = countedPriceItems[itemCountIndex].platformName + "_" +
                            countedPriceItems[itemCountIndex].categoryName + "_" +
                            countedPriceItems[itemCountIndex].name;

                priceItems[index] = null;
                countedPriceItems[itemCountIndex] = null;

                updateTotals();
            }

            function addItem(name, platformName, platformAbbreviation, categoryName, price, priceItemId, unit ) {
                var index = platformName + "_" + categoryName + "_" + name;

                if (priceItems[index] == null) {
                    countedPriceItems[itemCount] = new PriceItem(name, platformAbbreviation, platformName, categoryName, price, unit);
                    priceItems[index] = countedPriceItems[itemCount];

                    var divStart = "<div id=\"item-" + itemCount + "\">";
                    var divEnd = "</div>";
                    var removeItem = "<a onclick=\"removeItem(" + itemCount + ")\">" +
                                        "<img src=\"${ctxpath}/images/trash.png\"/></a>&nbsp;";

                    var quantityText = "<input type=\"text\" " +
                                               "name=\"quantities[" + itemCount + "]\" " +
                                               "size=\"5\"" +
                                               "onchange=\"updateQuantity(this, " + itemCount + ")\"/> ";

                    var hiddenText = "<input type=\"hidden\" value=\"" + priceItemId + "\" name=\"priceItemIds[" + itemCount + "]\"/> ";

                    var newItemText = divStart + hiddenText +
                                       "<table>" +
                                            "<tr>"
                                              + "<td>" + removeItem + "</td>" +
                                                "<td>" + quantityText + "</td>" +
                                                "<td>" +
                                                    "<b>" + name + "</b><br/>" +
                                                    platformAbbreviation + " - " + categoryName + " - $" + price.toFixed(2) +
                                                "</td>"
                                              + "<td>" +
                                                "<input type=\"text\" " +
                                                       "name=\"overages[" + itemCount + "]\" " +
                                                       "size=\"5\"" +
                                                       "value=\"0.0\" onchange=\"updateItemOverage(this, " + itemCount + ")\"/>(% item overage)" +
                                            "</td></tr>" +
                                       "</table>" +
                                      divEnd;
                    $j('#items').prepend(newItemText);

                    $j('#itemCountInput').attr("value", ++itemCount);
                }
            }

            function reloadWithPriceList() {
                if ($j("#priceListSelection").val() > 0) {
                    document.location.href = "${ctxpath}//quote/QuickQuote.action?showQuickQuote=true&priceList.id=" + $j("#priceListSelection").val();
                }
            }

        </script>

        <style type="text/css">
            .TotalTable th {
                background-color:#ccffcc;
                font-size: 12px;
            }

            .TotalTable td {
                font-size: 14px;
                text-align: center;
            }

            #priceItems {
                overflow:auto;
                height:300px;
            }

            #right {
                float:left;
                padding-left: 10px;
            }

            #left {
                float:left;
                width:500px;
            }

            #items {
                overflow:auto;
                width:100%;
                height:100%;
            }

            .well {
                background-color: #F5F5F5;
                border: 1px solid rgba(0, 0, 0, 0.05);
                border-radius: 4px 4px 4px 4px;
                box-shadow: 0 1px 1px rgba(0, 0, 0, 0.05) inset;
                margin-bottom: 20px;
                min-height: 20px;
                padding: 10px;
                width: 800px;
            }


        </style>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:messages/>
        <stripes:errors/>

        <h1 class="h1">Quick Quote</h1>

        <p>
            Quickly create a quote by clicking on any price item from the list below and updating the platform overage,
            item quantities and item overages.
        </p>

        <stripes:form beanclass="${actionBean.class.name}">
            <input type="hidden" id="itemTotalInput" name="itemTotal"/>
            <input type="hidden" id="itemOverageTotalInput" name="itemOverageTotal"/>
            <input type="hidden" id="platformOverageTotalInput" name="platformOverageTotal"/>
            <input type="hidden" id="itemCountInput" name="itemCount" value="0"/>

            <c:if test="${not empty actionBean.priceList}">
                <stripes:hidden name="priceList.id" value="${actionBean.priceList.id}"/>
                <div class="well">
                    <table class="TotalTable">
                        <tr>
                            <th width="220px">Item Total</th>
                            <th width="220px">w/Item Overage</th>
                            <th width="220px">w/Platform Overage</th>
                            <td rowspan="2">
                                <input type="submit" name="downloadQuoteInfo" value="Download"/><br/>
                                <input type="submit" name="createNewQuote" value="Create Quote"/>
                            </td>
                        </tr>
                        <tr>
                            <td id="itemTotal"></td>
                            <td id="itemOverageTotal"></td>
                            <td id="platformOverageTotal" ></td>
                        </tr>
                    </table>
                    <table>
                        <tr>
                            <td>Platform Overages:</td>
                            <c:forEach items="${actionBean.platforms}" var="platform" varStatus="loop">
                                <td>
                                    <label for="platformOverages[${platform.abbreviation}]">${platform.abbreviation}</label>
                                    <input name="platformOverages[${platform.abbreviation}]"
                                           id="platformOverages[${platform.abbreviation}]"
                                           class="PlatformOverage"
                                           value="${platform.defaultOveragePercent}"
                                           onchange="updatePlatformOverage(this, '${platform.abbreviation}')"
                                           size="3"/>
                                </td>
                            </c:forEach>
                        </tr>
                    </table>
                </div>
            </c:if>

            <div id="left">
                <c:choose>
                    <c:when test="${not empty actionBean.priceList}">
                        <h1 class="h1">Price List</h1>
                        <div id="priceItems" class="scrollTableContainer">
                            <table id="priceItem" class="scrollTable table its">
                                <thead>
                                <tr>
                                    <th>Name</th>
                                    <th>Platform</th>
                                    <th>Category</th>
                                    <th>Price</th>
                                    <th>Unit</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach items="${actionBean.activePriceItems}" var="item">
                                    <tr>
                                        <td>
                                            <a onclick="addItem('${item.name}', '${item.platformName}', '${item.shortName}', '${item.categoryName}', ${item.price}, ${item.id}, '${item.unit}');">
                                                    ${item.name}
                                            </a>
                                        </td>
                                        <td>${item.platformName}</td>
                                        <td>${item.categoryName}</td>
                                        <td>
                                            $<fmt:formatNumber maxFractionDigits="2"
                                                               minFractionDigits="2"
                                                               type="number"
                                                               value="${item.price}"/>
                                        </td>
                                        <td>${item.unit}</td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                        </div>

                        <div id="right">
                            <h1 class="h1">Items</h1>
                            <div id="items"></div>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <h1 class="h1">Genomics Platform Price List</h1>
                        <stripes:hidden name="priceList" value="1"/>
                    </c:otherwise>
                </c:choose>

        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
