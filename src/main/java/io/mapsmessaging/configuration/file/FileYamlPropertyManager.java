/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.configuration.file;

import io.mapsmessaging.configuration.ResourceList;
import io.mapsmessaging.configuration.yaml.YamlPropertyManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

import java.io.*;
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
      Collection<String> knownProperties = ResourceList.getResources(Pattern.compile(".*yaml"));
      for (String propertyName : knownProperties) {
        loadProperty(propertyName);
      }
    } catch (IOException e) {
      logger.log(PROPERTY_MANAGER_SCAN_FAILED, e);
    }
  }

  @Override
  protected List<String> getKeys(String lookup) {
    return new ArrayList<>();
  }

  private void loadProperty(String propertyName) {
    try {
      propertyName = propertyName.substring(propertyName.lastIndexOf(File.separatorChar) + 1);
      propertyName = propertyName.substring(0, propertyName.indexOf(".yaml"));
      loadFile(propertyName);
      logger.log(PROPERTY_MANAGER_FOUND, propertyName);
    } catch (IOException e) {
      logger.log(PROPERTY_MANAGER_LOAD_FAILED, e, propertyName);
    }
  }

  private void loadFile(String propertyName) throws IOException {
    String propResourceName = "/" + propertyName;
    while (propResourceName.contains(".")) {
      propResourceName = propResourceName.replace('.', File.separatorChar);
    }
    propResourceName = propResourceName + ".yaml";
    InputStream is = getClass().getResourceAsStream(propResourceName);
    if (is != null) {
      int read = 1;
      byte[] buffer = new byte[1024];
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      while (read > 0) {
        read = is.read(buffer);
        if (read > 0) {
          byteArrayOutputStream.write(buffer, 0, read);
        }
      }
      is.close();
      parseAndLoadYaml(propertyName, byteArrayOutputStream.toString());
    } else {
      throw new FileNotFoundException("No such resource found " + propResourceName);
    }
  }

}
