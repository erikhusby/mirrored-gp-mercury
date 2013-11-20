<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.LCSetSearchActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Search LCSets" sectionTitle="Search LCSets">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {

                $j("#accordion").accordion({  collapsible:true, active:false, heightStyle:"content", autoHeight:false});
                $j("#accordion").show();
                if (${fn:length(actionBean.foundBatches) == 1}) {
                    $j("#accordion").accordion({active:0})
                }

                if (${not actionBean.searchDone}) {
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
                    <div class="control-group">
                        <stripes:label for="barcode" class="control-label">LCSet name(s)</stripes:label>
                        <div class="controls">
                            <stripes:textarea rows="5" cols="160" name="searchKey" id="name"
                                              title="Enter the value to search"
                                              style="width:auto;" class="defaultText"/>
                        </div>
                    </div>

                    <div class="control-group">
                        <div class="controls">
                            <stripes:submit name="lcsetSearch" value="Search" class="btn btn-primary"/>
                        </div>
                    </div>
                </div>
            </stripes:form>

        </div>
        <div id="searchResults">
            <c:if test="${not actionBean.resultsAvailable}">
                No Results Found
            </c:if>
            <c:if test="${not empty actionBean.foundBatches}">
                <div id="resultSummary">Searched for ${actionBean.numSearchTerms} LCSET(s),
                    found ${fn:length(actionBean.foundBatches)}.
                </div>

                <div id="accordion" style="display:none;" class="accordion">
                    <c:forEach items="${actionBean.foundBatches}" var="batch" varStatus="status">

                        <div style="padding-left: 30px;padding-bottom: 2px">
                            <stripes:layout-render name="/batch/lcset_info_header.jsp" bean="${actionBean}"
                                                   batch="${batch}"/>
                        </div>

                        <div id="batchList-${batch.batchName}">
                            <div>
                                <stripes:layout-render name="/batch/lcset_sample_list.jsp" batch="${batch}"
                                                       index="${status.count}" bean="${actionBean}"/>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:if>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
