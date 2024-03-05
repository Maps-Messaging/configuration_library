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

package io.mapsmessaging.configuration.yaml;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.configuration.PropertyManager;
import io.mapsmessaging.configuration.parsers.JsonParser;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public abstract class YamlPropertyManager extends PropertyManager {

  private static final String GLOBAL = "global";

  protected void parseAndLoadYaml(String propertyName, String yamlString) throws IOException {
    Yaml yaml = new Yaml();
    JsonParser parser = new YamlParser(yaml.load(yamlString));
    Map<String, Object> response = parser.parse();
    Object topLevel = response.get(propertyName);
    if (topLevel instanceof Map) {
      Map<String, Object> root = (Map<String, Object>) topLevel;
      root.put("loaded", System.currentTimeMillis());
    }
    ConfigurationProperties configurationProperties = new ConfigurationProperties();
    for (Entry<String, Object> item : response.entrySet()) {
      Map<String, Object> entry = (Map<String, Object>) item.getValue();
      if (entry.get(GLOBAL) != null) {
        Map<String, Object> global = (Map<String, Object>) entry.remove(GLOBAL);
        configurationProperties.setGlobal(new ConfigurationProperties(global));
      }
      configurationProperties.putAll(entry);
    }
    configurationProperties.setSource(yamlString);
    properties.put(propertyName, configurationProperties);
  }

  @Override
  protected void store(String name) throws IOException {
    HashMap<String, Object> data = new LinkedHashMap<>(properties.getMap());
    if (properties.getGlobal() != null) {
      data.put(GLOBAL, new LinkedHashMap<>(properties.getGlobal().getMap()));
    }
    try (PrintWriter writer = new PrintWriter(name)) {
      Yaml yaml = new Yaml();
      yaml.dump(data, writer);
    }
  }

  protected String toYaml(){
    HashMap<String, Object> data = new LinkedHashMap<>(properties.getMap());
    if (properties.getGlobal() != null) {
      data.put(GLOBAL, new LinkedHashMap<>(properties.getGlobal().getMap()));
    }
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(10240);
    try (PrintWriter writer = new PrintWriter(byteArrayOutputStream)) {
      Yaml yaml = new Yaml();
      yaml.dump(data, writer);
    }
    return byteArrayOutputStream.toString();
  }

  @Override
  public void copy(PropertyManager propertyManager) throws IOException {
    HashMap<String, Object> data = new LinkedHashMap<>(propertyManager.getProperties().getMap());
    properties.clear();
    properties.putAll(data);
    properties.setGlobal(properties.getGlobal());
  }

  @Override
  public String toString() {
    HashMap<String, Object> data = new LinkedHashMap<>(properties.getMap());
    if (properties.getGlobal() != null) {
      data.put(GLOBAL, new LinkedHashMap<>(properties.getGlobal().getMap()));
    }
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024);
    try (PrintWriter writer = new PrintWriter(outputStream)) {
      Yaml yaml = new Yaml();
      yaml.dump(data, writer);
    }
    return outputStream.toString();
  }
}
