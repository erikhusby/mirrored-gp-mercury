package org.broadinstitute.gpinformatics.athena.presentation.products;

import net.sourceforge.stripes.controller.DispatcherServlet;
import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.mock.MockHttpSession;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.mock.MockServletContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 1/16/13
 * Time: 2:40 PM
 */
public class ProductActionBeanTest {


    private static MockServletContext mockServletContext ;
    private static MockHttpSession mockHttpSession;
    private static MockRoundtrip mockRoundtrip;

    @BeforeClass
    public static void setup()  throws Exception  {
        mockServletContext = new MockServletContext("ProductActionBeanTest");

        Map<String, String> filterParams = new HashMap<String, String>();
            filterParams.put("ActionResolver.Packages", "org.broadinstitute.gpinformatics.athena.presentation.products");
        mockServletContext.addFilter(StripesFilter.class, "StripesFilter", filterParams);
        mockServletContext.setServlet(DispatcherServlet.class, "DispatcherServlet", null);
        mockHttpSession = new MockHttpSession( mockServletContext );

        mockRoundtrip = new MockRoundtrip( mockServletContext,  )
    }

    @Test
    public void testCreate() throws Exception {

    }

    @Test
    public void testEdit() throws Exception {

    }

    @Test
    public void testMaterialTypesAutocomplete() throws Exception {

    }

    @Test
    public void testSave() throws Exception {

    }

    @Test
    public void testGetEditProduct() throws Exception {

    }

    @Test
    public void testSetEditProduct() throws Exception {

    }

    @Test
    public void testGetMaterialTypeCompleteData() throws Exception {

    }

    @Test
    public void testGetMaterialTypeList() throws Exception {

    }

    @Test
    public void testSetMaterialTypeList() throws Exception {

    }
}
