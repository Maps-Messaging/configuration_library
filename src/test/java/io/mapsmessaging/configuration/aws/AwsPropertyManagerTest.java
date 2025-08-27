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

package io.mapsmessaging.configuration.aws;

import io.mapsmessaging.configuration.ConfigurationProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

class AwsPropertyManagerTest {


  @BeforeAll
  public static void loadConfig() throws IOException {
    Properties prop = new Properties();
    try (InputStream input = AwsPropertyManagerTest.class.getClassLoader().getResourceAsStream("s3.properties")) {
      if (input != null) {
        prop.load(input);
        for(Map.Entry<Object, Object> entry:prop.entrySet()){
          System.setProperty(entry.getKey().toString(), entry.getValue().toString());
        }
        return;
      }
    }
    try (InputStream input = new FileInputStream("s3.properties")) {
      prop.load(input);
      for(Map.Entry<Object, Object> entry:prop.entrySet()){
        System.setProperty(entry.getKey().toString(), entry.getValue().toString());
      }
    }
    catch(FileNotFoundException ex){
      // ignore this since we can fall through
    }
  }

  @DisplayName("test valid load")
  @Test
  void load() throws IOException {
    AwsPropertyManager propertyManager = new AwsPropertyManager("/test/storeTest");
    propertyManager.load();
    System.err.println(propertyManager.toString());
  }


  @DisplayName("Ensure we can sve the config")
  @Test
  void store() throws IOException {
    AwsPropertyManager propertyManager = new AwsPropertyManager("/test/storeTest");
    ConfigurationProperties properties = loadProperties();
    propertyManager.getProperties().put("data", properties);
    propertyManager.storeAll("data");
    AwsPropertyManager manager = new AwsPropertyManager("/test/storeTest");
    manager.load();
    Assertions.assertNotNull(manager.getProperties());
  }

  @DisplayName("Ensure we can sve the config")
  @Test
  void copy() throws IOException {
    AwsPropertyManager propertyManager = new AwsPropertyManager("/test/storeTest");
    ConfigurationProperties properties = loadProperties();
    propertyManager.getProperties().put("data", properties);
    propertyManager.storeAll("data");
    AwsPropertyManager reloaded = new AwsPropertyManager("/test/storeTest");
    reloaded.load();

    AwsPropertyManager manager = new AwsPropertyManager("/test/storeCopy");
    manager.load();
    manager.copy(reloaded);
    manager.save();
    Assertions.assertNotNull(manager.getProperties());

  }

  private ConfigurationProperties loadProperties(){
    ConfigurationProperties properties = new ConfigurationProperties();
    for(int x=0;x<100;x++) {
      properties.put("key"+x, "value"+x);
    }
    return properties;
  }
}
