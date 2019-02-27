/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2016 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.presentation.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import net.sourceforge.stripes.mock.MockHttpSession;
import net.sourceforge.stripes.mock.MockServletContext;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.broadinstitute.gpinformatics.mercury.presentation.cache.SessionCacheTest.INVOCATION_COUNT;
import static org.broadinstitute.gpinformatics.mercury.presentation.cache.SessionCacheTest.THREADPOOL_SIZE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;


@Test(groups = TestGroups.MULTITHREADED, threadPoolSize = THREADPOOL_SIZE, invocationCount = INVOCATION_COUNT)
public class SessionCacheTest {
    static final int THREADPOOL_SIZE = 50;
    static final int INVOCATION_COUNT = 50;
    private static final TypeReference<TestData>
            TEST_DATA_TYPE_REFERENCE = new TypeReference<TestData>() {
    };
    private static final int MAX_CACHE_SIZE = 5;
    private AtomicInteger cacheId;
    private AtomicInteger uniqueId;
    private static final MockServletContext context = new MockServletContext("ctx");
    private MockHttpSession session = null;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        cacheId = new AtomicInteger(1);
        uniqueId = new AtomicInteger(1);
    }

    @BeforeTest
    public void setUp() throws Exception {
        session = new MockHttpSession(context);
    }

    private SessionCache<TestData> buildSessionCache(final int maxCacheSize, String namespace) throws Exception {
        return new SessionCache<>(session, namespace, maxCacheSize, TEST_DATA_TYPE_REFERENCE);
    }

    public void testSingleItemCache() throws Exception {
        SessionCache<TestData> sessionCache = buildSessionCache(MAX_CACHE_SIZE, newNamespace());
        TestData data = new TestData("testSingleItemCache");
        String cacheKey = getCacheKey();
        sessionCache.put(cacheKey, data);
        TestData cachedData = sessionCache.get(cacheKey);
        assertThat(cachedData, equalTo(data));
    }

    public void testLsuBehavior() throws Exception {
        String namespace = newNamespace();
        SessionCache<TestData> sessionCache = buildSessionCache(2, namespace);
        final TestData data1 = new TestData("Data 1");
        final TestData data2 = new TestData("Data 2");
        final TestData data3 = new TestData("Data 3");
        final String cacheKey1 = getCacheKey();
        final String cacheKey2 = getCacheKey();
        final String cacheKey3 = getCacheKey();

        sessionCache.put(cacheKey1, data1);
        sessionCache.put(cacheKey2, data2);
        // Cache order is now 'cacheKey1', 'cacheKey2'

        // getting cacheKey1 should cause it to bubble up to the top of the queue.
        TestData cachedData = sessionCache.get(cacheKey1);
        // The order of the queue should be 'cacheKey2', 'cacheKey1'

        sessionCache.put(cacheKey3, data3);
        // The order of the queue should be 'cacheKey3', 'cacheKey1'
        // 'cacheKey2' should have been ejected since the cache can only hold two items
        cachedData = sessionCache.get(cacheKey2);
        assertThat(cachedData, nullValue(TestData.class));
    }

    public void testReplaceItemInCache() throws Exception {
        String namespace = newNamespace();
        SessionCache<TestData> sessionCache = buildSessionCache(MAX_CACHE_SIZE, namespace);
        final TestData data = new TestData("testReplaceItemInCache");
        final TestData differentData = new TestData("Some other testReplaceItemInCache");
        final String cacheKey = getCacheKey();
        sessionCache.put(cacheKey, data);
        sessionCache.put(cacheKey, differentData);
        TestData cachedData = sessionCache.get(cacheKey);
        assertThat(cachedData, equalTo(differentData));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testAddToCacheNullNamespaceKey() throws Exception {
        new SessionCache<>(session, null, TEST_DATA_TYPE_REFERENCE);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testAddToCacheNullCacheKey() throws Exception {
        SessionCache<TestData> sessionCache = buildSessionCache(MAX_CACHE_SIZE, newNamespace());
        String cacheKey = null;
        TestData data = new TestData("testAddToCacheNullNamespaceKey");
        sessionCache.put(cacheKey, data);
    }

    public void testMaxCacheSize() throws Exception {
        int maxSize = 2;
        String namespace = newNamespace();
        SessionCache<TestData> sessionCache = buildSessionCache(maxSize, namespace);

        String dataName1 = generateDataName("testSingleItemCacheIsSingleItemCache", 1);
        String key1 = addTestDataToCache(sessionCache, dataName1);

        String dataName2 = generateDataName("testSingleItemCacheIsSingleItemCache", 2);
        addTestDataToCache(sessionCache, dataName2);

        String dataName3 = generateDataName("testSingleItemCacheIsSingleItemCache", 3);
        addTestDataToCache(sessionCache, dataName3);

        assertThat(sessionCache.size(), equalTo(maxSize));
        TestData fetchedData = sessionCache.get(key1);
        assertThat(fetchedData, nullValue());
    }

    private String generateDataName(String name, int itemNumber) {
        return String.format("%s %d", name, itemNumber);
    }


    public void testMultiItemCache() throws Exception {
        String key1 = getCacheKey();
        SessionCache<TestData> sessionCache = buildSessionCache(MAX_CACHE_SIZE, newNamespace());
        TestData data = new TestData("testMultiItemCache 1");
        sessionCache.put(key1, data);

        TestData data2 = new TestData("testMultiItemCache 2");
        String key2 = getCacheKey();
        sessionCache.put(key2, data2);

        TestData cachedData = sessionCache.get(key1);
        assertThat(cachedData, equalTo(data));

        cachedData = sessionCache.get(key2);
        assertThat(cachedData, equalTo(data2));
    }


    public void testMultiItemDeleteFromCache() throws Exception {
        SessionCache<TestData> sessionCache = buildSessionCache(MAX_CACHE_SIZE, newNamespace());
        String key1 = addTestDataToCache(sessionCache, "testMultiItemDeleteFromCache");
        addTestDataToCache(sessionCache, "more data");

        sessionCache.remove(key1);
        TestData cachedData = sessionCache.get(key1);
        assertThat(cachedData, nullValue());

        assertThat(sessionCache.isEmpty(), is(false));
    }

    public void testSameDataMultipleNamespace() throws Exception {
        String namespace = newNamespace();
        SessionCache<TestData> cache1 = buildSessionCache(MAX_CACHE_SIZE, namespace);

        namespace = newNamespace();
        SessionCache<TestData> cache2 = buildSessionCache(MAX_CACHE_SIZE, namespace);
        String cacheKey = getCacheKey();
        TestData data1 = new TestData("testSingleKeysMultipleNamespace 1");

        cache1.put(cacheKey, data1);
        assertThat(cache2.get(cacheKey), is(nullValue()));
    }

    private String newNamespace() {
        return String.format("NAMESPACE_%d", uniqueId.incrementAndGet());
    }

    public void testDeleteFromCache() throws Exception {
        SessionCache<TestData> sessionCache = buildSessionCache(MAX_CACHE_SIZE, newNamespace());
        String key = addTestDataToCache(sessionCache, "testDeleteFromCache");
        assertThat(sessionCache.isEmpty(), is(false));
        sessionCache.remove(key);
        assertThat(sessionCache.containsKey(key), is(false));
    }

    private String addTestDataToCache(SessionCache<TestData> sessionCache, String stringData) {
        String cacheKey = getCacheKey();
        TestData data = new TestData(stringData);
        sessionCache.put(cacheKey, data);
        return cacheKey;
    }

    private String getCacheKey() {
        int nextId = cacheId.incrementAndGet();
        return String.format("cache_key_%d", nextId);
    }

    public void testCompressData() throws Exception {
        SessionCache<TestData> sessionCache = buildSessionCache(MAX_CACHE_SIZE, newNamespace());
        String stringData = "some string data";
        TestData data = new TestData(stringData);
        byte[] compressed = sessionCache.compress(data);
        assertThat(compressed.length, greaterThan(0));
    }

    public void testUncompressData() throws Exception {
        SessionCache<TestData> sessionCache = buildSessionCache(MAX_CACHE_SIZE, newNamespace());
        String stringData = "some string data";
        TestData testData = new TestData(stringData);
        byte[] compressed = sessionCache.compress(testData);
        TestData fromCache = sessionCache.decompress(compressed);
        assertThat(fromCache, equalTo(testData));
    }

    @SuppressWarnings("unused")
    private static class TestData implements Serializable {
        private static final long serialVersionUID = 6042529638002075037L;
        private static final Long nanoTime = System.nanoTime();
        private String data;

        public TestData() {
        }

        TestData(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        Long getNanoTime() {
            return nanoTime;
        }

        @Override
        public String toString() {
            return String.format("TestData{data='%s', nanoTime=%d}", data, nanoTime);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TestData testData = (TestData) o;

            return new EqualsBuilder()
                    .append(getNanoTime(), testData.getNanoTime()).append(getData(), testData.getData()).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).append(getNanoTime()).append(getData()).toHashCode();
        }
    }
}
