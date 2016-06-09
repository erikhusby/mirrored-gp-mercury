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

import net.sourceforge.stripes.mock.MockHttpSession;
import net.sourceforge.stripes.mock.MockServletContext;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.codehaus.jackson.type.TypeReference;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;


@Test(groups = TestGroups.DATABASE_FREE)
public class SessionCacheTest {
    private static final String NAMESPACE = "NAMESPACE";

    private static final int THREADPOOL_SIZE = 10;
    private static final int INVOCATION_COUNT = 50;
    private static final TypeReference<TestData>
            TEST_DATA_TYPE_REFERENCE = new TypeReference<TestData>() {
    };
    private static final int MAX_CACHE_SIZE = 5;
    private HttpSession session = null;
    private AtomicInteger cacheId;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        cacheId = new AtomicInteger(1);
    }

    private SessionCache<TestData> buildSessionCache(final int maxCacheSize) throws Exception {
        session = new MockHttpSession(new MockServletContext("ctx"));
        return new SessionCache<>(session, NAMESPACE, maxCacheSize, TEST_DATA_TYPE_REFERENCE);
    }

    @Test(threadPoolSize = THREADPOOL_SIZE, alwaysRun = true, invocationCount = INVOCATION_COUNT)
    public void testSingleItemCache() throws Exception {
        SessionCache<TestData> sessionCache = buildSessionCache(MAX_CACHE_SIZE);
        TestData data = new TestData("testSingleItemCache");
        String cacheKey = getCacheKey();
        sessionCache.put(cacheKey, data);
        TestData cachedData = sessionCache.get(cacheKey);
        assertThat(cachedData, equalTo(data));
    }

    @Test(threadPoolSize = THREADPOOL_SIZE, alwaysRun = true, invocationCount = INVOCATION_COUNT)
    public void testReplaceItemInCache() throws Exception {
        SessionCache<TestData> sessionCache = buildSessionCache(MAX_CACHE_SIZE);
        TestData data = new TestData("testReplaceItemInCache");
        TestData differentData = new TestData("Some other testReplaceItemInCache");
        String cacheKey = getCacheKey();
        sessionCache.put(cacheKey, data);
        sessionCache.put(cacheKey, differentData);
        TestData cachedData = sessionCache.get(cacheKey);
        assertThat(cachedData, equalTo(differentData));
    }

    @Test(threadPoolSize = THREADPOOL_SIZE, invocationCount = INVOCATION_COUNT)
    public void testAddSameDataToCache() throws Exception {
        SessionCache<TestData> sessionCache = buildSessionCache(THREADPOOL_SIZE);
        TestData data = new TestData("testAddSameDataToCache");
        String cacheKey = getCacheKey();
        sessionCache.put(cacheKey, data);
        TestData testData = sessionCache.get(cacheKey);
        assertThat(testData, equalTo(data));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testAddToCacheNullNamespaceKey() throws Exception {
        SessionCache<TestData> sessionCache = new SessionCache<>(session, null, TEST_DATA_TYPE_REFERENCE);
        String key = getCacheKey();
        TestData data = new TestData("testAddToCacheNullNamespaceKey");
        sessionCache.put(key, data);
        TestData cachedData = sessionCache.get(key);

        assertThat(cachedData, nullValue());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testAddToCacheNullCacheKey() throws Exception {
        SessionCache<TestData> sessionCache = buildSessionCache(MAX_CACHE_SIZE);

        TestData data = new TestData("testAddToCacheNullNamespaceKey");
        sessionCache.put(null, data);
        TestData cachedData = sessionCache.get(null);

        assertThat(cachedData, nullValue());
    }

    @Test(threadPoolSize = THREADPOOL_SIZE, invocationCount = INVOCATION_COUNT)
    public void testMaxCacheSize() throws Exception {
        String key1 = getCacheKey();
        String key2 = getCacheKey();
        String key3 = getCacheKey();
        int maxSize = 2;
        SessionCache<TestData> sessionCache = buildSessionCache(maxSize);

        String dataName1 = generateDataName("testSingleItemCacheIsSingleItemCache", 1);
        TestData data1 = new TestData(dataName1);
        sessionCache.put(key1, data1);

        String dataName2 = generateDataName("testSingleItemCacheIsSingleItemCache", 2);
        TestData data2 = new TestData(dataName2);
        sessionCache.put(key2, data2);

        String dataName3 = generateDataName("testSingleItemCacheIsSingleItemCache", 3);
        TestData data3 = new TestData(dataName3);
        sessionCache.put(key3, data3);

        assertThat(sessionCache.size(), equalTo(maxSize));
        TestData fetchedData = sessionCache.get(key1);
        assertThat(fetchedData, nullValue());
    }

    private String generateDataName(String name, int itemNumber) {
        return String.format("%s %d", name, itemNumber);
    }

    @Test(threadPoolSize = THREADPOOL_SIZE, invocationCount = INVOCATION_COUNT)
    public void testMultiItemCache() throws Exception {
        SessionCache<TestData> cache = buildSessionCache(MAX_CACHE_SIZE);

        String key1 = getCacheKey();
        TestData data = new TestData("testMultiItemCache 1");
        cache.put(key1, data);

        TestData data2 = new TestData("testMultiItemCache 2");
        String key2 = getCacheKey();
        cache.put(key2, data2);

        TestData cachedData = cache.get(key1);
        assertThat(cachedData, equalTo(data));

        cachedData = cache.get(key2);
        assertThat(cachedData, equalTo(data2));
    }

    @Test(threadPoolSize = THREADPOOL_SIZE, invocationCount = INVOCATION_COUNT)
    public void testMultiItemDeleteFromCache() throws Exception {
        SessionCache<TestData> sessionCache = buildSessionCache(MAX_CACHE_SIZE);
        String key1 = getCacheKey();
        TestData data = new TestData("testMultiItemDeleteFromCache");
        sessionCache.put(key1, data);

        TestData data2 = new TestData("more data");
        String key2 = getCacheKey();
        sessionCache.put(key2, data2);

        sessionCache.remove(key1);
        TestData cachedData = sessionCache.get(key1);
        assertThat(cachedData, nullValue());

        assertThat(sessionCache.isEmpty(), is(false));
    }

    @Test(threadPoolSize = 10, invocationCount = INVOCATION_COUNT)
    public void testDeleteFromCache() throws Exception {
        final String key = getCacheKey();

        SessionCache<TestData> sessionCache = buildSessionCache(MAX_CACHE_SIZE);
        TestData data = new TestData("testDeleteFromCache");
        sessionCache.put(key, data);
        assertThat(sessionCache.isEmpty(), is(false));
        sessionCache.remove(key);
        assertThat(sessionCache.containsKey(key), is(false));
    }

    public void testRemoveAllFromCache() throws Exception {
        SessionCache<TestData> sessionCache = buildSessionCache(MAX_CACHE_SIZE);
        TestData testData = new TestData("testRemoveFromCache");
        String cacheKey = getCacheKey();
        sessionCache.put(cacheKey, testData);
        sessionCache.remove();
        assertThat(sessionCache.isEmpty(), is(true));
    }

    public void testRemoveAllFromCacheAndAdd() throws Exception {
        SessionCache<TestData> sessionCache = buildSessionCache(MAX_CACHE_SIZE);
        TestData testData = new TestData("testRemoveFromCache");
        String cacheKey = getCacheKey();
        sessionCache.put(cacheKey, testData);
        sessionCache.remove();
        sessionCache.put(cacheKey, testData);
        assertThat(sessionCache.size(), is(1));
    }

    private String getCacheKey() {
        int nextId = cacheId.incrementAndGet();
        return String.format("cache_key_%d", nextId);
    }

    public void testCompressData() throws Exception {
        SessionCache<TestData> sessionCache = buildSessionCache(MAX_CACHE_SIZE);
        String stringData = "some string data";
        TestData data = new TestData(stringData);
        byte[] compressed = sessionCache.compress(data);
        assertThat(compressed.length, greaterThan(0));
    }

    public void testUncompressData() throws Exception {
        SessionCache<TestData> sessionCache = buildSessionCache(MAX_CACHE_SIZE);
        String stringData = "some string data";
        TestData testData = new TestData(stringData);
        byte[] compressed = sessionCache.compress(testData);
        TestData fromCache = sessionCache.decompress(compressed);
        assertThat(fromCache, equalTo(testData));
    }

    @SuppressWarnings("unused")
    private static class TestData implements Serializable {
        private static final long serialVersionUID = 6042529638002075037L;
        private Long nanoTime = System.nanoTime();
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

        public Long getNanoTime() {
            return nanoTime;
        }

        public void setNanoTime(Long nanoTime) {
            this.nanoTime = nanoTime;
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
                    .append(getNanoTime(), testData.getNanoTime())
                    .append(getData(), testData.getData())
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(getNanoTime())
                    .append(getData())
                    .toHashCode();
        }
    }
}
