<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-render name="/layout.jsp" pageTitle="Unauthorized access">
    <stripes:layout-component name="content">

        <div class="row-fluid">
            <div class="span3">
                <img src="${ctxpath}/images/mercury_logo.png" alt="Broad Institute" style="float: left;"/>
            </div>

            <div class="span9">
                <div class="alert alert-block">
                    <h3>Unauthorized access!</h3>

                    <p>
                        You have attempted to access a page that you are not have permission to see.  Please go
                        <a href="javascript:history.back()">back</a> and try again.  If you feel this is an error,
                        please use the <em>Feedback</em> button at the top to report this issue.
                    </p>

                    <br/><br/><br/>
                </div>

            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>