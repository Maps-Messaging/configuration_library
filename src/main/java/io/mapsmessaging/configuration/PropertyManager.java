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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Getter
public abstract class PropertyManager {

  protected final ConfigurationProperties properties;

  protected PropertyManager() {
    properties = new ConfigurationProperties();
  }

  public abstract void load();

  public abstract void storeAll(String path) throws IOException;

  public abstract void store(String path, String key) throws IOException;

  public abstract void copy(PropertyManager propertyManager) throws IOException;

  public void update(String path, String name, ConfigurationProperties newProps) throws IOException{
    properties.replace(name, newProps);
    store(path, name);
  }

  protected abstract List<String> getKeys(String lookup);

  public String scanForDefaultConfig(String namespace) {
    if (!namespace.endsWith("/")) {
      namespace = namespace + "/";
    }
    while (namespace.contains("/")) { // we have a depth
      String lookup = namespace + "default";
      List<String> keys = getKeys(lookup);
      if (keys != null && !keys.isEmpty()) {
        return lookup;
      }
      namespace = namespace.substring(0, namespace.length() - 1); // Remove the "/"
      int idx = namespace.lastIndexOf("/");
      if (idx >= 0 && namespace.length() > 1) {
        namespace = namespace.substring(0, idx + 1); // Include the /
      } else {
        break;
      }
    }
    return "";
  }


  public @NonNull @NotNull JsonObject getPropertiesJSON(@NonNull @NotNull String name) {
    JsonObject jsonObject = new JsonObject();
    Object config = properties.get(name);

    if (config instanceof ConfigurationProperties) {
      config = ((ConfigurationProperties) config).getMap();
    }

    if (config instanceof Map) {
      Map<String, Object> map = pack((Map<String, Object>) config);
      JsonElement element = SystemProperties.getInstance().getGson().toJsonTree(map);
      jsonObject.add(name, element);

      if (properties.getGlobal() != null) {
        Map<String, Object> globalMap = pack(properties.getGlobal().getMap());
        ((JsonObject) element).add("global", SystemProperties.getInstance().getGson().toJsonTree(globalMap));
      }
    } else {
      jsonObject.add(name, SystemProperties.getInstance().getGson().toJsonTree(config));
    }

    return jsonObject;
  }


  private void pack(Map<String, Object> map, String key, Object obj) {
    if (obj instanceof ConfigurationProperties) {
      pack(map, key, ((ConfigurationProperties) obj).getMap());
    } else if (obj instanceof Map) {
      map.put(key, pack((Map<String, Object>) obj));
    } else if (obj instanceof List) {
      List<Object> list = (List<Object>) obj;
      List<Object> translated = new ArrayList<>();
      for (Object tmp : list) {
        if (tmp instanceof ConfigurationProperties) {
          translated.add(pack(((ConfigurationProperties) tmp).getMap()));
        } else {
          translated.add(tmp);
        }
      }
      map.put(key, translated);
    } else {
      map.put(key, obj);
    }
  }

  private Map<String, Object> pack(Map<String, Object> map) {
    Map<String, Object> tmp = new LinkedHashMap<>();
    for (Entry<String, Object> entry : map.entrySet()) {
      String key = entry.getKey();
      Object obj = entry.getValue();
      pack(tmp, key, obj);
    }
    return tmp;
  }

  public boolean contains(String name) {
    return properties.containsKey(name);
  }

  public @NonNull @NotNull ConfigurationProperties getProperties(String name) {
    Object obj = properties.get(name);
    if (obj instanceof ConfigurationProperties) {
      return (ConfigurationProperties) obj;
    }
    return new ConfigurationProperties();
  }

}
