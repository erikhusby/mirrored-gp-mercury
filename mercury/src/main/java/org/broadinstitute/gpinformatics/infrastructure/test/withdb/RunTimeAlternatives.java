package org.broadinstitute.gpinformatics.infrastructure.test.withdb;

import java.util.HashMap;
import java.util.Map;

public class RunTimeAlternatives {
    private static ThreadLocal<Map<Class, Object>> threadLocal = null;

    /**
     * Returns the run-time alternative implementation of a class from the thread local map.
     */
    public static <T> T getThreadLocalAlternative(Class<T> superclass) {
        return (threadLocal == null || threadLocal.get() == null) ? null : (T)threadLocal.get().get(superclass);
    }

    /**
     * Sets a run-time alternative implementation of a class in the thread local map.
     */
    public static <T> void addThreadLocalAlternative(Class<T> superclass, T implementation) {
        if (threadLocal == null) {
            threadLocal = new ThreadLocal();
        }
        if (threadLocal.get() == null) {
            threadLocal.set(new HashMap());
        }
        threadLocal.get().put(superclass, implementation);
    }

    /**
     * Removes all run-time alternative implementations from the thread local map.
     */
    public static void clearThreadLocalAlternatives() {
        if (threadLocal != null && threadLocal.get() != null) {
            threadLocal.get().clear();
        }
    }
}
