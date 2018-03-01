<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>

<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean"--%>

<stripes:layout-render name="/layout.jsp" pageTitle="Mercury">
    <stripes:layout-component name="content">

        <div class="row-fluid">
            <div class="span3">
                <img src="${ctxpath}/images/mercury_logo.png" alt="Broad Institute" style="float: left;"/>
            </div>

            <div class="span9">
                <div class="alert alert-block">
                    <h3>Sorry! Mercury seems to have run into a wall!</h3>

                    <p>
                        We're sorry you encountered some problems with our application. If you would
                        like to report this issue, please hit the <strong>Feedback</strong>
                        button on the menubar.
                    </p>

                    <p>
                        The cause of your problem from <strong>${requestScope["javax.servlet.error.request_uri"]}</strong>:
                    </p>

                    <p class="text-error" style="font-weight: bold; margin-left: 50px;">
                        <!-- todo jmt XSS -->
                        ${actionBean.getError(requestScope)}
                    </p>

                    <br/><br/><br/>

                    <security:authorizeBlock roles="<%= roles(Developer)%>">
                        <div style="border: 1px dotted #FF0000; border-radius: 10px;-moz-border-radius: 10px; -webkit-border-radius: 10px; padding: 10px;">
                            <h3>Additional information from the stack trace:</h3>

                            <p>
                                <c:forEach var="stackEntry" end="10" items="${actionBean.getStackTrace(requestScope)}">
                                    ${stackEntry}<br/>
                                </c:forEach>
                            </p>
                        </div>
                    </security:authorizeBlock>
                </div>

            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
