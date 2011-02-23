package com.atlassian.labs.speakeasy.commonjs.util;

import java.util.*;

/**
 *
 */
public class IterableTreeMap<K, V> extends TreeMap<K, V> implements Iterable<V>
{
    public IterableTreeMap()
    {
    }

    public IterableTreeMap(Comparator<? super K> comparator)
    {
        super(comparator);
    }

    public IterableTreeMap(Map<? extends K, ? extends V> m)
    {
        super(m);
    }

    public IterableTreeMap(SortedMap<K, ? extends V> m)
    {
        super(m);
    }

    public Iterator<V> iterator()
    {
        return values().iterator();
    }
}
