package io.mapsmessaging.configuration;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public abstract class PropertyManagerTest {

  protected abstract PropertyManager create();

  @Test
  void load() {
    PropertyManager manager = create();
    assertTrue(manager.properties.isEmpty());
    manager.load();
    assertFalse(manager.properties.isEmpty());
  }

  @Test
  void store() throws IOException {
    PropertyManager manager = create();
    assertTrue(manager.properties.isEmpty());
    manager.load();
    assertFalse(manager.properties.isEmpty());
    File outFile = new File("./configTestFile");
    if(outFile.exists()){
      outFile.delete();
    }
    manager.store(outFile.getAbsolutePath());
    assertTrue(outFile.exists());
    assertTrue(outFile.delete());
  }

  @Test
  void copy() throws IOException {
    PropertyManager manager1 = create();
    PropertyManager manager2 = create();
    assertTrue(manager1.properties.isEmpty());
    manager1.load();
    assertFalse(manager1.properties.isEmpty());

    assertTrue(manager2.properties.isEmpty());
    manager2.copy(manager1);
    assertFalse(manager2.properties.isEmpty());
    assertEquals(manager1.toString(), manager2.toString());
  }

  @Test
  void getPropertiesJSON() {
    PropertyManager manager = create();
    assertTrue(manager.properties.isEmpty());
    manager.load();
    assertFalse(manager.properties.isEmpty());
    for(String key:manager.properties.keySet()) {
      JSONObject jsonObject = manager.getPropertiesJSON(key);
      assertNotNull(jsonObject);
    }
  }

  @Test
  void getProperties() {
    PropertyManager manager = create();
    assertTrue(manager.properties.isEmpty());
    manager.load();
    assertFalse(manager.properties.isEmpty());
    for(String key:manager.properties.keySet()) {
      ConfigurationProperties config = manager.getProperties(key);
      assertNotNull(config);
    }
  }
}