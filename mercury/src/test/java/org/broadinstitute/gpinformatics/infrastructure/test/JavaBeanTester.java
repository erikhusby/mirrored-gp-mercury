package org.broadinstitute.gpinformatics.infrastructure.test;

import org.testng.Assert;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;


/**
 * This helper class can be used to unit test the get/set methods of JavaBean-style Value Objects.
 *
 * @author rob.dawson
 *
 */
@SuppressWarnings("UnusedDeclaration")
public class JavaBeanTester {
	/**
	 * Tests the get/set methods of the specified class.
	 *
	 * @param <T> the type parameter associated with the class under test
	 * @param clazz the Class under test
	 * @param skipThese the names of any properties that should not be tested
	 * @throws IntrospectionException thrown if the Introspector.getBeanInfo() method throws this exception
	 * for the class under test
	 */
	public static <T> void test(final Class<T> clazz, final String... skipThese) throws IntrospectionException {
		final PropertyDescriptor[] props = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
		nextProp: for (PropertyDescriptor prop : props) {
		 // Check the list of properties that we don't want to test
			for (String skipThis : skipThese) {
				if (skipThis.equals(prop.getName())) {
					continue nextProp;
				}
			}
			final Method getter = prop.getReadMethod();
			final Method setter = prop.getWriteMethod();

			if (getter != null && setter != null){
			 // We have both a get and set method for this property
				final Class<?> returnType = getter.getReturnType();
				final Class<?>[] params = setter.getParameterTypes();

				if (params.length == 1 && params[0] == returnType){
				 // The set method has 1 argument, which is of the same type as the return type of the get method, so we can test this property
					try{
					 // Build a value of the correct type to be passed to the set method
						Object value = buildValue(returnType);

					 // Build an instance of the bean that we are testing (each property test gets a new instance)
						T bean = clazz.newInstance();

					 // Call the set method, then check the same value comes back out of the get method
						setter.invoke(bean, value);

						@SuppressWarnings("UnnecessaryLocalVariable")
                        final Object expectedValue = value;
						final Object actualValue = getter.invoke(bean);

                        Assert.assertEquals(expectedValue,
                                String.format("Failed while testing property %s", prop.getName()));

					} catch (Exception ex){
						Assert.fail(String.format("An exception was thrown while testing the property %s: %s",
                                prop.getName(), ex.toString()));
					}
				}
			}
		}
	}

	private static Object buildMockValue(Class<?> clazz){
		if (!Modifier.isFinal(clazz.getModifiers())){
		 // Insert a call to your favourite mocking framework here
			return null;
		} else {
			return null;
		}
	}

	private static Object buildValue(Class<?> clazz) throws InstantiationException, IllegalAccessException, IllegalArgumentException, SecurityException, InvocationTargetException{
	 // If we are using a Mocking framework try that first...
		final Object mockedObject = buildMockValue(clazz);
		if (mockedObject != null){
			return mockedObject;
		}

	 // Next check for a no-arg constructor
		final Constructor<?>[] ctrs = clazz.getConstructors();
		for (Constructor<?> ctr : ctrs) {
			if (ctr.getParameterTypes().length == 0) {
			 // The class has a no-arg constructor, so just call it
				return ctr.newInstance();
			}
		}

	 // Specific rules for common classes
		if (clazz == String.class){
			return "testvalue";

		} else if (clazz.isArray()){
			return Array.newInstance(clazz.getComponentType(), 1);

		} else if (clazz == boolean.class || clazz == Boolean.class){
			return true;

		} else if (clazz == int.class || clazz == Integer.class) {
			return 1;

		} else if (clazz == long.class || clazz == Long.class) {
			return 1L;

		} else if (clazz == double.class || clazz == Double.class) {
			return 1.0D;

		} else if (clazz == float.class || clazz == Float.class) {
			return 1.0F;

		} else if (clazz == char.class || clazz == Character.class) {
			return 'Y';
            //TODO PMB need to refactor BSPSampleDTO_PMB out
//        } else if ( clazz == BSPSampleDTO.class) {
//            return new BSPSampleDTO("containerId","stockSample","rootSample","null","patientId","organism","collaboratorSampleId","collection",new BigDecimal("1"),new BigDecimal("2"));

	 // Add your own rules here

		} else {
			Assert.fail("Unable to build an instance of class " + clazz.getName() + ", please add some code to the "
					+ JavaBeanTester.class.getName() + " class to do this.");
			return null; // for the compiler
		}
	}
}
