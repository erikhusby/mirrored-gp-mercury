<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.VesselViewActionBean"/>

<style type="text/css">
    .vvCell {
        margin-top: 3px;
        background-color: #e3e3e3;
        border-radius: 20px;
        border-style: solid;
        border-width: 1px;
        height: 50px;
        vertical-align: top;
    }

    .vvInfo {
        margin: 8px;
    }

    .vvName {
        padding-bottom: 3px;
        text-decoration: underline;
        text-align: center;
    }

    .vvTable th {
        text-align: center;
        background-color: #c7c7c7;
    }
</style>

<script type="text/javascript">
    $(document).ready(function () {
        $j('.vvCheckbox').enableCheckboxRangeSelection({
            checkAllClass: 'vvCheckAll',
            countDisplayClass: 'vvCheckedCount',
            checkboxClass: 'vvCheckbox'});
    });

    function sectionCheck(sectionClass) {
        var currentCheckValue = $j('#' + sectionClass + '-head').attr('checked');

        if (currentCheckValue == undefined) {
            $j('.' + sectionClass).removeAttr('checked');
            $j('.' + sectionClass).updateCheckCount();
        } else {
            $j('.' + sectionClass).attr('checked', currentCheckValue);
            $j('.' + sectionClass).updateCheckCount();
        }
    }
</script>

<table cellspacing="5" class="vvTable">
    <!-- Need a row of checkboxes for select all and then each select all column box -->
    <tr>
        <th width="40">
            <span id="count" class="vvCheckedCount"></span><input type="checkbox" class="vvCheckAll" style="float:right;"/>
        </th>

        <c:forEach var="column" items="${actionBean.vessel.vesselGeometry.columnNames}" varStatus="colStatus">
            <th>
                <input id="col-${colStatus.index}-head" type="checkbox" style="float:none;" onchange="sectionCheck('col-' + ${colStatus.index})"/>
            </th>
        </c:forEach>
    </tr>
    <c:forEach var="row" items="${actionBean.vessel.vesselGeometry.rowNames}" varStatus="rowStatus">
        <tr>
            <th style="text-align: center">
                <input id="row-${rowStatus.index}-head" type="checkbox" style="float: right;" onchange="sectionCheck('row-' + ${rowStatus.index})"/>
            </th>
            <c:forEach var="column" items="${actionBean.vessel.vesselGeometry.columnNames}" varStatus="colStatus">
                <td class="vvCell">
                    <div class="vvName">
                        <input type="checkbox" class="vvCheckbox row-${rowStatus.index} col-${colStatus.index}" style="float:none;"
                                          name="vesselLabel" value="${actionBean.vessel.label}"/>
                        ${row}${column}
                    </div>
                    <div class="vvInfo">
                        <c:forEach var="sample" items="${actionBean.samplesAtPosition(row, column)}">
                            ${sample.startingSample.sampleKey}<br/>
                        </c:forEach>
                    </div>
                </td>
            </c:forEach>
        </tr>
    </c:forEach>
</table>
