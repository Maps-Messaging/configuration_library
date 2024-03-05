package io.mapsmessaging.configuration.aws;

import io.mapsmessaging.configuration.SystemProperties;
import lombok.Getter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;

@Getter
public class AwsConfiguration {
  private final String applicationName;

  private Region region;
  private AwsCredentials credentials;

  public AwsConfiguration() throws IOException {
    String regionProps = SystemProperties.getInstance().getProperty("aws.region");
    String accessKey = SystemProperties.getInstance().getProperty("aws.accessKey");
    String secretKey = SystemProperties.getInstance().getProperty("aws.secretKey");
    applicationName = SystemProperties.getInstance().getProperty("applicationName");
    if (regionProps == null || accessKey == null || secretKey == null ||applicationName == null){
      throw new IOException("AWS SSM is not configured correctly");
    }

    credentials = AwsBasicCredentials.create(accessKey, secretKey);
    region = Region.of(regionProps);
  }

}
