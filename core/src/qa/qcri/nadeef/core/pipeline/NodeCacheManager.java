/*
 * Copyright (C) Qatar Computing Research Institute, 2013.
 * All rights reserved.
 */

package qa.qcri.nadeef.core.pipeline;

import java.util.HashMap;

/**
 * Node Cache manager manages the input/output of the node execution.
 * It is basically a pair-value container.
 */
public class NodeCacheManager {
    private static HashMap<String, Object> cachePool;
    private static HashMap<String, Integer> refPool;

    private static final NodeCacheManager instance = new NodeCacheManager();

    private NodeCacheManager() {
        cachePool = new HashMap<>(0);
        refPool = new HashMap<>(0);
    }

    /**
     * Singleton instance.
     */
    public static NodeCacheManager getInstance() {
        return instance;
    }

    /**
     * Add key-value pair in the container.
     * @param key value key.
     * @param value value.
     */
    public synchronized void put(String key, Object value) {
        if (cachePool.containsKey(key)) {
            throw new IllegalStateException("Invalid key, key already existed in the cache.");
        }

        cachePool.put(key, value);
        refPool.put(key, 1);
    }
    /**
     * Add key-value pair in the container.
     * @param key value key.
     * @param value value.
     * @param lifeCount life time of the value.
     */
    public synchronized void put(String key, Object value, int lifeCount) {
        if (cachePool.containsKey(key)) {
            throw new IllegalStateException("Invalid key, key already existed in the cache.");
        }

        cachePool.put(key, value);
        refPool.put(key, lifeCount);
    }

    /**
     * Get the value from key, without reducing the life count.
     * @param key
     * @return value.
     */
    public synchronized Object tease(String key) {
        if (!cachePool.containsKey(key)) {
            throw new IllegalStateException("Invalid key, key doesn't exist in the cache.");
        }

        return cachePool.get(key);
    }

    /**
     * Get the value from the key. Clean the object from the cache once the life
     * cycle is done.
     * @param key value key.
     * @return value.
     */
    public synchronized Object get(String key) {
        Object result = tease(key);

        // clean the cache once the life is finished, to prevent memory leaking.
        int refCount = refPool.get(key) - 1;
        if (refCount == 0) {
            cachePool.remove(key);
            refPool.remove(key);
        } else {
            refPool.put(key, refCount);
        }

        return result;
    }

    public int getSize() {
        return cachePool.size();
    }
}