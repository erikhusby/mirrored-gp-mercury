<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.presentation.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.presentation.Role.roles" %>

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
                        The cause of your problem from <strong>${request.getAttribute("javax.servlet.error.servlet_name")}</strong>:
                    </p>

                    <p class="text-error" style="font-weight: bold; margin-left: 50px;">
                        ${requestScope["javax.servlet.error.message"]}
                    </p>

                    <br/><br/><br/>

                    <security:authorizeBlock roles="<%= roles(Developer)%>">
                        <div style="border: 1px dotted #FF0000; border-radius: 10px;-moz-border-radius: 10px; -webkit-border-radius: 10px; padding: 10px;">
                            <h3>Additional information from the stack trace:</h3>

                            <p>
                            <%
                                Throwable e = (Throwable)request.getAttribute("javax.servlet.error.exception");
                                StackTraceElement[] stack = e.getStackTrace();
                                if (stack != null) {
                                    for(int n = 0; n < Math.min(10, stack.length); n++) {
                                      stack[n].toString();
                                    }
                                }
                            %>
                            </p>
                        </div>
                    </security:authorizeBlock>
                </div>

            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
