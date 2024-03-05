package io.mapsmessaging.configuration.aws;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.configuration.PropertyManager;
import io.mapsmessaging.configuration.yaml.YamlPropertyManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.mapsmessaging.logging.ConfigLogMessages.*;

public class AwsPropertyManager extends YamlPropertyManager {

  private final String serverPrefix;
  private final Logger logger = LoggerFactory.getLogger(AwsPropertyManager.class);
  private final AwsSsmApi awsSsmApi;

  public AwsPropertyManager(String prefix) throws IOException {
    if (prefix.startsWith("/")) {
      prefix = prefix.substring(1);
    }
    serverPrefix = prefix + "/";
    awsSsmApi = new AwsSsmApi();
  }

  @Override
  public void load() {
    for (String key : getKeys(serverPrefix)) {
      processKey(key);
    }
  }

  private void processKey(String key) {
    try {
      String value = awsSsmApi.getValue(key);
      String name = key.substring(serverPrefix.length());
      logger.log(AWS_PROPERTY_MANAGER_KEY_LOOKUP_SUCCESS, name, value.length());
      parseAndLoadYaml(name, value);
    } catch (IOException AWSException) {
      logger.log(AWS_PROPERTY_MANAGER_KEY_LOOKUP_EXCEPTION, key, AWSException);
    }
  }

  @Override
  protected void store(String name) throws IOException {
    logger.log(AWS_PROPERTY_MANAGER_STORE, serverPrefix, name);
    awsSsmApi.putValue(serverPrefix + name, toYaml(name));
  }

  @Override
  public void copy(PropertyManager propertyManager) throws IOException {
    // Remove what we have
    for (String name : properties.keySet()) {
      awsSsmApi.deleteKey(serverPrefix+name);
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
      return awsSsmApi.getKeys(lookup);
    } catch (IOException e) {
      logger.log(AWS_PROPERTY_MANAGER_NO_KEY_VALUES, serverPrefix);
    }
    return new ArrayList<>();
  }


  public void save() throws IOException {
    logger.log(AWS_PROPERTY_MANAGER_SAVE_ALL, serverPrefix);
    for (Map.Entry<String, Object> entry : properties.entrySet()) {
      String source = ((ConfigurationProperties) entry.getValue()).getSource();
      String key = serverPrefix.trim() + entry.getKey().trim();
      awsSsmApi.putValue(key, source);
    }
  }
}
