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
    String[] connectionTypes = {"ecwid", "orbitz"};
    for (String connectionType : connectionTypes) {
      argumentsList.add(Arguments.of(connectionType));
    }
    return argumentsList.stream();
  }


  @BeforeAll
  public static void beforeMethod() throws IOException {
    System.setProperty("ConsulUrl", "http://10.140.62.12:8500");
  }

  @AfterEach
  void stopManager() throws IOException {
    for(String key:ConsulManagerFactory.getInstance().getManager().getKeys("/test/")){
      ConsulManagerFactory.getInstance().getManager().deleteKey(key);
    }
    ConsulManagerFactory.getInstance().stop();
  }

  private void startManager(String driver) throws IOException {
    ConsulManagerFactory.getInstance().start("/test/", driver);
    Assumptions.assumeTrue(ConsulManagerFactory.getInstance().getManager() != null);
    for(String key:ConsulManagerFactory.getInstance().getManager().getKeys("/test/")){
      ConsulManagerFactory.getInstance().getManager().deleteKey(key);
    }

  }

  @DisplayName("test valid load")
  @ParameterizedTest
  @MethodSource("driverNames")
  void load(String driver) throws IOException {
    startManager(driver);
    ConsulPropertyManager propertyManager = new ConsulPropertyManager("/test/storeTest");
    propertyManager.load();
    System.err.println(propertyManager.toString());
  }


  @DisplayName("Ensure we can sve the config")
  @ParameterizedTest
  @MethodSource("driverNames")
  void store(String driver) throws IOException {
    startManager(driver);
    ConsulPropertyManager propertyManager = new ConsulPropertyManager("/test/storeTest");
    ConfigurationProperties properties = loadProperties();
    propertyManager.getProperties().put("data", properties);
    propertyManager.store("data");
    ConsulPropertyManager manager = new ConsulPropertyManager("/test/storeTest");
    manager.load();
    Assertions.assertNotNull(manager.getProperties());
  }

  @DisplayName("Ensure we can sve the config")
  @ParameterizedTest
  @MethodSource("driverNames")
  void copy(String driver) throws IOException {
    startManager(driver);
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