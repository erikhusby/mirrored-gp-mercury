<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass=[Path to Action bean class]/>

<stripes:layout-render name="/layout.jsp" pageTitle=[Short Title Name Here] sectionTitle=[Section Title name Here>

<stripes:layout-component name="extraHead">
    <script type="text/javascript">

    </script>
</stripes:layout-component>

<stripes:layout-component name="content">


</stripes:layout-component>
</stripes:layout-render>
