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

package io.mapsmessaging.configuration;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;;
import java.util.stream.Collectors;

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

  @Test
  void getDefaultProperties() {
    PropertyManager manager = new TestPropertyManager();
    assertTrue(manager.properties.isEmpty());
    manager.load();
    assertFalse(manager.properties.isEmpty());
    for(String key:manager.properties.keySet()) {
      ConfigurationProperties config = manager.getProperties(key);
      assertNotNull(config);
    }
    assertEquals("/global/eur/default", manager.scanForDefaultConfig("/global/eur/gb"));
    assertEquals("/global/default", manager.scanForDefaultConfig("/global/na/us"));
    assertEquals("/default", manager.scanForDefaultConfig("/local/aus/syd"));
  }


  public final class TestPropertyManager extends PropertyManager {



    @Override
    public void load() {
      properties.put("/top", new LinkedHashMap<String, Object>());
      properties.put("/default", new LinkedHashMap<String, Object>());
      properties.put("/global/default", new LinkedHashMap<String, Object>());
      properties.put("/global/eur/default", new LinkedHashMap<String, Object>());
      properties.put("/global/eur/gb", new LinkedHashMap<String, Object>());
      properties.put("/global/eur/it", new LinkedHashMap<String, Object>());
      properties.put("/global/eur/de", new LinkedHashMap<String, Object>());
      properties.put("/global/eur/fr", new LinkedHashMap<String, Object>());
      properties.put("/global/asi/au", new LinkedHashMap<String, Object>());
      properties.put("/global/asi/nz", new LinkedHashMap<String, Object>());
      properties.put("/global/na/us", new LinkedHashMap<String, Object>());
      // not required
    }

    @Override
    protected void store(String name) throws IOException {
      // not required
    }

    @Override
    public void copy(PropertyManager propertyManager) throws IOException {
      properties.clear();
      properties.putAll(propertyManager.getProperties().getMap());
    }

    @Override
    protected List<String> getKeys(String lookup) {
      return new ArrayList<>(properties.keySet()).stream().filter(s -> s.startsWith(lookup)).collect(Collectors.toList());
    }
  }
}