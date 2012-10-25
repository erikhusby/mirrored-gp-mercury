package org.broadinstitute.gpinformatics.mercury.entity;

import org.hibernate.proxy.HibernateProxy;

/**
 * Utility methods for the object relational mapper
 */
public class OrmUtil {
    /**
     * For objects with {@link HibernateProxy} gets the underlying class and casts to it
     *
     * @param <T>         specific class
     * @param obj         potential proxy to cast
     * @param castToClass class to cast to
     * @return if {@link HibernateProxy} get the underlying class and cast it, if not just cast it
     */
    public static <T> T proxySafeCast(Object obj, Class<T> castToClass) {
        if (obj instanceof HibernateProxy) {

            //noinspection unchecked
            return (T) ((HibernateProxy) obj).getHibernateLazyInitializer().getImplementation();
        }
        //noinspection unchecked
        return (T) obj;
    }

    /**
     * @param obj      object that may be a {@link HibernateProxy}
     * @param subClass subClass of interest
     * @return true if the {@link HibernateProxy}'s underlying class is an instance of the subclass
     */
    public static boolean proxySafeIsInstance(Object obj, Class<?> subClass) {

        if (obj instanceof HibernateProxy) {
            Object implementation = ((HibernateProxy) obj).getHibernateLazyInitializer().getImplementation();
            if (subClass.isInstance(implementation)) {
                return true;
            }
        }
        return subClass.isInstance(obj);
    }
}
