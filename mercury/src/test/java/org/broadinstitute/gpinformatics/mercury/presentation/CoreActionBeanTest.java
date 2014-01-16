package org.broadinstitute.gpinformatics.mercury.presentation;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletResponse;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean}.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class CoreActionBeanTest {

    private HttpServletResponse mockResponse;

    @BeforeTest(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        mockResponse = mock(HttpServletResponse.class);
    }

    @Test
    public void testSetFileDownloadHeadersNullContentTypeNullFileName() {
        CoreActionBean.setFileDownloadHeaders(null, null, mockResponse);

        verify(mockResponse).setContentType("application/octet-stream");
        verify(mockResponse, never()).setHeader(eq("Content-Disposition"), anyString());
    }

    @Test
    public void testSetFileDownloadHeadersNullFileName() {
        CoreActionBean.setFileDownloadHeaders("test/content-type", null, mockResponse);

        verify(mockResponse).setContentType("test/content-type");
        verify(mockResponse, never()).setHeader(eq("Content-Disposition"), anyString());
    }

    @Test
    public void testSetFileDownloadHeadersNullContentType() {
        CoreActionBean.setFileDownloadHeaders(null, "test.txt", mockResponse);

        verify(mockResponse).setContentType("application/octet-stream; file=\"test.txt\"");
        verify(mockResponse).setHeader("Content-Disposition", "attachment; filename=\"test.txt\"");
    }

    @Test
    public void testSetFileDownloadHeaders() {
        CoreActionBean.setFileDownloadHeaders("test/content-type", "test.txt", mockResponse);

        verify(mockResponse).setContentType("test/content-type; file=\"test.txt\"");
        verify(mockResponse).setHeader("Content-Disposition", "attachment; filename=\"test.txt\"");
    }
}
