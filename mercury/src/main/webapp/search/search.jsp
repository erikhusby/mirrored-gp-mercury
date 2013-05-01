<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Search Vessels" sectionTitle="Search">

    <stripes:layout-component name="extraHead">

        <script type="text/javascript">
            function showResult(type) {
                $j('#' + type + 'Div').show();
                $j('#' + type + "Anchor").hide();
                $j('#' + type + "AnchorHide").show();
            }

            function hideResult(type) {
                $j('#' + type + 'Div').hide();
                $j('#' + type + "Anchor").show();
                $j('#' + type + "AnchorHide").hide();
            }
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="searchForm" class="form-horizontal">
            <div class="form-horizontal">
                <div class="control-group" style="margin-bottom:5px;">
                    <stripes:label for="barcode" class="control-label" style="width: 60px;">barcode</stripes:label>
                    <div class="controls" style="margin-left: 80px;">
                        <stripes:textarea rows="5" cols="160" name="searchKey" id="name"
                                          title="Enter the value to search"
                                          style="width:auto;" class="defaultText"/>
                    </div>
                </div>

                <div class="control-group">
                    <div class="controls" style="margin-left: 80px;">
                        <stripes:submit name="search" value="Search"/>
                    </div>
                </div>
            </div>
        </stripes:form>

        <c:if test="${not actionBean.resultsAvailable}">
            No Results Found
        </c:if>

        <c:if test="${not empty actionBean.foundVessels}">

            <!-- This should be using a batch action bean class to do the create gesture-->
            <div class="tableBar">
                Found ${fn:length(actionBean.foundVessels)} Vessels

                <c:if test="${actionBean.multipleResultTypes}">
                    <a id="vesselAnchor" href="javascript:showResult('vessel')" style="margin-left: 10px;">show</a>
                    <a id="vesselAnchorHide" href="javascript:hideResult('vessel')"
                       style="display:none; margin-left: 10px;">hide</a>
                </c:if>

                <div class="pull-right">
                    <img alt="show plate view" width="20" height="20" name="" title="show plate view"
                         src="${ctxpath}/images/plate.png" style="margin-top: -5px;"/> - plate layout
                    <img alt="show sample view" width="20" height="20" name="" title="show sample view"
                         src="${ctxpath}/images/list.png" style="margin-top: -5px; margin-left: 10px;"/> - sample
                    list
                </div>
            </div>

            <!-- If we get here, then it is showing at least this one, SO, if there are mutliple, hide it, otherwise just show this only one -->
            <c:choose>
                <c:when test="${actionBean.multipleResultTypes}">
                    <div id="vesselDiv" style="${actionBean.resultTypeStyle}">
                </c:when>
                <c:otherwise>
                    <div id="vesselDiv">
                </c:otherwise>
            </c:choose>
            <stripes:layout-render name="/search/vessel_list.jsp" vessels="${actionBean.foundVessels}"
                                   bean="${actionBean}" showCheckboxes="false"/>
            </div>
        </c:if>

        <c:if test="${not empty actionBean.foundSamples}">
            <div class="tableBar" style="margin-top: 5px;">
                Found ${fn:length(actionBean.foundSamples)} Samples

                <c:if test="${actionBean.multipleResultTypes}">
                    <a id="sampleAnchor" href="javascript:showResult('sample')" style="margin-left: 10px;">show</a>
                    <a id="sampleAnchorHide" href="javascript:hideResult('sample')"
                       style="display:none; margin-left: 10px;">hide</a>
                </c:if>
            </div>

            <!-- If we get here, then it is showing at least this one, SO, if there are mutliple, hide it, otherwise just show this only one -->
            <c:choose>
                <c:when test="${actionBean.multipleResultTypes}">
                    <div id="sampleDiv" style="display:none">
                </c:when>
                <c:otherwise>
                    <div id="sampleDiv">
                </c:otherwise>
            </c:choose>
            <stripes:layout-render name="/sample/sample_list.jsp" samples="${actionBean.foundSamples}"
                                   bean="${actionBean}" showCheckboxes="false"/>
            </div>
        </c:if>

        <c:if test="${not empty actionBean.foundPDOs}">
            <div class="tableBar" style="margin-top: 5px;">
                Found ${fn:length(actionBean.foundPDOs)} PDOs

                <c:if test="${actionBean.multipleResultTypes}">
                    <a id="pdoAnchor" href="javascript:showResult('pdo')" style="margin-left: 10px;">show</a>
                    <a id="pdoAnchorHide" href="javascript:hideResult('pdo')" style="display:none; margin-left: 10px;">hide</a>
                </c:if>
            </div>

            <!-- If we get here, then it is showing at least this one, SO, if there are mutliple, hide it, otherwise just show this only one -->
            <c:choose>
                <c:when test="${actionBean.multipleResultTypes}">
                    <div id="pdoDiv" style="display:none">
                </c:when>
                <c:otherwise>
                    <div id="pdoDiv">
                </c:otherwise>
            </c:choose>
            <stripes:layout-render name="/orders/pdo_list.jsp" pdos="${actionBean.foundPDOs}"
                                   bean="${actionBean}" showCheckboxes="false"/>
            </div>
        </c:if>

        <c:if test="${not empty actionBean.foundBatches}">
            <stripes:form beanclass="${actionBean.class.name}" id="vesselForm" class="form-horizontal">
                <div class="tableBar" style="margin-top: 5px;">
                    Found ${fn:length(actionBean.foundBatches)} Batches

                    <c:if test="${actionBean.multipleResultTypes}">
                        <a id="batchAnchor" href="javascript:showResult('batch')" style="margin-left: 10px;">show</a>
                        <a id="batchAnchorHide" href="javascript:hideResult('batch')"
                           style="display:none; margin-left: 10px;">hide</a>
                    </c:if>
                </div>

                <!-- If we get here, then it is showing at least this one, SO, if there are mutliple, hide it, otherwise just show this only one -->
                <c:choose>
                    <c:when test="${actionBean.multipleResultTypes}">
                        <div id="batchDiv" style="display:none">
                    </c:when>
                    <c:otherwise>
                        <div id="batchDiv">
                    </c:otherwise>
                </c:choose>
                <stripes:layout-render name="/batch/batch_list.jsp" batches="${actionBean.foundBatches}"
                                       bean="${actionBean}" showCheckboxes="false"/>
                </div>
            </stripes:form>
        </c:if>
    </stripes:layout-component>
</stripes:layout-render>
