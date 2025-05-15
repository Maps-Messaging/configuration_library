/*
 *
 *  Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *  Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
