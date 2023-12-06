package org.patonki.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ArrayMap<K,V> implements Map<K,V> {
    private static class KeyValue<K,V> implements Entry<K,V>{
        private final K key;
        private V value;

        public KeyValue(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            var old = this.value;
            this.value = value;
            return old;
        }
    }
    private final ArrayList<KeyValue<K,V>> values = new ArrayList<>();

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        for (KeyValue<K, V> value : values) {
            if (value.key.equals(key)) return true;
        }
        return false;
    }
    @Nullable
    @Override
    public V put(K key, V value) {
        for (KeyValue<K, V> kvKeyValue : values) {
            if (kvKeyValue.key == key) {
                V oldValue = kvKeyValue.value;
                kvKeyValue.value = value;
                return oldValue;
            }
        }
        values.add(new KeyValue<>(key, value));
        return null;
    }

    @Override
    public boolean containsValue(Object value) {
        for (KeyValue<K, V> val : values) {
            if (val.value.equals(value)) return true;
        }
        return false;
    }

    @Override
    public V get(Object key) {
        for (KeyValue<K, V> value : values) {
            if (value.key.equals(key)) return value.value;
        }
        return null;
    }



    @Override
    public V remove(Object key) {
        for (int i = 0; i < values.size(); i++) {
            KeyValue<K, V> value = values.get(i);
            if (value.key.equals(key)) {
                return values.remove(i).value;
            }
        }
        return null;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        for (K key : m.keySet()) {
            put(key, m.get(key));
        }
    }

    @Override
    public void clear() {
        this.values.clear();
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return values.stream().map(kv -> kv.key).collect(Collectors.toSet());
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return values.stream().map(kv -> kv.value).collect(Collectors.toSet());
    }
    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return new HashSet<>(values);
    }
}
