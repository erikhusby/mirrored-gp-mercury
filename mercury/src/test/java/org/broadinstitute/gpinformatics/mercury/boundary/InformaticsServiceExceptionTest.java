package org.broadinstitute.gpinformatics.mercury.boundary;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@Test(groups = TestGroups.DATABASE_FREE)
public class InformaticsServiceExceptionTest {

    public static final String TEMPLATE = "Template with argument %s";
    public static final String ARGUMENT = "foo";

    public void templateWithArgument() {
        InformaticsServiceException exception = new InformaticsServiceException(TEMPLATE, ARGUMENT);
        assertThat(exception.getMessage(), is(equalTo(String.format(TEMPLATE, ARGUMENT))));
    }

    public void templateWithoutArgument() {
        InformaticsServiceException exception = new InformaticsServiceException(TEMPLATE);
        assertThat(exception.getMessage(), is(equalTo(TEMPLATE)));
    }

    public void templateWithArgumentWithThrowable() {
        InformaticsServiceException exception = new InformaticsServiceException(TEMPLATE, new Throwable(), ARGUMENT);
        assertThat(exception.getMessage(), is(equalTo(String.format(TEMPLATE, ARGUMENT))));
    }

    public void templateWithoutArgumentWithThrowable() {
        InformaticsServiceException exception = new InformaticsServiceException(TEMPLATE, new Throwable());
        assertThat(exception.getMessage(), is(equalTo(TEMPLATE)));
    }
}