package io.mapsmessaging.configuration.aws;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ParameterStoreManager {

  private final SsmClient ssmClient;

  public ParameterStoreManager() {
    this.ssmClient = SsmClient.builder()
        .region(Region.of("your-region")) // Specify your region
        .build();
  }

  // Retrieve a list of parameter names based on a path
  public List<String> getKeys(String path) throws IOException {
    try {
      GetParametersByPathResponse response = ssmClient.getParametersByPath(GetParametersByPathRequest.builder()
          .path(path)
          .recursive(true)
          .build());
      return response.parameters().stream().map(Parameter::name).collect(Collectors.toList());
    } catch (SsmException e) {
      throw new IOException("Error retrieving keys from Parameter Store", e);
    }
  }

  // Retrieve a parameter value by name
  public String getValue(String key) throws IOException {
    try {
      GetParameterResponse response = ssmClient.getParameter(GetParameterRequest.builder()
          .name(key)
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
          .name(key)
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
          .name(key)
          .build());
    } catch (SsmException e) {
      throw new IOException("Error deleting key from Parameter Store", e);
    }
  }
}
