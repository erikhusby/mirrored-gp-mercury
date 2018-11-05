<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.storage.CreateStorageActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Create Storage" sectionTitle="Create Storage">

    <stripes:layout-component name="extraHead">
        <link rel="stylesheet"
              href="${ctxpath}/resources/scripts/jsTree/themes/default/style.min.css"/>
        <script src="${ctxpath}/resources/scripts/jsTree/jstree.min.js"></script>
        <script type="javascript">
            var ctxpath = ${ctxpath};
        </script>
        <script src="${ctxpath}/resources/scripts/storage-location-ajax.js"></script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <div id="storage_location_overlay">
            <div class="control-group">
                <div class="control">
                    <input type="text" id="searchTerm" name="searchTerm" placeholder="storage barcode"/>
                    <input type="submit" value="Find" id="searchTermSubmit"/>
                </div>
            </div>

            <div id="ajax-jstree"></div>
        </div>

        <%--Step 1 Choose Storage Unit Type--%>
        <stripes:form beanclass="${actionBean.class.name}" id="createStorageForm">
            <stripes:hidden id="createdStorageId" name="createdStorageId" value="${actionBean.createdStorageId}" />
            <div class="form-horizontal">
                <div class="control-group">
                    <stripes:label for="storageUnitName" name="Unit Name" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="storageUnitName" name="name"/>
                    </div>
                </div>
                <div class="control-group">
                    <stripes:label for="storageUnitTypeName" name="Unit Type" class="control-label"/>
                    <div class="controls">
                        <stripes:select id="storageUnitTypeName" name="storageUnitTypeName">
                            <stripes:options-collection collection="${actionBean.creatableLocationTypes}"
                                                        label="displayName" value="displayName"/>
                        </stripes:select>
                    </div>
                </div>
                <div class="control-group">
                    <div class="controls">
                        <stripes:submit id="chooseStorageType" name="chooseStorageType" value="Select Type" class="btn"/>
                    </div>
                </div>

                <c:if test="${actionBean.readyForDetails}">
                    <c:choose>
                        <c:when test="${actionBean.locationType.moveable}">
                            <div class="control-group">
                                <stripes:label for="slots" name="Number Of Slots" class="control-label"/>
                                <div class="controls">
                                    <stripes:text id="slots" name="slots"/>
                                </div>
                            </div>
                            <div class="control-group">
                                <stripes:label for="storageName" class="control-label"/>
                                <div class="controls">
                                    <stripes:text id="storageName" name="storageName"  readonly="true"/>
                                    <stripes:submit name="browse" id="browse" value="Browse"
                                                    class="btn btn-primary" onclick="handleBrowseClick(event);"/>
                                </div>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="control-group">
                                <stripes:label for="sections" name="Number Of Sections" class="control-label"/>
                                <div class="controls">
                                    <stripes:text id="sections" name="sections"/>
                                </div>
                            </div>
                            <div class="control-group">
                                <stripes:label for="shelves" name="Number Of Shelves" class="control-label"/>
                                <div class="controls">
                                    <stripes:text id="shelves" name="shelves"/>
                                </div>
                            </div>
                        </c:otherwise>
                    </c:choose>
                    <div class="control-group">
                        <div class="controls">
                            <stripes:hidden id="storageId" name="storageId" value="${actionBean.storageId}" />
                            <stripes:hidden name="readyForDetails" value="${actionBean.readyForDetails}" />
                            <stripes:submit id="createStorageUnit" name="chooseStorageUnit" value="Create"
                                            class="btn btn-primary"/>
                        </div>
                    </div>
                </c:if>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>