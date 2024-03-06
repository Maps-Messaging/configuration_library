package io.mapsmessaging.configuration.aws;

import io.mapsmessaging.configuration.yaml.RemotePropertyManager;
import io.mapsmessaging.logging.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class AwsPropertyManager extends RemotePropertyManager {

  private final AwsSsmApi awsSsmApi;

  public AwsPropertyManager(String prefix) throws IOException {
    super(fixPrefix(prefix), LoggerFactory.getLogger(AwsPropertyManager.class));
    awsSsmApi = new AwsSsmApi();
  }

  @Override
  protected String getValue(String key) throws IOException {
    return awsSsmApi.getValue(key);
  }

  @Override
  protected void putValue(String name, String value) throws IOException {
    awsSsmApi.putValue(name, toYaml());
  }

  @Override
  protected void deleteKey(String key) throws IOException {
    awsSsmApi.deleteKey(key);
  }

  @Override
  protected List<String> getAllKeys(String prefix) throws IOException {
    return awsSsmApi.getKeys(prefix);
  }

  private static String fixPrefix(String prefix){
    if(!prefix.endsWith("/")){
      prefix = prefix+"/";
    }
    return prefix;
  }
}
