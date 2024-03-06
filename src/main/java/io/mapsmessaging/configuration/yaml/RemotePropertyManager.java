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
      String name = key.substring(serverPrefix.length());
      logger.log(REMOTE_PROPERTY_MANAGER_KEY_LOOKUP_SUCCESS, name, value.length());
      parseAndLoadYaml(name, value);
    } catch (IOException awsException) {
      logger.log(REMOTE_PROPERTY_MANAGER_KEY_LOOKUP_EXCEPTION, key, awsException);
    }
  }

  @Override
  public void store(String name) throws IOException {
    logger.log(REMOTE_PROPERTY_MANAGER_STORE, serverPrefix, name);
    putValue(serverPrefix + name, toYaml());
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
