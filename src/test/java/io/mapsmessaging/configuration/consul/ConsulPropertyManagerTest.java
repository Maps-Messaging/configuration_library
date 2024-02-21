package io.mapsmessaging.configuration.consul;


import io.mapsmessaging.configuration.ConfigurationProperties;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

class ConsulPropertyManagerTest {

  @BeforeEach
  public void beforeMethod() {
    ConsulManagerFactory.getInstance().start(UUID.randomUUID().toString());
    if(ConsulManagerFactory.getInstance().isStarted()) {
      ConsulManagerFactory.getInstance().getManager();
    }
    Assumptions.assumeTrue(ConsulManagerFactory.getInstance().getManager() != null);
  }

  @Test
  void load() {
    ConsulPropertyManager propertyManager = new ConsulPropertyManager("storeTest");
    propertyManager.load();
    System.err.println(propertyManager.toString());
  }

  @Test
  void store() throws IOException {
    ConsulPropertyManager propertyManager = new ConsulPropertyManager("storeTest");
    ConfigurationProperties properties = new ConfigurationProperties();
    for(int x=0;x<100;x++) {
      properties.put("key"+x, "value"+x);
    }
    propertyManager.getProperties().put("data", properties);
    propertyManager.store("data");
    propertyManager.load();
    System.err.println(propertyManager.toString());
  }

  @Test
  void copy() {

  }

  @Test
  void save() {

  }
}