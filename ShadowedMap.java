package com.ibm.utm.informix.server.database.datatypes.udt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 
 * ShadowedMap
 * 
 * This class creates a hash-map that 'shadows' an underlying map.
 * 
 * Changes to this map do not propagate to the underlying map being shadowed.
 * 
 * Effectively this map is copy-on-write with 2 primary differences:
 *   1) We don't copy all of the elements on creation.
 *   2) We lazily delay instantiation of the shadow map to save memory
 *      (i.e. we never make it at all if we never do a write).
 * 
 * @author jxkb
 *
 * @param <K>
 * @param <V>
 */

public class ShadowedMap<K, V> implements Map<K, V>, Cloneable, Serializable {

	private static final long serialVersionUID = -6570879934261756848L;
	private final Integer initialCapacity;
	private Map<K, V> shadowedMap;
	private Map<K, V> underlyingMap;
	
	public class MapEntry<K, V> implements Map.Entry<K, V> {
	
		private final K key;
		private V value;
		
		public MapEntry(K key, V value) {
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
			V tmpValue = value;
			this.value = value;
			return tmpValue;
		}
		
	}
	
	public ShadowedMap(Map<K, V> m) {
		this.initialCapacity = 4;
		this.underlyingMap = m;
	}
	
	public ShadowedMap(Map<K, V> m, int initialCapacity) {
		this.initialCapacity = initialCapacity;
		this.underlyingMap = m;
	}
	
	@Override
	public V put(K key, V value) {
		if (shadowedMap == null) {
			shadowedMap = new HashMap<K,V>(initialCapacity);
			shadowedMap.put(key, value);
			return underlyingMap.get(key);
		}
				
		return shadowedMap.put(key, value);
	}
	
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		if (shadowedMap == null) {
			shadowedMap = new HashMap<K,V>(initialCapacity);
		}
		
		shadowedMap.putAll(m);
	}
	
	@Override
	public V get(Object key) {
		if (shadowedMap == null) {
			return underlyingMap.get(key);
		}
		else if (shadowedMap.containsKey(key)) {
			return shadowedMap.get(key);
		}
		else {
			return underlyingMap.get(key);
		}
	}
	
	@Override
	public void clear() {
		if (shadowedMap != null) {
			shadowedMap.clear();
		}
	}
	
	@Override
	public boolean containsKey(Object key) {
		if (shadowedMap != null) {
			return (shadowedMap.containsKey(key) || underlyingMap.containsKey(key));
		}
		
		return underlyingMap.containsKey(key);
	}
	
	@Override
	public boolean containsValue(Object value) {
		if (shadowedMap != null) {
			return (shadowedMap.containsValue(value)|| underlyingMap.containsValue(value));
		}

		return underlyingMap.containsValue(value);
	}
	
	@Override
	public boolean isEmpty() {
		if (shadowedMap != null) {
			return (shadowedMap.isEmpty() && underlyingMap.isEmpty());
		}

		return underlyingMap.isEmpty();
	}
	
	@Override
	public int size() {
		int count = underlyingMap.size();
		if (shadowedMap != null) {
			for (K key : shadowedMap.keySet()) {
				if (!underlyingMap.containsKey(key)) {
					count++;
				}
			}
		}
				
		return count;
	}
	
	@Override
	public V remove(Object key) {
		if (shadowedMap != null) {
			return shadowedMap.remove(key);
		}
		
		return underlyingMap.get(key);
	}
	
	@Override
	public Set<K> keySet() {
		if (shadowedMap != null) {
			Set<K> ret = new HashSet<K>(shadowedMap.size()+underlyingMap.size());
			ret.addAll(shadowedMap.keySet());
			ret.addAll(underlyingMap.keySet());
			return ret;
		}
		
		return underlyingMap.keySet();
	}
	
	@Override
	public Collection<V> values() {
		if (shadowedMap != null) {
			Collection<V> ret = new ArrayList<V>(shadowedMap.size()+underlyingMap.size());
			ret.addAll(shadowedMap.values());
			ret.addAll(underlyingMap.values());
			return ret;
		}
		
		return underlyingMap.values();
	}
	
	public String toString() {
		if (shadowedMap != null) {
			StringBuilder sb = new StringBuilder("{");
			for (Map.Entry<K, V> entry : underlyingMap.entrySet()) {
				K key = entry.getKey();
				sb.append(key).append("=");
				if (shadowedMap.containsKey(key)) {
					sb.append(shadowedMap.get(key)).append(", ");
				}
				else {
					V value = entry.getValue();
					sb.append(value).append(", ");
				}
			}
			for (Map.Entry<K, V> entry : shadowedMap.entrySet()) {
				K key = entry.getKey();
				V value = entry.getValue();		
				if (!underlyingMap.containsKey(key)) {
					sb.append(key).append("=").append(value).append(", ");
				}
			}
			sb.setLength(sb.length()-2);
			sb.append("}");
			
			return sb.toString();
		}
		
		return super.toString();
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		if (shadowedMap != null) {
			Set<Map.Entry<K, V>> ret = new HashSet<Map.Entry<K, V>>();
			for (Map.Entry<K, V> entry : underlyingMap.entrySet()) {
				K key = entry.getKey();
				if (shadowedMap.containsKey(key)) {
					ret.add(new MapEntry<K, V>(key, shadowedMap.get(key)));
				}
				else {
					V value = entry.getValue();
					ret.add(new MapEntry<K, V>(key, value));
				}
			}
			for (Map.Entry<K, V> entry : shadowedMap.entrySet()) {
				K key = entry.getKey();
				V value = entry.getValue();		
				if (!underlyingMap.containsKey(key)) {
					ret.add(new MapEntry<K, V>(key, value));
				}
			}
			
			return ret;
		}
		
		return underlyingMap.entrySet();
	}

}
