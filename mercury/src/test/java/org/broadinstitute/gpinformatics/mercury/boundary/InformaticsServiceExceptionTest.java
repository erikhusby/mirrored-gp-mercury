package org.broadinstitute.gpinformatics.mercury.boundary;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@Test(groups = TestGroups.DATABASE_FREE)
public class InformaticsServiceExceptionTest {

    public static final String TEMPLATE_1_ARGUMENT = "Template with argument %s";
    public static final String TEMPLATE_2_ARGUMENTS = "Template with argument %s and %s";
    public static final String ARGUMENT = "foo";

    public void templateOneWithArgument() {
        InformaticsServiceException exception = new InformaticsServiceException(TEMPLATE_1_ARGUMENT, ARGUMENT);
        assertThat(exception.getMessage(), is(equalTo(String.format(TEMPLATE_1_ARGUMENT, ARGUMENT))));
    }

    public void templateOneMissingArgument() {
        InformaticsServiceException exception = new InformaticsServiceException(TEMPLATE_1_ARGUMENT);
        assertThat(exception.getMessage(), is(equalTo(TEMPLATE_1_ARGUMENT)));
    }

    public void templateOneWithArgumentWithThrowable() {
        InformaticsServiceException exception = new InformaticsServiceException(new Throwable(), TEMPLATE_1_ARGUMENT,
                ARGUMENT);
        assertThat(exception.getMessage(), is(equalTo(String.format(TEMPLATE_1_ARGUMENT, ARGUMENT))));
    }

    public void templateOneMissingArgumentWithThrowable() {
        InformaticsServiceException exception = new InformaticsServiceException(new Throwable(), TEMPLATE_1_ARGUMENT);
        assertThat(exception.getMessage(), is(equalTo(TEMPLATE_1_ARGUMENT)));
    }

    public void templateTwoWithArguments() {
        InformaticsServiceException exception = new InformaticsServiceException(TEMPLATE_2_ARGUMENTS, ARGUMENT, ARGUMENT);
        assertThat(exception.getMessage(), is(equalTo(String.format(TEMPLATE_2_ARGUMENTS, ARGUMENT, ARGUMENT))));
    }

    public void templateTwoMissingArgument() {
        InformaticsServiceException exception = new InformaticsServiceException(TEMPLATE_2_ARGUMENTS, ARGUMENT);
        assertThat(exception.getMessage(), is(equalTo(TEMPLATE_2_ARGUMENTS)));
    }

    public void templateTwoWithArgumentsWithThrowable() {
        InformaticsServiceException exception = new InformaticsServiceException(new Throwable(), TEMPLATE_2_ARGUMENTS,
                ARGUMENT, ARGUMENT);
        assertThat(exception.getMessage(), is(equalTo(String.format(TEMPLATE_2_ARGUMENTS, ARGUMENT, ARGUMENT))));
    }

    public void templateTwoMissingArgumentWithThrowable() {
        InformaticsServiceException exception = new InformaticsServiceException(new Throwable(), TEMPLATE_2_ARGUMENTS, ARGUMENT);
        assertThat(exception.getMessage(), is(equalTo(TEMPLATE_2_ARGUMENTS)));
    }
}