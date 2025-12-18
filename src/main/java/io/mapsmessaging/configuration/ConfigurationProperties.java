/*
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.configuration;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import static io.mapsmessaging.logging.ConfigLogMessages.PROPERTY_SMART_QUOTES_DETECTED;
import static io.mapsmessaging.logging.LogMessages.PROPERTY_MANAGER_ENTRY_LOOKUP;
import static io.mapsmessaging.logging.LogMessages.PROPERTY_MANAGER_ENTRY_LOOKUP_FAILED;

@SuppressWarnings("java:S3740")
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
    if (globalObject instanceof ConfigurationProperties globConf) {
      this.global = globConf;
    }
  }

  public Object get(String key) {
    Object val = map.get(key);
    if (val == null && global != null) {
      val = global.get(key);
    }
    if (val instanceof JsonObject jsonObject) {
      Type type = new TypeToken<Map<String, Object>>() {}.getType();
      Map<String, Object> map1 = SystemProperties.getInstance().getGson().fromJson(jsonObject, type);
      return new ConfigurationProperties(map1);
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

  public int getThreadCount(String key, int defaultValue) {
    String value = getProperty(key, String.valueOf(defaultValue)).trim();

    if (value.toLowerCase().contains("{processors}")) {
      int threads = Runtime.getRuntime().availableProcessors();

      int plus = value.lastIndexOf('+');
      int minus = value.lastIndexOf('-');
      int mult = value.lastIndexOf('*');
      int div = value.lastIndexOf('/');

      int operatorIndex = Math.max(Math.max(plus, minus), Math.max(mult, div));

      if (operatorIndex > -1 && operatorIndex < value.length() - 1) {
        char operator = value.charAt(operatorIndex);
        int operand = Integer.parseInt(value.substring(operatorIndex + 1).trim());

        switch (operator) {
          case '/': threads = threads / operand; break;
          case '*': threads = threads * operand; break;
          case '+': threads = threads + operand; break;
          case '-': threads = threads - operand; break;
          default:  break;
        }
      }

      if (threads < 1) {
        threads = 1;
      }
      return threads;
    }

    int dot = value.indexOf('.');
    if (dot >= 0) {
      value = value.substring(0, dot).trim();
    }
    return Integer.parseInt(value);
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
      if(val instanceof String string && string.startsWith("“") && string.endsWith("”")) {
        String fix = string.substring(1, string.length() - 1);
        logger.log(PROPERTY_SMART_QUOTES_DETECTED, key, string, fix);
        val = fix;
      }
      return val;
    }
    logger.log(PROPERTY_MANAGER_ENTRY_LOOKUP_FAILED, key, defaultValue);
    return defaultValue;
  }

  private boolean asBoolean(Object value) {
    if (value instanceof Boolean b) {
      return b;
    } else if (value instanceof String sval) {
      if (sval.equalsIgnoreCase("enable")) {
        return true;
      }
      if (sval.equalsIgnoreCase("disable")) {
        return false;
      }
      return Boolean.parseBoolean(sval.trim());
    }
    return false;
  }

  private long asLong(Object entry) {
    if (entry instanceof Number eNum) {
      if (eNum instanceof Float fl) {
        return Math.round(fl);
      }
      if (eNum instanceof Double db) {
        return Math.round(db);
      }
      return eNum.longValue();
    } else if (entry instanceof String sval) {
      String value = sval.trim();
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
    if (entry instanceof Number num) {
      return num.doubleValue();
    } else if (entry instanceof String str) {
      return Double.parseDouble(str.trim());
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
    if (object instanceof ConfigurationProperties cfg) {
      boolean listEquals = super.equals(object);
      if (global != null) {
        return listEquals && global.equals(cfg.global);
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

  public void replace(String key, Object val) {
    if(val instanceof ConfigurationProperties cfg) {
      map.replace(key, cfg);
    }
  }

  @SuppressWarnings("java:S3740")
  public void put(String key, Object val) {
    if (val instanceof Map map1) {
      ConfigurationProperties props = new ConfigurationProperties(map1);
      props.setGlobal(global);
      map.put(key, props);
    } else if (val instanceof List list1) {
      List<Object> parsedList = new ArrayList<>();
      for (Object list : list1) {
        if (list instanceof Map map2) {
          ConfigurationProperties props = new ConfigurationProperties(map2);
          props.setGlobal(global);
          parsedList.add(props);
        }
        else{
          if(list instanceof ConfigurationProperties) {
            parsedList.add(list);
          }
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
      if (entry.getValue() instanceof ConfigurationProperties cfg) {
        response.put(entry.getKey(), packMap(cfg.map));
      } else if (entry.getValue() instanceof Map mapEntry) {
        response.put(entry.getKey(), packMap(mapEntry));
      } else if (entry.getValue() instanceof List list) {
        List<Object> replacement = new ArrayList<>();
        for (Object obj : list) {
          if (obj instanceof Map mapEntry) {
            replacement.add(packMap(mapEntry));
          } else if (obj instanceof ConfigurationProperties cfg) {
            replacement.add(packMap(cfg.getMap()));
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
