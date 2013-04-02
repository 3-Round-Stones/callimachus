package org.callimachusproject.util;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A {@link Map} implementation that can also return the closest value.
 * 
 * @see #getClosest(String)
 */
public class PrefixMap<V> extends AbstractMap<String, V> implements Cloneable,
		Serializable {
	private static final long serialVersionUID = -3196352714861883183L;

	private final TreeMap<String, V> map;

	public PrefixMap() {
		this.map = new TreeMap<String, V>();
	}

	public PrefixMap(Map<String, V> map) {
		this.map = new TreeMap<String, V>(map);
	}

	/**
	 * Returns the value of the longest key entry which the specified string
	 * starts with, or {@code null} if this map contains no prefix mapping for
	 * the specified string.
	 * 
	 * <p>
	 * More formally, if this map contains a mapping from a key {@code k} to a
	 * value {@code v} such that {@code string}
	 * {@link String#startsWith(String)} {@code k}, then this method returns
	 * {@code v}; otherwise it returns {@code null}. Where {@code k} has the
	 * highest {@link String#length()} value of the matching {@code k}.
	 * 
	 * @throws ClassCastException
	 *             if the specified key cannot be compared with the keys
	 *             currently in the map
	 * @throws NullPointerException
	 *             if the specified string is null
	 */
	public V getClosest(String string) {
		Entry<String, V> entry = getClosestEntry(string);
		if (entry == null)
			return null;
		return entry.getValue();
	}

	/**
	 * Returns the entry with the longest key which the specified string starts
	 * with, or {@code null} if this map contains no prefix mapping for the
	 * specified string.
	 * 
	 * <p>
	 * More formally, if this map contains a mapping from a key {@code k} to a
	 * value {@code v} such that {@code string}
	 * {@link String#startsWith(String)} {@code k}, then this method returns
	 * {@code k} and {@code v}; otherwise it returns {@code null}. Where
	 * {@code k} has the highest {@link String#length()} value of the matching
	 * {@code k}.
	 * 
	 * @throws ClassCastException
	 *             if the specified key cannot be compared with the keys
	 *             currently in the map
	 * @throws NullPointerException
	 *             if the specified string is null
	 */
	public Entry<String, V> getClosestEntry(String string) {
		Entry<String, V> entry = map.floorEntry(string);
		if (entry == null)
			return null;
		String key = entry.getKey();
		if (string == key || string.startsWith(key))
			return entry;
		if (string == null || string.length() == 0)
			return null;
		int idx = 0;
		while (idx < string.length() && idx < key.length()
				&& string.charAt(idx) == key.charAt(idx)) {
			idx++;
		}
		return getClosestEntry(string.substring(0, idx));
	}

	public V get(Object key) {
		return map.get(key);
	}

	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	public int size() {
		return map.size();
	}

	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	public void putAll(Map<? extends String, ? extends V> m) {
		map.putAll(m);
	}

	public V put(String key, V value) {
		return map.put(key, value);
	}

	public V remove(Object key) {
		return map.remove(key);
	}

	public void clear() {
		map.clear();
	}

	public PrefixMap<V> clone() {
		return new PrefixMap<V>(map);
	}

	public Set<String> keySet() {
		return map.keySet();
	}

	public Set<java.util.Map.Entry<String, V>> entrySet() {
		return map.entrySet();
	}

}
