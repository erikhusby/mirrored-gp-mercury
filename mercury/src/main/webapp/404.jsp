<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-render name="/layout.jsp" pageTitle="404">
    <stripes:layout-component name="content">

        <div class="row-fluid">
            <div class="span3">
                <img src="${ctxpath}/images/mercury_logo.png" alt="Broad Institute" style="float: left;"/>
            </div>

            <div class="span9">
                <div class="alert alert-block">
                    <h3>Sorry! Mercury lost the page you were looking for!</h3>

                    <p>
                        Sorry, but the page you were looking for can't be found. Try typing the URL again or
                        simply try to start from the <a href="${ctxpath}">homepage</a> instead.

                    </p>
                    <br/><br/><br/>
                </div>

            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>



