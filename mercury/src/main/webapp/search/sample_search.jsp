<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.SampleSearchActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Search Samples" sectionTitle="Search Samples">
    <stripes:layout-component name="extraHead">

        <script type="text/javascript">
            $(document).ready(function () {
                if (${empty actionBean.mercurySampleToVessels}) {
                    showSearch();
                }
                else {
                    hideSearch();
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
                                       style="width: 60px;">Sample ID(s)</stripes:label>
                        <div class="controls" style="margin-left: 80px;">
                            <stripes:textarea rows="5" cols="160" name="searchKey" id="name"
                                              title="Enter the value to search"
                                              style="width:auto;" class="defaultText"/>
                        </div>
                    </div>

                    <div class="control-group">
                        <div class="controls" style="margin-left: 80px;">
                            <stripes:submit name="sampleSearch" value="Search"/>
                        </div>
                    </div>
                </div>
            </stripes:form>
        </div>
        <c:if test="${empty actionBean.mercurySampleToVessels}">
            No Results Found
        </c:if>
        <div id="searchResults">
            <c:if test="${not empty actionBean.mercurySampleToVessels}">
                <div id="resultSummary">Found ${fn:length(actionBean.mercurySampleToVessels)} Samples</div>
                <hr style="margin-top: 5px; margin-bottom: 5px;"/>
                <c:forEach items="${actionBean.mercurySampleToVessels}" var="sampleToVessels" varStatus="status">
                    <stripes:layout-render name="/sample/sample_info_header.jsp" bean="${actionBean}"
                                           sample="${sampleToVessels.key}"/>
                    <stripes:layout-render name="/sample/sample_event_list.jsp" vessels="${sampleToVessels.value}"
                                           index="${status.count}" bean="${actionBean}"
                                           sample="${sampleToVessels.key}"/>
                    <hr style="color: #0088CC; background-color: #0088CC; height: 2px; margin-top: 10px; margin-bottom: 10px;"/>
                </c:forEach>
            </c:if>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
