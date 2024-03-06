/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.configuration.consul;


import io.mapsmessaging.configuration.yaml.RemotePropertyManager;
import io.mapsmessaging.logging.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class ConsulPropertyManager extends RemotePropertyManager {
  public ConsulPropertyManager(String prefix) {
    super(prefix, LoggerFactory.getLogger(ConsulPropertyManager.class));
  }

  @Override
  protected String getValue(String key) throws IOException {
    return ConsulManagerFactory.getInstance().getManager().getValue(key);
  }

  @Override
  protected void putValue(String name, String value) throws IOException {
    ConsulManagerFactory.getInstance()
        .getManager()
        .putValue(serverPrefix + name, getPropertiesJSON(name).toString(2));
  }

  @Override
  protected void deleteKey(String key) throws IOException {
    ConsulManagerFactory.getInstance()
        .getManager()
        .deleteKey(key);
  }

  @Override
  protected List<String> getAllKeys(String prefix) throws IOException {
    return ConsulManagerFactory.getInstance().getManager().getKeys(prefix);
  }
}
