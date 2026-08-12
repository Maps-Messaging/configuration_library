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

import io.mapsmessaging.configuration.ResourceList;
import io.mapsmessaging.configuration.yaml.YamlPropertyManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import static io.mapsmessaging.logging.ConfigLogMessages.*;

public class FileYamlPropertyManager extends YamlPropertyManager {

  private final Logger logger = LoggerFactory.getLogger(FileYamlPropertyManager.class);

  @Override
  public void load() {
    try {
      Collection<String> knownProperties = ResourceList.getResources(Pattern.compile(".*\\.yaml$"));
      for (String propertyName : knownProperties) {
        try {
          loadProperty(propertyName);
        } catch (RuntimeException e) { // ignore during load
          logger.log(PROPERTY_MANAGER_SCAN_FAILED, e);
        }
      }
    } catch (IOException e) {
      logger.log(PROPERTY_MANAGER_SCAN_FAILED, e);
    }
  }



  @Override
  protected List<String> getKeys(String lookup) {
    return new ArrayList<>();
  }

  private void loadProperty(String resourceName) {
    String propertyName = resourceName;
    try {
      int separator = Math.max(propertyName.lastIndexOf('/'), propertyName.lastIndexOf('\\'));
      propertyName = propertyName.substring(separator + 1);
      propertyName = propertyName.substring(0, propertyName.indexOf(".yaml"));
      Path sourcePath = Path.of(resourceName);
      if (Files.isRegularFile(sourcePath)) {
        loadFile(propertyName, sourcePath);
      } else {
        loadResource(propertyName);
      }
      logger.log(PROPERTY_MANAGER_FOUND, propertyName);
    } catch (IOException e) {
      logger.log(PROPERTY_MANAGER_LOAD_FAILED, e, propertyName);
    }
  }

  private void loadFile(String propertyName, Path sourcePath) throws IOException {
    Path canonicalPath = sourcePath.toRealPath();
    parseAndLoadYaml(propertyName, Files.readString(canonicalPath, StandardCharsets.UTF_8), canonicalPath.toString());
  }

  private void loadResource(String propertyName) throws IOException {
    String propResourceName = "/" + propertyName;
    while (propResourceName.contains(".")) {
      propResourceName = propResourceName.replace('.', File.separatorChar);
    }
    propResourceName = propResourceName + ".yaml";
    try (InputStream inputStream = getClass().getResourceAsStream(propResourceName)) {
      if (inputStream != null) {
        parseAndLoadYaml(propertyName, new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        return;
      }
      throw new FileNotFoundException("No such resource found " + propResourceName);
    }
  }

}
