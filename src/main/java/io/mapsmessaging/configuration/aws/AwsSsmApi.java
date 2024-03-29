package io.mapsmessaging.configuration.aws;

import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class AwsSsmApi {

  private final SsmClient ssmClient;
  private final String prefix;

  public AwsSsmApi() throws IOException {
    AwsConfiguration awsConfiguration = new AwsConfiguration();
    prefix = awsConfiguration.getApplicationName();
    this.ssmClient = SsmClient.builder()
        .region(awsConfiguration.getRegion())
        .credentialsProvider(awsConfiguration::getCredentials)
        .build();
  }

  // Retrieve a list of parameter names based on a path
  public List<String> getKeys(String path) throws IOException {
    try {
      GetParametersByPathResponse response = ssmClient.getParametersByPath(GetParametersByPathRequest.builder()
          .path(buildKey(path))
          .recursive(true)
          .build());
      List<String> keys = response.parameters().stream().map(Parameter::name).collect(Collectors.toList());
      return keys.stream()
          .map(this::processKey)
          .collect(Collectors.toList());
    } catch (SsmException e) {
      throw new IOException("Error retrieving keys from Parameter Store", e);
    }
  }

  private String processKey(String key){
    if(key.startsWith(prefix)){
      key = key.substring(prefix.length());
    }
    if(!key.startsWith("/")){
      key = "/"+key;
    }
    return key;
  }
  // Retrieve a parameter value by name
  public String getValue(String key) throws IOException {
    try {
      GetParameterResponse response = ssmClient.getParameter(GetParameterRequest.builder()
          .name(buildKey(key))
          .withDecryption(true)
          .build());
      return response.parameter().value();
    } catch (SsmException e) {
      throw new IOException("Error retrieving value from Parameter Store", e);
    }
  }

  // Put a parameter value by name
  public void putValue(String key, String value) throws IOException {
    try {
      ssmClient.putParameter(PutParameterRequest.builder()
          .name(buildKey(key))
          .value(value)
          .type(ParameterType.STRING)
          .overwrite(true)
          .build());
    } catch (SsmException e) {
      throw new IOException("Error putting value to Parameter Store", e);
    }
  }

  // Delete a parameter by name
  public void deleteKey(String key) throws IOException {
    try {
      ssmClient.deleteParameter(DeleteParameterRequest.builder()
          .name(buildKey(key))
          .build());
    } catch (SsmException e) {
      throw new IOException("Error deleting key from Parameter Store", e);
    }
  }

  private String buildKey(String key){
   return (prefix+key).replace("//", "/");
  }
}
