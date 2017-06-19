<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<div class="control-group">
    <stripes:label for="freezerSelect" class="control-label">Freezer</stripes:label>
    <div class="controls">
        <select name="storageLocations[0]" id="storageLocations[0]">
            <c:forEach items="${actionBean.freezerLocations}" var="entry">
                <option value="${entry.label}">${entry.label}</option>
            </c:forEach>
        </select>
    </div>
</div>
<c:forEach items="${actionBean.searchResult.locationTrail}"
           var="location" varStatus="loop">
    <c:if test="${not empty location.childrenStorageLocation}">
        <div class="control-group">
            <stripes:label for="${location.childrenStorageLocation[0].locationType}" class="control-label"/>
            <div class="controls">
                <select name="storageLocations[${loop.index + 1}]" id="storageLocations[${loop.index + 1}]">
                    <c:forEach items="${location.childrenStorageLocation}" var="entry">
                        <option value="${entry.label}">${entry.label}</option>
                    </c:forEach>
                </select>
            </div>
        </div>
    </c:if>
</c:forEach>