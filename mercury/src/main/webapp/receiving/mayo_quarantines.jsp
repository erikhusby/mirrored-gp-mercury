<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoPackageReceiptActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Mayo Quarantines" sectionTitle="Mayo Quarantines">
    <stripes:layout-component name="content">
        <style type="text/css">
            div.displayGroup {
                display: table;
            }
            div.displayGroup > div.displayRow {
                display: table-row;
            }
            div.displayGroup > div.displayRow > div.firstCol {
                display: table-cell;
            }
            div.displayGroup > div.displayRow > div.secondCol {
                display: table-cell;
                padding-left: 10px;
                padding-top: 10px;
            }
        </style>

        <stripes:form beanclass="${actionBean.class.name}" id="quarantinesForm" class="form-horizontal">
            <div class="displayGroup" id="quarantinedItems">
                <c:forEach items="${actionBean.quarantined}" var="qItem">
                    <div class="displayRow">
                        <div class="firstCol">${qItem.item}</div>
                        <div class="secondCol">${qItem.reason}</div>
                    </div>
                </c:forEach>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>