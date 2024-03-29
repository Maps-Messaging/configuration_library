/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.configuration;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import static io.mapsmessaging.logging.LogMessages.PROPERTY_MANAGER_ENTRY_LOOKUP;
import static io.mapsmessaging.logging.LogMessages.PROPERTY_MANAGER_ENTRY_LOOKUP_FAILED;

public class ConfigurationProperties {

  private final Logger logger = LoggerFactory.getLogger(ConfigurationProperties.class);
  private final Map<String, Object> map;
  @Getter
  @Setter
  private String source;
  @Setter
  private ConfigurationProperties global;

  public ConfigurationProperties() {
    super();
    map = new LinkedHashMap<>();
  }

  public ConfigurationProperties(Map<String, Object> inMap) {
    map = new LinkedHashMap<>();
    putAll(inMap);

    Object globalObject = inMap.get("global");
    if (globalObject instanceof ConfigurationProperties) {
      this.global = (ConfigurationProperties) globalObject;
    }
  }

  public Object get(String key) {
    Object val = map.get(key);
    if (val == null && global != null) {
      val = global.get(key);
    }
    if (val instanceof JSONObject) {
      return new ConfigurationProperties(((JSONObject) val).toMap());
    }
    return val;
  }

  public String getProperty(String key) {
    return getProperty(key, null);
  }

  public String getProperty(String key, String defaultValue) {
    Object val = get(key, defaultValue);
    if (val != null) {
      String response = val.toString();
      String check = key.toLowerCase();
      if (check.contains("file") ||
          check.contains("directory") ||
          check.contains("path")
      ) {
        response = EnvironmentConfig.getInstance().translatePath(response);
      }
      response = EnvironmentConfig.getInstance().scanForNonStandardSub(response);
      return response;
    }
    return null;
  }

  public boolean getBooleanProperty(String key, boolean defaultValue) {
    return asBoolean(get(key, defaultValue));
  }

  public long getLongProperty(String key, long defaultValue) {
    try {
      return asLong(get(key, defaultValue));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public int getIntProperty(String key, int defaultValue) {
    try {
      return (int) asLong(get(key, defaultValue));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public float getFloatProperty(String key, float defaultValue) {
    try {
      return (float) asDouble(get(key, defaultValue));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public double getDoubleProperty(String key, double defaultValue) {
    try {
      return asDouble(get(key, defaultValue));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private Object get(String key, Object defaultValue) {
    Object val = get(key);
    if (val != null) {
      logger.log(PROPERTY_MANAGER_ENTRY_LOOKUP, key, val, "Main");
    } else if (global != null) {
      val = global.get(key);
      logger.log(PROPERTY_MANAGER_ENTRY_LOOKUP, key, val, "Global");
    }

    if (val != null) {
      return val;
    }
    logger.log(PROPERTY_MANAGER_ENTRY_LOOKUP_FAILED, key, defaultValue);
    return defaultValue;
  }

  private boolean asBoolean(Object value) {
    if (value instanceof Boolean) {
      return (Boolean) value;
    } else if (value instanceof String) {
      if (((String) value).equalsIgnoreCase("enable")) {
        return true;
      }
      if (((String) value).equalsIgnoreCase("disable")) {
        return false;
      }
      return Boolean.parseBoolean(((String) value).trim());
    }
    return false;
  }

  private long asLong(Object entry) {
    if (entry instanceof Number) {
      if (entry instanceof Float) {
        return Math.round((float) entry);
      }
      if (entry instanceof Double) {
        return Math.round((double) entry);
      }
      return ((Number) entry).longValue();
    } else if (entry instanceof String) {
      String value = ((String) entry).trim();
      if (value.contains(".")) {
        double d = asDouble(value);
        return Math.round(d);
      }

      long val = parseTime(value);
      if (val != Long.MAX_VALUE) {
        return val;
      }

      long multiplier = computeMultiplier(value);
      if (multiplier > 1) {
        value = value.substring(0, value.length() - 1);
      }

      return (Long.parseLong(value) * multiplier);
    }
    throw new NumberFormatException("Unknown number format detected [" + entry + "]");
  }

  private long computeMultiplier(String value) {
    long multiplier = 1L;
    String end = value.substring(value.length() - 1);
    if (end.equalsIgnoreCase("T")) {
      multiplier = 1024L * 1024L * 1024L * 1024L;
    } else if (end.equalsIgnoreCase("G")) {
      multiplier = 1024L * 1024L * 1024L;
    } else if (end.equalsIgnoreCase("M")) {
      multiplier = 1024L * 1024L;
    } else if (end.equalsIgnoreCase("K")) {
      multiplier = 1024;
    }
    return multiplier;
  }

  private long parseTime(String value) {
    long val = Long.MAX_VALUE;
    if (value.equalsIgnoreCase("weekly")) {
      val = TimeUnit.DAYS.toMillis(7);
    } else if (value.equalsIgnoreCase("daily")) {
      val = TimeUnit.DAYS.toMillis(1);
    } else if (value.equalsIgnoreCase("hourly")) {
      val = TimeUnit.HOURS.toMillis(1);
    }
    return val;
  }

  private double asDouble(Object entry) {
    if (entry instanceof Number) {
      return ((Number) entry).doubleValue();
    } else if (entry instanceof String) {
      return Double.parseDouble(((String) entry).trim());
    }
    throw new NumberFormatException("Unknown number format detected [" + entry + "]");
  }

  public boolean containsKey(String key) {
    if (!map.containsKey(key)) {
      if (global != null) {
        return global.containsKey(key);
      }
      return false;
    }
    return true;
  }

  public ConfigurationProperties getGlobal() {
    if (global == null) {
      return (ConfigurationProperties) get("global");
    }
    return global;
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof ConfigurationProperties) {
      boolean listEquals = super.equals(object);
      if (global != null) {
        return listEquals && global.equals(((ConfigurationProperties) object).global);
      }
      return listEquals;
    }
    return false;
  }

  @Override
  public int hashCode() {
    if (global != null) {
      return super.hashCode() + global.hashCode();
    }
    return super.hashCode();
  }

  @Override
  public String toString() {
    if (global != null) {
      return super.toString() + " " + global.toString();
    }
    return super.toString();
  }

  public boolean isEmpty() {
    return map.isEmpty();
  }

  public Set<Entry<String, Object>> entrySet() {
    return map.entrySet();
  }

  public Collection<Object> values() {
    return map.values();
  }

  public Set<String> keySet() {
    return map.keySet();
  }

  public void put(String key, Object val) {
    if (val instanceof Map) {
      ConfigurationProperties props = new ConfigurationProperties((Map<String, Object>) val);
      props.setGlobal(global);
      map.put(key, props);
    } else if (val instanceof List) {
      List<Object> parsedList = new ArrayList<>();
      for (Object list : (List<Object>) val) {
        if (list instanceof Map) {
          ConfigurationProperties props = new ConfigurationProperties((Map<String, Object>) list);
          props.setGlobal(global);
          parsedList.add(props);
        }
      }
      map.put(key, parsedList);
    } else {
      map.put(key, val);
    }
  }

  public void putAll(Map<String, Object> copy) {
    for (Map.Entry<String, Object> entry : copy.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  public void clear() {
    map.clear();
  }

  public int size() {
    return map.size();
  }

  public Map<String, Object> getMap() {
    return packMap(map);
  }

  private Map<String, Object> packMap(Map<String, Object> map) {
    Map<String, Object> response = new LinkedHashMap<>();
    for (Entry<String, Object> entry : map.entrySet()) {
      if (entry.getValue() instanceof ConfigurationProperties) {
        response.put(entry.getKey(), packMap(((ConfigurationProperties) entry.getValue()).map));
      } else if (entry.getValue() instanceof Map) {
        response.put(entry.getKey(), packMap((Map<String, Object>) entry.getValue()));
      } else if (entry.getValue() instanceof List) {
        List<Object> list = (List<Object>) entry.getValue();
        List<Object> replacement = new ArrayList<>();
        for (Object obj : list) {
          if (obj instanceof Map) {
            replacement.add(packMap((Map<String, Object>) obj));
          } else if (obj instanceof ConfigurationProperties) {
            replacement.add(packMap(((ConfigurationProperties) obj).getMap()));
          } else {
            replacement.add(obj);
          }
        }
        response.put(entry.getKey(), replacement);
      } else {
        response.put(entry.getKey(), entry.getValue());
      }
    }
    return response;
  }

}
