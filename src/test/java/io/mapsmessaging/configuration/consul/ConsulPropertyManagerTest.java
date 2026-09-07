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

package io.mapsmessaging.configuration.consul;

import io.mapsmessaging.configuration.ConfigurationProperties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.provider.Arguments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class ConsulPropertyManagerTest {

  static Stream<Arguments> driverNames() {

    List<Arguments> argumentsList = new ArrayList<>();
    String[] connectionTypes = {"ecwid"};
    for (String connectionType : connectionTypes) {
      argumentsList.add(Arguments.of(connectionType));
    }
    return argumentsList.stream();
  }

  @BeforeAll
  static void beforeMethod() {
    String consulUrl = System.getenv("CONSUL_URL");
    if (consulUrl == null || consulUrl.isBlank()) {
      consulUrl = "http://127.0.0.1:8500";
    }
    System.setProperty("ConsulUrl", consulUrl);
  }

  @AfterEach
  void stopManager() throws IOException {
    ConsulManagerFactory factory = ConsulManagerFactory.getInstance();
    ConsulServerApi manager = factory.getManager();
    try {
      if (manager != null) {
        for (String key : manager.getKeys("/test/")) {
          manager.deleteKey(key);
        }
      }
    } finally {
      factory.stop();
    }
  }

  private void startManager() throws IOException {
    ConsulManagerFactory factory = ConsulManagerFactory.getInstance();
    factory.start("/test/");
    ConsulServerApi manager = factory.getManager();
    Assumptions.assumeTrue(manager != null);
    for (String key : manager.getKeys("/test/")) {
      manager.deleteKey(key);
    }
  }

  @DisplayName("test valid load")
  @Test
  void load() throws IOException {
    startManager();
    ConsulPropertyManager propertyManager = new ConsulPropertyManager("/test/storeTest");
    propertyManager.load();
    Assertions.assertNotNull(propertyManager.getProperties());
    Assertions.assertNotNull(propertyManager.scanForDefaultConfig("/depth1/depth2/depth3"));
  }

  @DisplayName("Ensure we can sve the config")
  @Test
  void store() throws IOException {
    startManager();
    ConsulPropertyManager propertyManager = new ConsulPropertyManager("/test/storeTest");
    ConfigurationProperties properties = loadProperties();
    propertyManager.getProperties().put("data", properties);
    propertyManager.storeAll("data");
    ConsulPropertyManager manager = new ConsulPropertyManager("/test/storeTest");
    manager.load();
    Assertions.assertNotNull(manager.getProperties());
  }

  @DisplayName("Ensure we can sve the config")
  @Test
  void copy() throws IOException {
    startManager();
    ConsulPropertyManager propertyManager = new ConsulPropertyManager("/test/storeTest");
    ConfigurationProperties properties = loadProperties();
    propertyManager.getProperties().put("data", properties);
    propertyManager.storeAll("data");
    ConsulPropertyManager reloaded = new ConsulPropertyManager("/test/storeTest");
    reloaded.load();

    ConsulPropertyManager manager = new ConsulPropertyManager("/test/storeCopy");
    manager.load();
    manager.copy(reloaded);
    manager.save();
    Assertions.assertNotNull(manager.getProperties());
  }

  private ConfigurationProperties loadProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    for (int x = 0; x < 100; x++) {
      properties.put("key" + x, "value" + x);
    }
    return properties;
  }
}
