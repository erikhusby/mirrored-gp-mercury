<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.storage.CreateStorageActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Create Storage" sectionTitle="Create Storage">
    <stripes:layout-component name="content">

        <%--Step 1 Choose Storage Unit Type--%>
        <stripes:form beanclass="${actionBean.class.name}" id="createStorageForm">
            <div class="form-horizontal">
                <div class="control-group">
                    <stripes:label for="storageUnitType" name="Unit Type" class="control-label"/>
                    <div class="controls">
                        <stripes:select name="storageUnit.storageUnitType" id="storageUnitType">
                            <stripes:options-enumeration
                                    enum="org.broadinstitute.gpinformatics.mercury.presentation.storage.CreateStorageActionBean.StorageUnitType"
                                    label="displayName"/>
                        </stripes:select>
                    </div>
                </div>
                <div class="control-group">
                    <stripes:label for="storageUnitName" name="Unit Name" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="storageUnitName" name="storageUnit.name"/>
                    </div>
                </div>

                <div class="control-group">
                    <div class="controls">
                        <stripes:submit name="chooseStorageUnit" value="Continue" class="btn btn-primary"/>
                    </div>
                </div>
            </div>
        </stripes:form>

        <%--Step 2 Enter Storage Unit information --%>
        <c:if test="${not empty actionBean.storageUnit}">
            <stripes:form beanclass="${actionBean.class.name}" id="storageUnitInformation">
                <c:choose>
                    <c:when test="${actionBean.storageUnit.storageUnitType.level == 'FREEZER'}">
                        <div class="form-horizontal">
                            <div class="control-group">
                                <stripes:label for="sections" name="Number Of Sections" class="control-label"/>
                                <div class="controls">
                                    <stripes:text id="sections" name="storageUnit.sections"/>
                                </div>
                            </div>
                            <div class="control-group">
                                <stripes:label for="shelves" name="Number Of Shelves" class="control-label"/>
                                <div class="controls">
                                    <stripes:text id="shelves" name="storageUnit.shelves"/>
                                </div>
                            </div>
                            <div class="control-group">
                                <div class="controls">
                                    <stripes:submit name="chooseStorageUnit" value="Submit" class="btn btn-primary"/>
                                </div>
                            </div>
                        </div>
                    </c:when>
                    <c:when test="${actionBean.storageUnit.storageUnitType.level == 'RACK'}">
                        <div class="form-horizontal">
                            <div class="control-group">
                                <stripes:label for="sections" name="Number Of Slots" class="control-label"/>
                                <div class="controls">
                                    <stripes:text id="sections" name="storageUnit.slots"/>
                                </div>
                            </div>
                            <div class="control-group">
                                <div class="controls">
                                    <stripes:submit name="chooseStorageUnit" value="Submit" class="btn btn-primary"/>
                                </div>
                            </div>
                        </div>
                    </c:when>
                </c:choose>
            </stripes:form>
        </c:if>
    </stripes:layout-component>
</stripes:layout-render>