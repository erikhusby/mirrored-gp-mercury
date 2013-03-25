package org.broadinstitute.gpinformatics.infrastructure.tableau;

import org.broadinstitute.gpinformatics.athena.presentation.links.TableauLink;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/** Unit test of Tableau config */

@Test(groups = TestGroups.DATABASE_FREE)
public class TableauLinkTest {
    private TableauConfig tableauConfig;
    private static String SERVERNAME = "http://server.none.org";
    private static String[] NAMES = new String[] {"name1", "name2", "name3"};
    private static String[] URLS = new String[] {"/path1?param=", "/path2?param=", null};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeTest() {
        tableauConfig = new TableauConfig();
        tableauConfig.setTableauServer(SERVERNAME);
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        @SuppressWarnings("unchecked")
        Map<String, String>[] maps = new HashMap[] {
                new HashMap<String, String>(),
                new HashMap<String, String>(),
                new HashMap<String, String>()};
        for (int i = 0; i < maps.length; ++i) {
            maps[i].put("name",NAMES[i]);
            maps[i].put("url", URLS[i]);
            list.add(maps[i]);
        }
        tableauConfig.setReportUrls(list);
    }

    public void testNoInit() {
        assertNull(new TableauConfig().getReportUrl(NAMES[0]));
    }

    public void testConfig() {
        assertEquals(tableauConfig.getReportUrls().size(), NAMES.length);
        assertEquals(tableauConfig.getReportUrl(NAMES[0]), URLS[0]);
    }

    public void testTableauLink() throws Exception {
        TableauLink tl = new TableauLink();
        tl.setTableauConfig(tableauConfig);
        String param = "myParam";
        String url = tl.tableauReportUrl(NAMES[0], param);

        String encodedParam = URLDecoder.decode(param, "UTF-8");
        Assert.assertEquals(url, SERVERNAME + URLS[0] + encodedParam);
    }

}

