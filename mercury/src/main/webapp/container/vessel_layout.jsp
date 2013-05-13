<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.VesselViewActionBean"/>

<style type="text/css">
    .vvCell {
        padding: 2px;
        vertical-align: top;
        height: 100px;
    }

    .vvWell {
        background-color: #e3e3e3;
        border-radius: 20px;
        height: 100%;
    }

    .vvInfo {
        padding-left: 5px;
        padding-right: 5px;
        padding-bottom: 5px;
        font-size: x-small;
    }

    .vvName {
        border-top-left-radius: 20px;
        border-top-right-radius: 20px;
        padding-bottom: 3px;
        text-decoration: underline;
        text-align: center;
        background-color: #bfbfbf;
    }

    .vvTable th {
        text-align: center;
        background-color: #c7c7c7;
    }
</style>

<script type="text/javascript">
    $(document).ready(function () {
        $j('.vvCheckbox').enableCheckboxRangeSelection({
            checkAllClass:'vvCheckAll',
            countDisplayClass:'vvCheckedCount',
            checkboxClass:'vvCheckbox'
        });
    });

    function sectionCheck(sectionClass) {
        var currentCheckValue = $j('#' + sectionClass + '-head').attr('checked');

        // Set all the col or row classes to the opposite of the current state desired by the header checkbox
        if (currentCheckValue == undefined) {
            $j('.' + sectionClass).attr('checked', currentCheckValue);
        } else {
            $j('.' + sectionClass).removeAttr('checked');
        }

        // NOW, do a click to set to the proper choice AND so that it updates the count appropriately
        $j('.' + sectionClass).click();
    }

    function showHeatMapOptions() {
        $j('#heatMapDiv').html("<img src=\"${ctxpath}/images/spinner.gif\"/>");
        var classes = {};
        var classesString = '';
        $j('div[heatmapvalue="true"]').each(function () {
            classes[$j(this).attr("class")] = true;
        });
        for (var key in classes) {
            classesString += key + ',';
        }
        $j('#heatMapDiv').load('${ctxpath}/view/heatMap.action?heatMapFieldString=' + classesString);
        $j('#heatMapDiv').show();
    }
</script>
<a href="javascript:showHeatMapOptions()">Add Heat Map</a><br/>

<div id="heatMapDiv"></div>
<stripes:form beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.VesselViewActionBean">
    <table cellspacing="5" class="vvTable">
        <!-- Need a row of checkboxes for select all and then each select all column box -->
        <tr>
            <th width="40">
                <span id="vvCheckedCount" class="vvCheckedCount"></span><input type="checkbox" class="vvCheckAll"
                                                                               style="float:right;"/>
            </th>

            <c:forEach var="column" items="${actionBean.vessel.vesselGeometry.columnNames}" varStatus="colStatus">
                <th>
                        ${column}
                        <%--<stripes:checkbox name="selectedSamples" id="col-${colStatus.index}-head" style="float:none;"--%>
                        <%--onchange="sectionCheck('col-' + ${colStatus.index})"/>--%>
                    <input id="col-${colStatus.index}-head" type="checkbox" style="float:none;"
                           onchange="sectionCheck('col-' + ${colStatus.index})"/>
                </th>
            </c:forEach>

        </tr>
        <c:forEach var="row" items="${actionBean.vessel.vesselGeometry.rowNames}" varStatus="rowStatus">
            <tr>
                <th style="text-align: center">
                        ${row}
                    <input id="row-${rowStatus.index}-head" type="checkbox" style="float: right;"
                           onchange="sectionCheck('row-' + ${rowStatus.index})"/>
                </th>
                <c:forEach var="column" items="${actionBean.vessel.vesselGeometry.columnNames}" varStatus="colStatus">
                    <td class="vvCell" width="120">
                        <div class="vvWell">
                            <div class="vvName">
                                <stripes:checkbox class="vvCheckbox row-${rowStatus.index} col-${colStatus.index}"
                                                  style="float:none;"
                                                  name="vesselLabel" value="${actionBean.vessel.label}"/>
                                    ${row}${column}
                            </div>
                            <div class="vvInfo">
                                <c:forEach var="sample" items="${actionBean.samplesAtPosition(row, column)}">
                                    <a href="${ctxpath}/search/all.action?search=&searchKey=${sample.startingSample.sampleKey}">
                                            ${sample.startingSample.sampleKey}

                                    </a>

                                    <div class="wellNum" heatmapvalue="true">
                                            ${rowStatus.index * 12 + colStatus.index}
                                        <div style="display:none">${rowStatus.index * 12 + colStatus.index}</div>
                                    </div>
                                    <c:forEach var="metric"
                                               items="${actionBean.sampleToMetricsMap.get(sample.startingSample.sampleKey)}">
                                        <div class="${metric.name}" heatmapvalue="true">
                                                ${metric.name.displayName} : ${metric.value} ${metric.units.displayName}
                                            <div style="display:none">${metric.value}</div>
                                        </div>
                                    </c:forEach>
                                </c:forEach>
                            </div>
                        </div>
                    </td>
                </c:forEach>
            </tr>
        </c:forEach>
    </table>

    <%--<table class="vvTable">--%>
    <%--<tr><th>sample name</th></tr>--%>
    <%--<c:forEach items="${actionBean.selectedCells}" var="sampleInstance">--%>
    <%--<tr><td>${sampleInstance.startingSample.sampleKey}</td></tr>--%>
    <%--</c:forEach>--%>
    <%----%>
    <%--</table>--%>


</stripes:form>
