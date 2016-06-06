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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.xnio.IoUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Class for caching and compressing data in HTTP Sessions.
 * <p>
 * A SessionCache holds a type-safe mapping of key value pairs which are stored in their own namespace.
 * The SessionCache can hold up to maxCacheSize items within a namespace. When new elements are added to the
 * sessionCache when the cache size reaches maxCacheSize, the oldest element is removed. Additionally, the
 * data stored in the cache is compressed.
 *
 * @param <T> Type of data being cached.
 *
 * @see LinkedBlockingQueue
 */
public class SessionCache<T> {
    private final HttpSession session;
    private final String namespace;
    private final int maxCacheSize;
    private final TypeReference<T> objectTypeReference;
    private static ExecutorService executorService = Executors.newCachedThreadPool();

    private final AtomicReference<LinkedBlockingQueue<SimpleEntry<String, byte[]>>> sessionCacheReference;

    private static final int DEFAULT_MAX_CACHE_SIZE = 3;
    private static final int TIMEOUT = 10;

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
        this.maxCacheSize = maxCacheSize;
        this.objectTypeReference = typeReference;

        synchronized (this.session) {
            LinkedBlockingQueue<SimpleEntry<String, byte[]>> cacheItems = new LinkedBlockingQueue<>(maxCacheSize);
            sessionCacheReference = new AtomicReference<>(cacheItems);
            session.setAttribute(namespace, sessionCacheReference);
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
     * @param sessionKey the identifier the data is keyed off of.
     *
     * @return Item retrieved by cache, or null if it isn't in the cache. This is true even in cases where
     * objectTypeReference is a Collection.
     */
    @Nullable
    public T get(@Nonnull String sessionKey) {
        SimpleEntry<String, byte[]> sessionCache = getEntry(sessionKey);
        if (sessionCache != null) {
            byte[] compressed = sessionCache.getValue();
            if (ArrayUtils.isNotEmpty(compressed)) {
                log.debug(String.format("cache hit for %s in %s", sessionKey, this.namespace));
                try {
                    return decompress(compressed);
                } catch (IOException e) {
                    throw new SessionCacheException("Could not retrieve item from cache", e);
                }
            }
        }

        return null;
    }

    /**
     * Add data identified by key to the cache.
     */
    public void put(@Nonnull final String key, @Nonnull T data) {
        try {
            byte[] compressed = compress(data);
            if (compressed != null) {
                final SimpleEntry<String, byte[]> cacheItem = new SimpleEntry<>(key, compressed);
                Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            LinkedBlockingQueue<SimpleEntry<String, byte[]>> oldValue = sessionCacheReference.get();
                            LinkedBlockingQueue<SimpleEntry<String, byte[]>> newValue = newQueue(oldValue);
                            if (newValue.size() == maxCacheSize && !newValue.isEmpty()) {
                                newValue.remove();
                            }
                            if (!newValue.offer(cacheItem)) {
                                log.debug(String.format("%s wasn't added to the cache.", cacheItem));
                            } else {
                                log.debug(String.format("%s added to the cache", cacheItem));
                            }
                            if (sessionCacheReference.compareAndSet(oldValue, newValue)) {
                                break;
                            }
                        }
                    }
                };
                executeTask(task);
                log.info(String.format("%s Data added to %s cache. Cache size now: %d", key, this.namespace, size()));
            }
        } catch (IOException e) {
            throw new SessionCacheException(String.format("Could not serialize sampleData in %s", this.namespace), e);
        }
    }


    /**
     * Add data identified by key to the cache.
     */
    public void replace(@Nonnull final String key, @Nonnull T data) {
        remove(key);
        put(key, data);
    }

    /**
     * Remove all items from the cache. The namespace attribute on the session is not removed.
     */
    public void remove() {
        log.debug(String.format("Removing all items from cache namespace %s", namespace));
        Runnable task = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (sessionCacheReference.compareAndSet(sessionCacheReference.get(), null)) {
                        break;
                    }
                }
            }
        };

        executeTask(task);
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
        Queue<SimpleEntry<String, byte[]>> cacheItems = sessionCacheReference.get();
        int size = 0;
        if (cacheItems != null) {
            size = cacheItems.size();
        }
        return size;
    }

    /**
     * @return true if the cache contains element with specified key, or false if it doesn't.
     */
    public boolean containsKey(String key) {
        return get(key) != null;
    }

    /**
     * Remove item with cacheKey from the cache. The namespace attribute on the session is not removed.
     */
    public void remove(final String cacheKey) {
        log.info(String.format("Removing %s items from cache namespace %s", cacheKey, namespace));
        final Runnable task = removeTask(cacheKey);
        executeTask(task);
    }

    private Runnable removeTask(final String cacheKey) {
        return new Runnable() {
            @Override
            public void run() {
                while (true) {
                    LinkedBlockingQueue<SimpleEntry<String, byte[]>> oldValue = sessionCacheReference.get();
                    LinkedBlockingQueue<SimpleEntry<String, byte[]>> cacheItems = newQueue(oldValue);

                    for (SimpleEntry<String, byte[]> cacheItem : cacheItems) {
                        if (cacheItem.getKey().equals(cacheKey)) {
                            cacheItems.remove();
                        }
                    }

                    if (sessionCacheReference.compareAndSet(sessionCacheReference.get(), cacheItems)) {
                        break;
                    }
                }
            }
        };
    }

    private SimpleEntry<String, byte[]> getEntry(@Nonnull String cacheKey) {
        //noinspection ConstantConditions
        if (cacheKey == null) {
            throw new IllegalArgumentException("Required Parameter 'cacheKey' missing");
        }
        return executeTask(getTask(cacheKey));
    }

    private Callable<SimpleEntry<String, byte[]>> getTask(final String cacheKey) {
        return new Callable<SimpleEntry<String, byte[]>>() {
            @Override
            public SimpleEntry<String, byte[]> call() {
                Queue<SimpleEntry<String, byte[]>> cacheQueue = sessionCacheReference.get();

                for (SimpleEntry<String, byte[]> cacheItem : cacheQueue) {
                    if (cacheItem.getKey().equals(cacheKey)) {
                        return cacheItem;
                    }
                }
                return null;
            }
        };
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
                IoUtils.safeClose(objectOut);
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


    /**
     * Create a new Queue initialized with cacheItems.
     */
    private LinkedBlockingQueue<SimpleEntry<String, byte[]>> newQueue(Queue<SimpleEntry<String, byte[]>> cacheItems) {
        LinkedBlockingQueue<SimpleEntry<String, byte[]>> queue = new LinkedBlockingQueue<>(maxCacheSize);
        if (cacheItems != null) {
            queue.addAll(cacheItems);
        }
        return queue;
    }

    /**
     * Execute Callable task
     *
     * @see SessionCache#TIMEOUT
     * @see TimeUnit#SECONDS
     */
    private SimpleEntry<String, byte[]> executeTask(Callable<SimpleEntry<String, byte[]>> task) {
        Future<SimpleEntry<String, byte[]>> future = executorService.submit(task);
        try {
            return future.get(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error(e);
        }
        return null;
    }

    /**
     * Execute Runnable task.
     *
     * @see SessionCache#TIMEOUT
     * @see TimeUnit#SECONDS
     */
    private void executeTask(Runnable task) {
        Future future = executorService.submit(task);
        try {
            future.get(TIMEOUT, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error(e);
        }
    }

}
