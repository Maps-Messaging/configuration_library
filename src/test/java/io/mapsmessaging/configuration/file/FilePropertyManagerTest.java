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

package io.mapsmessaging.configuration.file;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.configuration.PropertyManager;
import io.mapsmessaging.configuration.PropertyManagerTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class FilePropertyManagerTest extends PropertyManagerTest {

  private static LogListener logListener;

  @BeforeAll
  static void setUp() {
    logListener = new LogListener();
    logListener.register();
  }

  @Override
  protected PropertyManager create() {
    return new FileYamlPropertyManager();
  }


  @Test
  void smartQuotes() {
    PropertyManager manager = create();
    manager.load();
    ConfigurationProperties prop = manager.getProperties("test2");
    ConfigurationProperties data = (ConfigurationProperties) prop.get("data");
    data.getProperty("badValue");
    boolean found = false;
    for(String message: logListener.getLogMessages()) {
      if(message.contains("Detected smart quotes for key")){
        found = true;
        break;
      }
    }
    Assertions.assertTrue(found, "Log message should have been raised");
  }

  @Test
  void recordsSourcePathAndUpdatesOriginalFile() throws IOException {
    Path source = writeConfiguration("source-path", "original");
    Path redirected = tempDirectory.resolve("redirected.yaml");
    FileYamlPropertyManager manager = loadFrom(tempDirectory);

    ConfigurationProperties loaded = manager.getProperties("source-path");
    assertEquals(source.toRealPath().toString(), loaded.getSourcePath());

    ConfigurationProperties replacement = createDocument("source-path", "updated");
    replacement.setSourcePath(redirected.toString());
    manager.update(tempDirectory.resolve("fallback").toString(), "source-path", replacement);

    assertEquals(source.toRealPath().toString(), replacement.getSourcePath());
    assertTrue(Files.readString(source, StandardCharsets.UTF_8).contains("value: updated"));
    assertFalse(Files.exists(redirected));
    assertFalse(Files.exists(tempDirectory.resolve("fallback/source-path.yaml")));
  }

  @Test
  void rejectsUnknownConfiguration() {
    FileYamlPropertyManager manager = new FileYamlPropertyManager();

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> manager.update(tempDirectory.toString(), "missing", createDocument("missing", "updated")));

    assertEquals("Unknown configuration: missing", exception.getMessage());
    assertFalse(Files.exists(tempDirectory.resolve("missing.yaml")));
  }

  @Test
  void restoresInMemoryConfigurationWhenStoreFails() throws IOException {
    Path source = writeConfiguration("rollback", "original");
    FileYamlPropertyManager manager = loadFrom(tempDirectory);
    ConfigurationProperties original = manager.getProperties("rollback");

    Files.delete(source);
    Files.createDirectory(source);

    assertThrows(IOException.class, () -> manager.update(tempDirectory.toString(), "rollback", createDocument("rollback", "updated")));
    assertSame(original, manager.getProperties("rollback"));
    assertEquals("original", original.getProperty("value"));
  }

  @Test
  void skipsMalformedYamlAndNonYamlFiles() throws IOException {
    Files.writeString(tempDirectory.resolve("malformed.yaml"), "malformed: [", StandardCharsets.UTF_8);
    Files.writeString(tempDirectory.resolve("ignored.yaml.bak"), "ignored:\n   value: ignored\n", StandardCharsets.UTF_8);

    FileYamlPropertyManager manager = loadFrom(tempDirectory);

    assertFalse(manager.contains("malformed"));
    assertFalse(manager.contains("ignored"));
  }

  private FileYamlPropertyManager loadFrom(Path directory) {
    String originalClassPath = System.getProperty("java.class.path");
    FileYamlPropertyManager manager = new FileYamlPropertyManager();
    try {
      System.setProperty("java.class.path", directory.toString());
      manager.load();
    } finally {
      System.setProperty("java.class.path", originalClassPath);
    }
    return manager;
  }

  private Path writeConfiguration(String name, String value) throws IOException {
    Path path = tempDirectory.resolve(name + ".yaml");
    Files.writeString(path, name + ":\n   value: " + value + "\n", StandardCharsets.UTF_8);
    return path;
  }

  private ConfigurationProperties createDocument(String name, String value) {
    ConfigurationProperties values = new ConfigurationProperties();
    values.put("value", value);
    ConfigurationProperties document = new ConfigurationProperties();
    document.put(name, values);
    return document;
  }

}
