package io.mapsmessaging.configuration;
/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationPropertiesTest {

  @Test
  void getProperty() {
    HashMap<String, Object> props = new LinkedHashMap<>();
    props.put("notEmpty", "value");
    ConfigurationProperties properties = new ConfigurationProperties(props);
    assertNull(properties.getProperty("empty"));
    assertNotNull(properties.getProperty("notEmpty"));
    assertEquals("value", properties.getProperty("notEmpty"));
    List<String> stringList = new ArrayList<>();
    stringList.add("aString");
    properties.put("stringList", stringList);
    properties.setSource(properties.toString());
    Assertions.assertEquals(properties.toString(), properties.getSource());

    Assertions.assertFalse(properties.isEmpty());
    Assertions.assertNotNull(properties.getProperty("stringList"));
    for(String key:properties.keySet()){
      Assertions.assertNotNull(properties.getProperty(key));
    }

    for(Map.Entry<String,Object> entry:properties.entrySet()){
      Assertions.assertNotNull(entry);
      Assertions.assertNotNull(entry.getKey());
      Assertions.assertNotNull(entry.getValue());
    }
  }

  @Test
  void getEnvironmentProperty() throws IOException {
    EnvironmentPathLookup lookup = new EnvironmentPathLookup("home", ".", false);
    Assertions.assertTrue(EnvironmentConfig.getInstance().registerPath(lookup));
    HashMap<String, Object> props = new LinkedHashMap<>();
    props.put("homeDirectory", "{{home}}/value");
    props.put("osName", "{{os.name}}");
    ConfigurationProperties properties = new ConfigurationProperties(props);
    assertNull(properties.getProperty("empty"));
    assertNotNull(properties.getProperty("homeDirectory"));
    assertEquals("."+ File.separator+"value", properties.getProperty("homeDirectory"));
    assertNotNull(properties.getProperty("osName"));
  }

  @Test
  void getBooleanProperty() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("trueStringValue", "true");
    props.put("falseStringValue", "false");
    props.put("enableStringValue", "enable");
    props.put("disableStringValue", "disable");

    props.put("trueValue", true);
    props.put("falseValue", false);
    props.put("booleanError", "this is not boolean");
    List<String> stringList = new ArrayList<>();
    stringList.add("aString");
    props.put("stringList", stringList);
    Assertions.assertNotNull(props.getProperty("stringList"));

    ConfigurationProperties properties = new ConfigurationProperties(props.getMap());
    properties.put("nextLevel", props.getMap());


    assertTrue(properties.getBooleanProperty("trueStringValue", false));
    assertTrue(properties.getBooleanProperty("enableStringValue", false));
    assertTrue(properties.getBooleanProperty("trueValue", false));

    assertFalse(properties.getBooleanProperty("falseStringValue", true));
    assertFalse(properties.getBooleanProperty("disableStringValue", true));
    assertFalse(properties.getBooleanProperty("falseValue", true));

    assertFalse(properties.getBooleanProperty("booleanError", false));
    assertTrue(properties.getBooleanProperty("empty", true));
  }

  @Test
  void getLongProperty() {
    HashMap<String, Object> props = new LinkedHashMap<>();
    props.put("longString", "10");
    props.put("long", 10);
    props.put("longPowerK", "10K");
    props.put("longPowerM", "10M");
    props.put("longPowerG", "10G");
    props.put("longPowerT", "10T");
    props.put("longError", "10errorf");
    props.put("longDaily", "daily");
    props.put("longWeekly", "weekly");
    props.put("longHourly", "hourly");

    ConfigurationProperties properties = new ConfigurationProperties(props);
    assertEquals(props.size(), properties.size());

    assertEquals(10, properties.getLongProperty("longString", 0));
    assertEquals(10, properties.getLongProperty("long", 0));

    assertEquals(10L*1024L, properties.getLongProperty("longPowerK", 0));
    assertEquals(10L*1024L*1024L, properties.getLongProperty("longPowerM", 0));
    assertEquals(10L*1024L*1024L*1024L, properties.getLongProperty("longPowerG", 0));
    assertEquals(10L*1024L*1024L*1024L*1024L, properties.getLongProperty("longPowerT", 0));

    assertEquals(60*60*1000, properties.getLongProperty("longHourly", 0));
    assertEquals(24*60*60*1000, properties.getLongProperty("longDaily", 0));
    assertEquals(7*24*60*60*1000, properties.getLongProperty("longWeekly", 0));

    assertEquals(123, properties.getLongProperty("longError", 123));
    assertEquals(123, properties.getLongProperty("empty", 123));
  }

  @Test
  void getIntProperty() {
    HashMap<String, Object> props = new LinkedHashMap<>();
    props.put("longString", "10");
    props.put("long", 10);
    props.put("longPowerK", "10K");
    props.put("longPowerM", "10M");
    props.put("longError", "10errorf");
    ConfigurationProperties properties = new ConfigurationProperties(props);

    assertEquals(10, properties.getIntProperty("longString", 0));
    assertEquals(10, properties.getIntProperty("long", 0));

    assertEquals(10L*1024, properties.getIntProperty("longPowerK", 0));
    assertEquals(10L*1024*1024, properties.getIntProperty("longPowerM", 0));
    assertEquals(123, properties.getIntProperty("longError", 123));
    assertEquals(123, properties.getIntProperty("empty", 123));
    Map<String, Object> map = properties.getMap();
    Assertions.assertNotNull(map);
    Assertions.assertEquals(properties.size(), map.size());
  }

  @Test
  void getFloatProperty() {
    HashMap<String, Object> props = new LinkedHashMap<>();
    props.put("floatString", "10.5");
    props.put("float", 10.5f);
    props.put("floatError", "10.5errorf");
    ConfigurationProperties properties = new ConfigurationProperties(props);

    assertEquals(10.5f, properties.getFloatProperty("floatString", 0));
    assertEquals(10.5f, properties.getFloatProperty("float", 0));

    assertEquals(11, properties.getIntProperty("floatString", 0)); // Round up
    assertEquals(11, properties.getIntProperty("float", 0));       // Round up

    assertEquals(123.5f, properties.getFloatProperty("floatError", 123.5f));
    assertEquals(123.5f, properties.getFloatProperty("empty", 123.5f));
  }

  @Test
  void getDoubleProperty() {
    HashMap<String, Object> props = new LinkedHashMap<>();
    props.put("doubleString", "12.5");
    props.put("double", 12.5);
    props.put("doubleError", "12.5ErrorNumber2");
    ConfigurationProperties properties = new ConfigurationProperties(props);

    assertEquals(12.5f, properties.getDoubleProperty("doubleString", 0));
    assertEquals(12.5f, properties.getDoubleProperty("double", 0));

    assertEquals(13, properties.getIntProperty("doubleString", 0)); // Round up
    assertEquals(13, properties.getIntProperty("double", 0));       // Round up

    assertEquals(123.5, properties.getDoubleProperty("doubleError", 123.5));
    assertEquals(123.5, properties.getDoubleProperty("empty", 123.5));
  }

  @Test
  void containsKey() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("notEmpty", "value");
    assertTrue(properties.containsKey("notEmpty"));
    assertFalse(properties.containsKey("empty"));
  }

  @Test
  void setGlobal() {
    ConfigurationProperties properties = new ConfigurationProperties();
    ConfigurationProperties global = new ConfigurationProperties();
    properties.setGlobal(global);
    assertEquals(global, properties.getGlobal());
  }

  @Test
  void getGlobal() {
    ConfigurationProperties properties = new ConfigurationProperties();
    ConfigurationProperties global = new ConfigurationProperties();
    properties.setGlobal(global);
    global.put("globalLong", "10");
    assertEquals(global, properties.getGlobal());
    assertEquals(10, properties.getLongProperty("globalLong", 0));
  }
}