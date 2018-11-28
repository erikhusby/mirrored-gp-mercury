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

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Class for caching and compressing data in HTTP Sessions.
 * <p>
 * A SessionCache holds a type-safe mapping of cacheKey value pairs which are stored in their own namespace.
 * The SessionCache can hold up to maxCacheSize items within a namespace. When new elements are added to the
 * sessionCache and the cache size reaches maxCacheSize, the oldest element is removed. Additionally, the
 * data stored in the cache is compressed.
 *
 * @param <T> Type of data being cached.
 *
 * @see LinkedBlockingQueue
 */
public class SessionCache<T> {
    private final HttpSession session;
    private final String namespace;
    private final TypeReference<T> objectTypeReference;
    private final CircularFifoQueue<SimpleEntry<String, byte[]>> sessionQueue;

    private static final int DEFAULT_MAX_CACHE_SIZE = 3;
    private static final Log log = LogFactory.getLog(SessionCache.class);
    /**
     * Construct a new SessionCache
     *
     * @param session       The HttpSession in which data will be stored.
     * @param namespace     Namespace to write the cache to.
     * @param maxCacheSize  The maximum size (keys) a cache can grow to.
     * @param typeReference The TypeReference of the stored data. Used to pass full generics type information,
     *                      and avoid problems with type erasure.
     *
     * @see TypeReference
     */
    @SuppressWarnings("ConstantConditions")
    public SessionCache(@Nonnull HttpSession session, @Nonnull String namespace, int maxCacheSize,
                        @Nonnull TypeReference<T> typeReference) {
        if (session == null || namespace == null || typeReference == null) {
            throw new IllegalArgumentException("Required parameter missing.");
        }
        this.session = session;
        this.namespace = namespace;
        this.objectTypeReference = typeReference;

            synchronized (this.session) {
                @SuppressWarnings("unchecked")
                CircularFifoQueue<SimpleEntry<String, byte[]>> sessionAttribute =
                        (CircularFifoQueue<SimpleEntry<String, byte[]>>) session.getAttribute(namespace);
                if (sessionAttribute == null) {
                    sessionQueue = new CircularFifoQueue<>(maxCacheSize);
                    session.setAttribute(namespace, sessionQueue);
                } else {
                    sessionQueue = sessionAttribute;
                }
            }

        if (sessionQueue == null) {
            throw new SessionCacheException(String.format("Could not initialize cache in namespace '%s'", namespace));
        }
    }

    /**
     * Construct a new SessionCache using the default maxCacheSize.
     *
     * @param session             The HttpSession which data will be stored.
     * @param namespace           Namespace to write the cache to.
     * @param objectTypeReference TypeReference of the stored data.
     *
     * @see SessionCache#DEFAULT_MAX_CACHE_SIZE
     * @see TypeReference
     */
    public SessionCache(@Nonnull HttpSession session, @Nonnull String namespace,
                        @Nonnull TypeReference<T> objectTypeReference) {
        this(session, namespace, DEFAULT_MAX_CACHE_SIZE, objectTypeReference);
    }


    /**
     * Retrieve data from session.
     *
     * @param cacheKey the identifier the data is keyed off of.
     *
     * @return Item retrieved by cache, or null if it isn't in the cache. This is true even in cases where
     * objectTypeReference is a Collection.
     */
    @Nullable
    public T get(@Nonnull String cacheKey) {
        if (cacheKey == null) {
            throw new IllegalArgumentException(String.format("Cache key is null in '%s'", namespace));
        }
        byte[] compressed = null;
        synchronized (sessionQueue) {
            SimpleEntry<String, byte[]> cachedEntry=null;
            for (SimpleEntry<String, byte[]> cacheItem : sessionQueue) {
                if (cacheItem.getKey().equals(cacheKey)) {
                    compressed = cacheItem.getValue();
                    cachedEntry = sessionQueue.poll();
                    break;
                }
            }
            if (cachedEntry != null) {
                sessionQueue.add(cachedEntry);
            }
        }
        if (ArrayUtils.isNotEmpty(compressed)) {
            log.info(String.format("Cache hit for '%s' in '%s'", cacheKey, this.namespace));
            try {
                return decompress(compressed);
            } catch (IOException e) {
                throw new SessionCacheException(String.format("Could not retrieve item '%s' from cache", cacheKey), e);
            }
        }

        return null;
    }

    /**
     * Add data identified by cacheKey to the cache.
     */
    public void put(@Nonnull final String cacheKey, @Nonnull T data) {
        if (cacheKey == null) {
            throw new IllegalArgumentException(String.format("Cache cacheKey is null in '%s'", namespace));
        }
        byte[] compressed;
        try {
            compressed = compress(data);
        } catch (IOException e) {
            String error = String.format("Could not serialize sampleData in '%s'", namespace);
            throw new SessionCacheException(error, e);
        }
        synchronized (sessionQueue) {
            if (compressed != null) {
                final SimpleEntry<String, byte[]> cacheItem = new SimpleEntry<>(cacheKey, compressed);
                boolean removed = false;
                SimpleEntry<String, byte[]> cacheEntry = getCacheEntry(cacheKey);
                if (cacheEntry!=null) {
                    removed = sessionQueue.remove(cacheEntry);
                }
                sessionQueue.add(cacheItem);
                String verb = removed ? "Replacing" : "Adding";
                log.debug(
                        String.format("%s '%s' to '%s' cache. Cache size now: %d", verb, cacheKey, namespace, size()));
            }
        }
    }

    private SimpleEntry<String, byte[]> getCacheEntry(String cacheKey) {
        for (SimpleEntry<String, byte[]> cacheEntry : sessionQueue) {
            if (cacheEntry.getKey().equals(cacheKey)) {
                return cacheEntry;
            }
        }
        return null;
    }

    /**
     * @return true if the cache is empty, or false if the cache is not empty.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * @return the current number of elements in the cache.
     */
    public int size() {
        return sessionQueue.size();
    }

    /**
     * @return true if the cache contains element with specified cacheKey, or false if it doesn't.
     */
    public boolean containsKey(String cacheKey) {
        return get(cacheKey) != null;
    }

    /**
     * Remove item with cacheKey from the cache. The namespace attribute on the session is not removed.
     */
    public boolean remove(final String cacheKey) {
        synchronized (sessionQueue) {
            boolean removed=false;
            log.debug(String.format("Removing %s from cache namespace %s", cacheKey, namespace));
            for (SimpleEntry<String, byte[]> cacheItem : sessionQueue) {
                if (cacheItem.getKey().equals(cacheKey)) {
                    removed = sessionQueue.remove(cacheItem);
                    String negateValue = removed ? "removed" : "not removed";
                    log.debug(String.format("%s was %s from cache namespace %s", cacheKey, negateValue,namespace));
                }
            }
            return removed;
        }
    }

    /**
     * Compress input data using GZIP.
     *
     * @throws IOException
     */
    byte[] compress(T data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzipOut = null;
        ObjectOutputStream objectOut = null;
        try {
            String jsonBean = toJson(data);

            gzipOut = new GZIPOutputStream(baos);
            objectOut = new ObjectOutputStream(gzipOut);
            objectOut.writeObject(jsonBean);
        } catch (IOException e) {
            log.error("Error compressing data", e);
        } finally {
            if (objectOut != null) {
                IOUtils.closeQuietly(objectOut);
            }
        }
        return baos.toByteArray();
    }

    /**
     * Decompress GZIP compressed input data.
     *
     * @throws IOException
     */
    T decompress(byte[] bytes) throws IOException {
        String jsonData = null;
        try {

            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            GZIPInputStream gzipIn = new GZIPInputStream(bais);
            ObjectInputStream objectIn = new ObjectInputStream(gzipIn);
            jsonData = (String) objectIn.readObject();
        } catch (ClassNotFoundException e) {
            log.error("Error decompressing data.", e);
        }
        return fromJson(jsonData);
    }

    /**
     * Convert json String to Object
     */
    private T fromJson(String jsonData) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonData, objectTypeReference);
    }

    /**
     * Convert input data to json String.
     */
    private String toJson(T data) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(data);
    }
}
