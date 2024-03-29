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

package io.mapsmessaging.configuration.consul.ecwid;

import com.ecwid.consul.transport.TransportException;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.kv.model.GetValue;
import io.mapsmessaging.configuration.consul.Constants;
import io.mapsmessaging.configuration.consul.ConsulServerApi;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import static io.mapsmessaging.logging.ConfigLogMessages.*;

public class EcwidConsulManager extends ConsulServerApi {

  private final Logger logger = LoggerFactory.getLogger(EcwidConsulManager.class);

  private final Map<String, String> cache;
  private ConsulClient client;

  public EcwidConsulManager(String name) throws IOException {
    super(name);
    cache = new WeakHashMap<>();
    try {
      if (Boolean.getBoolean("ConsulDebug")) {
        java.util.logging.Logger apacheLogger = java.util.logging.Logger.getLogger("org.apache.http");
        apacheLogger.setLevel(Level.FINEST); // Use FINEST for most detailed logging
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINEST);
        apacheLogger.addHandler(consoleHandler);
        apacheLogger.setUseParentHandlers(false);
      }

      logger.log(CONSUL_CLIENT_LOG, "Creating client", consulConfiguration);
      client = createClient();
      this.getKeys("/");
      logger.log(CONSUL_CLIENT_LOG, "Created client", consulConfiguration);
      logger.log(CONSUL_STARTUP);
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  private ConsulClient createClient() throws MalformedURLException {
    URL url = new URL(consulConfiguration.getConsulUrl());
    int port = url.getPort();

    if (port == -1) {
      if (url.getProtocol().equalsIgnoreCase("https")) {
        port = 443;
      } else {
        port = 8500;
      }
    }


    List<Header> defaultHeaders = new ArrayList<>();
    if (consulConfiguration.getConsulToken() != null) {
      defaultHeaders.add(new BasicHeader("X-Consul-Token", consulConfiguration.getConsulToken()));
    }

    HttpClient httpClient = HttpClients.custom()
        .setDefaultHeaders(defaultHeaders)
        .build();

    String host = url.getProtocol() + "://" + url.getHost();

    ConsulRawClient rawClient = new ConsulRawClient(host, port, httpClient);

    return new ConsulClient(rawClient);
  }


  @Override
  protected void pingService() {
    // To Do
  }

  @Override
  public void register(Map<String, String> meta) {
    if (!consulConfiguration.registerAgent()) {
      return;
    }
    NewService.Check serviceCheck = new NewService.Check();
    serviceCheck.setInterval("10s");

    List<String> propertyNames = new ArrayList<>();
    logger.log(CONSUL_REGISTER);
    NewService newService = new NewService();
    newService.setId(uniqueName);
    newService.setName(Constants.NAME);
    newService.setTags(propertyNames);
    newService.setPort(Constants.CONSUL_PORT);
    newService.setCheck(serviceCheck);
    client.agentServiceRegister(newService);
    registerPingTask();
  }

  private void recreateClient() throws IOException {
    try {
      client = createClient();
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public List<String> getKeys(String key) throws IOException {
    int retry = 0;
    while (retry < 3) {
      try {
        return getKeysInternal(key);
      } catch (TransportException exception) {
        retry++;
        recreateClient();
        if (retry == 3) {
          throw exception;
        }
      }
    }
    return new ArrayList<>();
  }

  @Override
  public String getValue(String key) throws IOException {
    int retry = 0;
    while (retry < 3) {
      try {
        return getValueInternal(key);
      } catch (TransportException exception) {
        retry++;
        recreateClient();
        if (retry == 3) {
          throw exception;
        }
      }
    }
    return "";
  }

  @Override
  public void putValue(String key, String value) throws IOException {
    int retry = 0;
    while (retry < 3) {
      try {
        putValueInternal(key, value);
        return;
      } catch (TransportException exception) {
        retry++;
        recreateClient();
        if (retry == 3) {
          throw exception;
        }
      }
    }
  }

  @Override
  public void deleteKey(String key) throws IOException {
    int retry = 0;
    while (retry < 3) {
      try {
        deleteKeyInternal(key);
        return;
      } catch (TransportException exception) {
        retry++;
        recreateClient();
        if (retry == 3) {
          throw exception;
        }
      }
    }
  }

  private List<String> getKeysInternal(String key) {
    String keyName = validateKey(key);
    logger.log(CONSUL_KEY_VALUE_MANAGER, "getKeys", keyName);
    Response<List<String>> response = client.getKVKeysOnly(keyName);
    List<String> list = response.getValue();
    if (list == null) {
      list = new ArrayList<>();
    }
    return list;
  }

  private String getValueInternal(String key) {
    String keyName = validateKey(key);
    logger.log(CONSUL_KEY_VALUE_MANAGER, "GetValues", keyName);
    if (cache.containsKey(key)) {
      return cache.get(key);
    }
    Response<GetValue> response = client.getKVValue(keyName);
    GetValue getValue = response.getValue();
    String value = getValue.getDecodedValue();
    cache.put(key, value);
    return value;
  }

  private void putValueInternal(String key, String value) {
    String keyName = validateKey(key);
    cache.remove(key);
    value = value.replace("\n", "\r\n");
    value = value.replace("\r\r", "\r");
    logger.log(CONSUL_KEY_VALUE_MANAGER, "putValue", keyName);
    client.setKVValue(keyName, value);
  }

  private void deleteKeyInternal(String key) {
    String keyName = validateKey(key);
    cache.remove(key);
    logger.log(CONSUL_KEY_VALUE_MANAGER, "deleteKey", keyName);
    client.deleteKVValue(keyName);
  }
}
