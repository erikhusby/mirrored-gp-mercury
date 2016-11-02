<%@ page import="org.broadinstitute.gpinformatics.athena.entity.infrastructure.AccessStatus" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.admin.SAPAccessControlActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.accessController.controlTitle}"
                       sectionTitle="Manage availability of SAP">

    <stripes:layout-component name="extraHead">
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}">
            <div id="enableAccess">
                        <stripes:checkbox value="<%=AccessStatus.DISABLED.name()%>" name="enabledAccess" id="DISABLED-id"/> Disable SAP Access
                        <stripes:checkbox value="<%=AccessStatus.ENABLED.name()%>" name="enabledAccess" id="ENABLED-id"/> Enable SAP Access
            </div>

            <div class="form-horizontal span7">

                <div id="priceItems" class="control-group">
                    <c:set var="preSelectedOptions" value="${actionBean.selectedOptionsString}"/>

                    <c:forEach items="${actionBean.priceListOptions}" var="priceItem">

                        <div class="controls">
                            <div class="form-value">

                                    <%--<c:choose>--%>
                                    <%--<c:when test="${fn:contains(preSelectedOptions, priceItem)}">--%>
                                    <%--<stripes:checkbox name="selectedPriceItems" class="shiftCheckbox"--%>
                                    <%--title="${priceItem}"--%>
                                    <%--value="${priceItem}" id="${priceItem}"--%>
                                    <%--checked="checked"/> ${priceItem}--%>
                                    <%--</c:when>--%>
                                    <%--<c:otherwise>--%>
                                <stripes:checkbox name="selectedPriceItems" class="shiftCheckbox"
                                                  title="${priceItem}"
                                                  value="${priceItem}" id="${priceItem}-id"/>
                                <stripes:label for="${priceItem}-id">
                                    ${priceItem}
                                </stripes:label>
                                    <%--</c:otherwise>--%>
                                    <%--</c:choose>--%>
                            </div>

                                <%--<stripes:label for="${priceItem}" class="control-label">${priceItem}</stripes:label>--%>
                        </div>
                    </c:forEach>
                </div>

                <div class="controls actionButtons">
                    <stripes:submit name="setAccess" value="Set Selected Access options" class="btn"/>
                    <stripes:submit name="resetAccess" value="Reset the Access Settings" class="btn"/>
                </div>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>    