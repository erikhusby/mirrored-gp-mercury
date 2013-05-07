<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.VesselSearchActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Search Vessels" sectionTitle="Search Vessels">
    <stripes:layout-component name="extraHead">

        <script type="text/javascript">
            $(document).ready(function () {
                $j("#accordion").accordion({ collapsible: true, active: false, heightStyle: "content", autoHeight: false });
                $j("#accordion").show();

                if (${empty actionBean.foundVessels}) {
                    showSearch();
                }
                else {
                    hideSearch()
                }
            });

            function showSearch() {
                $j('#searchInput').show();
                $j('#searchResults').hide();
            }

            function hideSearch() {
                $j('#searchInput').hide();
                $j('#searchResults').show();
            }
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <div id="searchInput">
            <stripes:form beanclass="${actionBean.class.name}" id="searchForm" class="form-horizontal">
                <div class="form-horizontal">
                    <div class="control-group" style="margin-bottom:5px;">
                        <stripes:label for="barcode" class="control-label"
                                       style="width: 60px;">Vessel Barcode(s)</stripes:label>
                        <div class="controls" style="margin-left: 80px;">
                            <stripes:textarea rows="5" cols="160" name="searchKey" id="name"
                                              title="Enter the value to search"
                                              style="width:auto;" class="defaultText"/>
                        </div>
                    </div>

                    <div class="control-group">
                        <div class="controls" style="margin-left: 80px;">
                            <stripes:submit name="vesselSearch" value="Search"/>
                        </div>
                    </div>
                </div>
            </stripes:form>
            <c:if test="${not actionBean.resultsAvailable}">
                No Results Found
            </c:if>
        </div>
        <div id="searchResults">
            <c:if test="${not empty actionBean.foundVessels}">
                <div id="resultSummary">Found ${fn:length(actionBean.foundVessels)} Vessels</div>

                <div id="accordion" style="display:none;">
                    <c:forEach items="${actionBean.foundVessels}" var="vessel" varStatus="status">
                        <div style="padding-left: 20px;">
                            <stripes:layout-render name="/vessel/vessel_info_header.jsp" bean="${actionBean}"
                                                   vessel="${vessel}"/>
                        </div>

                        <div id="vesselList-${vessel.labCentricName}" style="height: 300px;">
                            <div>
                                <stripes:layout-render name="/vessel/vessel_sample_list.jsp" vessel="${vessel}"
                                                       index="${status.count}" bean="${actionBean}"/>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:if>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
