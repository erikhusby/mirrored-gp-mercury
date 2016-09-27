<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean"--%>

<stripes:layout-definition>
    <script type="text/javascript">
        function applyHeatMap() {
            $j('.' + $j('#heatField').val()).heatcolor(
                    function () {
                        return $j("div", this).text();
                    },
                    {
                        lightness:$j("#amount").val() / 100,
                        reverseOrder:$j('#reverseOrder').is(':checked'),
                        colorStyle:$j('input[name=colorStyle]:checked').val()
                    }
            );
        }

        function clearHeatMap() {
            $j("." + $j('#heatField').val()).each(function () {
                var elToColor = $j(this);
                if (elToColor[0].nodeType == 1)
                    elToColor.css("background-color", "#E3E3E3");
                else if (elToColor[0].nodeType == 3)
                    elToColor.css("color", "#E3E3E3");
            })
        }

        $j(function () {
            $j('#colorSlider').slider({
                range:"min",
                value:45,
                min:0,
                max:90,
                slide:function (event, ui) {
                    applyHeatMap();
                    $j('#amount').val(ui.value);
                }

            });
            $j('#amount').val($j('#colorSlider').slider('value'));
        });
    </script>
    <stripes:form beanclass="${actionBean.class.name}">
        <div>

            <div class="control-group">
                <div class="controls">
                    <select id="heatField" onchange="applyHeatMap()">
                        <c:forEach items="${actionBean.heatMapFields}" var="field">
                            <option value="${field}">${field}</option>
                        </c:forEach>
                    </select>
                </div>
                <div class="controls">
                    <table class="simple">
                        <tr>
                            <td width="100">
                                <input type="radio" onchange="applyHeatMap()" value="roygbiv" name="colorStyle"
                                       id="roygbiv" style="display:inline;">
                                <label for="roygbiv" style="display:inline;">Roygbiv</label>
                            </td>
                            <td>
                                <input type="radio" onchange="applyHeatMap()" value="redtogreen" name="colorStyle"
                                       id="redtogreen" style="display:inline;">
                                <label for="redtogreen" style="display:inline;">Red to green</label>
                            </td>
                            <td>
                                <input type="radio" onchange="clearHeatMap()" value="redtogreen" name="colorStyle"
                                       id="none" style="display:inline;">
                                <label for="none" style="display:inline;">Remove color</label>
                            </td>
                            <td>
                                <stripes:checkbox onchange="applyHeatMap()" name="reverseOrder" id="reverseOrder"
                                                  style="display:inline;"/>
                                <label for="reverseOrder" style="display:inline;">Reverse Color Order</label>
                            </td>

                        </tr>
                        <tr>
                            <td colspan="2">
                                <label for="amount">Color Intensity</label>
                                <input type="hidden" id="amount" disabled="true" size="3" value=".45">
                            </td>
                            <td colspan="5">
                                <div id="colorSlider"></div>
                            </td>
                        </tr>
                    </table>
                </div>
            </div>
        </div>
    </stripes:form>
</stripes:layout-definition>