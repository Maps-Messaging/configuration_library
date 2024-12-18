/*
 *     Copyright [2020 - 2024]   [Matthew Buckton]
 *     Copyright [2024 - 2024]   [Maps Messaging B.V.]
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *
 */

package io.mapsmessaging.configuration.yaml;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.configuration.PropertyManager;
import io.mapsmessaging.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.mapsmessaging.logging.ConfigLogMessages.*;

public abstract class RemotePropertyManager extends YamlPropertyManager {

  protected final String serverPrefix;
  private final Logger logger;

  protected RemotePropertyManager(String serverPrefix, Logger logger) {
    this.serverPrefix = serverPrefix;
    this.logger = logger;
  }

  @Override
  public void load() {
    for (String key : getKeys(serverPrefix)) {
      processKey(key);
    }
  }

  protected abstract String getValue(String key) throws IOException;
  protected abstract void putValue(String key, String value)throws IOException;
  protected abstract void deleteKey(String key)throws IOException;
  protected abstract List<String> getAllKeys(String prefix) throws IOException;

  protected void processKey(String key) {
    try {
      String value = getValue(key);
      int stripLeadingSlash = (!key.startsWith(serverPrefix) && serverPrefix.startsWith("/")) ? 1 : 0;
      String name = key.substring(serverPrefix.length()-stripLeadingSlash);
      logger.log(REMOTE_PROPERTY_MANAGER_KEY_LOOKUP_SUCCESS, name, value.length());
      parseAndLoadYaml(name, value);
    } catch (IOException awsException) {
      logger.log(REMOTE_PROPERTY_MANAGER_KEY_LOOKUP_EXCEPTION, key, awsException);
    }
  }

  @Override
  public void storeAll(String name) throws IOException {
    logger.log(REMOTE_PROPERTY_MANAGER_STORE, serverPrefix, name);
    putValue(serverPrefix + name, toYaml());
  }

  @Override
  public void store(String path, String key) throws IOException {
    logger.log(REMOTE_PROPERTY_MANAGER_STORE, serverPrefix, key);
    putValue(serverPrefix + key, toYaml());
  }

  @Override
  public void copy(PropertyManager propertyManager) throws IOException {
    // Remove what we have
    for (String name : properties.keySet()) {
      deleteKey(serverPrefix+name);
    }

    // Now let's add the new config
    properties.clear();
    properties.putAll(propertyManager.getProperties().getMap());
    for (String key : properties.keySet()) {
      ConfigurationProperties copy = (ConfigurationProperties) properties.get(key);
      ConfigurationProperties orig = (ConfigurationProperties) propertyManager.getProperties().get(key);
      copy.setSource(orig.getSource());
    }


    if (properties.getGlobal() != null) {
      properties.getGlobal().clear();
    }
    if (propertyManager.getProperties().getGlobal() != null) {
      properties.setGlobal(propertyManager.getProperties().getGlobal());
    }
    properties.setSource(propertyManager.getProperties().getSource());
    save();
  }

  @Override
  protected List<String> getKeys(String lookup) {
    try {
      return getAllKeys(lookup);
    } catch (IOException e) {
      logger.log(REMOTE_PROPERTY_MANAGER_NO_KEY_VALUES, serverPrefix);
    }
    return new ArrayList<>();
  }


  public void save() throws IOException {
    logger.log(REMOTE_PROPERTY_MANAGER_SAVE_ALL, serverPrefix);
    for (Map.Entry<String, Object> entry : properties.entrySet()) {
      String source = ((ConfigurationProperties) entry.getValue()).getSource();
      String key = serverPrefix.trim() + entry.getKey().trim();
      putValue(key, source);
    }
  }
}
