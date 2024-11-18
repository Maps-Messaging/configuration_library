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

package io.mapsmessaging.configuration.consul;


import io.mapsmessaging.configuration.ConfigurationProperties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
  public static void beforeMethod() {
    System.setProperty("ConsulUrl", "http://10.140.62.12:8500");
  }

  @AfterEach
  void stopManager() throws IOException {
    for(String key:ConsulManagerFactory.getInstance().getManager().getKeys("/test/")){
      ConsulManagerFactory.getInstance().getManager().deleteKey(key);
    }
    ConsulManagerFactory.getInstance().stop();
  }

  private void startManager() throws IOException {
    ConsulManagerFactory.getInstance().start("/test/");
    Assumptions.assumeTrue(ConsulManagerFactory.getInstance().getManager() != null);
    for(String key:ConsulManagerFactory.getInstance().getManager().getKeys("/test/")){
      ConsulManagerFactory.getInstance().getManager().deleteKey(key);
    }

  }

  @DisplayName("test valid load")
  @Test
  void load() throws IOException {
    startManager();
    ConsulPropertyManager propertyManager = new ConsulPropertyManager("/test/storeTest");
    propertyManager.load();
    System.err.println(propertyManager.toString());
  }


  @DisplayName("Ensure we can sve the config")
  @Test
  void store() throws IOException {
    startManager();
    ConsulPropertyManager propertyManager = new ConsulPropertyManager("/test/storeTest");
    ConfigurationProperties properties = loadProperties();
    propertyManager.getProperties().put("data", properties);
    propertyManager.store("data");
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
    propertyManager.store("data");
    ConsulPropertyManager reloaded = new ConsulPropertyManager("/test/storeTest");
    reloaded.load();

    ConsulPropertyManager manager = new ConsulPropertyManager("/test/storeCopy");
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