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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    String advertise = consulConfiguration.getServiceAddress();
    NewService newService = createService(uniqueName, meta, advertise);
    logger.log(CONSUL_REGISTER);
    client.agentServiceRegister(newService);
    for (NewService listener : createListenerServices(uniqueName, meta, advertise)) {
      client.agentServiceRegister(listener);
    }
    registerPingTask();
  }

  static NewService createService(String uniqueName, Map<String, String> meta) {
    return createService(uniqueName, meta, null);
  }

  static NewService createService(String uniqueName, Map<String, String> meta, String advertiseAddress) {
    String restEndpoint = meta == null ? null : meta.get(Constants.REST_API);
    if (restEndpoint == null || restEndpoint.isBlank()) {
      throw new IllegalArgumentException("REST API endpoint is required for Consul registration");
    }

    String[] hostPort = parseEndpoint("tcp://" + restEndpoint, restEndpoint);
    // The advertise address override exists for deployments where the address
    // this process can SEE is not the address its CONSUMERS can route to. In a
    // container with bridge networking (Nomad/Docker), the auto-detected local
    // address is the bridge-internal one: reachable from the host running the
    // Consul agent, unroutable from every other node. Fleets whose nodes only
    // share an overlay (e.g. WireGuard/Tailscale) must register the overlay
    // address or cross-node discovery returns an address that cannot be
    // reached. The health check follows the advertised address deliberately:
    // the local agent runs the check, and an advertise address the local host
    // itself cannot reach is misconfigured and SHOULD fail visibly.
    String host = (advertiseAddress != null && !advertiseAddress.isBlank()) ? advertiseAddress.trim() : hostPort[0];
    int port = Integer.parseInt(hostPort[1]);

    NewService.Check serviceCheck = new NewService.Check();
    serviceCheck.setTcp(formatHostPort(host, port));
    serviceCheck.setInterval("10s");
    serviceCheck.setDeregisterCriticalServiceAfter("1m");

    NewService newService = new NewService();
    newService.setId(uniqueName);
    newService.setName(Constants.NAME);
    newService.setAddress(host);
    newService.setPort(port);
    newService.setMeta(new LinkedHashMap<>(meta));
    newService.setCheck(serviceCheck);
    return newService;
  }

  // One service per protocol listener, derived from the endpoint entries the
  // server already places in meta (e.g. "mqtt" -> "tcp://0.0.0.0:1883/").
  // Consumers discover a PROTOCOL, not the server: a bridge or an edge node
  // asks for the mqtt endpoint ("<prefix>mqtt"), and asking the mapsMessaging
  // service then guessing ports is exactly the orchestration-side workaround
  // this replaces. Listener endpoints normally bind the wildcard address, so
  // the registered address comes from the advertise override when set, else
  // from the REST endpoint's (auto-detected) address — same reachability
  // reasoning as createService above.
  static List<NewService> createListenerServices(String uniqueName, Map<String, String> meta, String advertiseAddress) {
    if (meta == null) {
      return new ArrayList<>();
    }

    String restEndpoint = meta.get(Constants.REST_API);
    if (restEndpoint == null || restEndpoint.isBlank()) {
      return new ArrayList<>();
    }

    String[] restHostPort = parseEndpoint("tcp://" + restEndpoint, restEndpoint);
    String serviceHost = resolveServiceHost(advertiseAddress, restHostPort[0]);
    String restCheckTarget = formatHostPort(serviceHost, Integer.parseInt(restHostPort[1]));

    List<NewService> services = new ArrayList<>();
    for (Map.Entry<String, String> entry : meta.entrySet()) {
      NewService service = createListenerService(uniqueName, entry, serviceHost, restCheckTarget);
      if (service != null) {
        services.add(service);
      }
    }
    return services;
  }

  private static NewService createListenerService(
      String uniqueName,
      Map.Entry<String, String> entry,
      String serviceHost,
      String restCheckTarget) {
    String key = entry.getKey();
    String value = entry.getValue();
    URI endpoint = parseListenerEndpoint(key, value);
    if (endpoint == null) {
      return null;
    }

    int port = endpoint.getPort();
    if (port < 1 || port > 65535) {
      return null;
    }

    String scheme = endpoint.getScheme() == null ? "" : endpoint.getScheme().toLowerCase();
    NewService.Check check = new NewService.Check();
    check.setTcp(isTcpTransport(scheme) ? formatHostPort(serviceHost, port) : restCheckTarget);
    check.setInterval("10s");
    check.setDeregisterCriticalServiceAfter("1m");

    NewService service = new NewService();
    service.setId(uniqueName + "-" + key);
    service.setName(Constants.LISTENER_SERVICE_PREFIX + key);
    service.setAddress(serviceHost);
    service.setPort(port);
    service.setTags(List.of(key, uniqueName));
    service.setCheck(check);
    return service;
  }

  private static URI parseListenerEndpoint(String key, String value) {
    if (Constants.REST_API.equals(key) || value == null || !value.contains("://")) {
      return null;
    }

    try {
      return new URI(value.trim());
    } catch (URISyntaxException exception) {
      return null;
    }
  }

  private static boolean isTcpTransport(String scheme) {
    return scheme.startsWith("tcp")
        || scheme.startsWith("ssl")
        || scheme.startsWith("tls")
        || scheme.startsWith("ws");
  }

  private static String resolveServiceHost(String advertiseAddress, String restHost) {
    return advertiseAddress != null && !advertiseAddress.isBlank()
        ? advertiseAddress.trim()
        : restHost;
  }

  // parseEndpoint URISTRING ORIGINAL -> {host, port}; shared validation for the
  // REST endpoint (wildcard/blank hosts rejected: an unroutable registration is
  // worse than a failed one).
  private static String[] parseEndpoint(String uriString, String original) {
    URI endpoint;
    try {
      endpoint = new URI(uriString);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid REST API endpoint: " + original, e);
    }
    String host = endpoint.getHost();
    int port = endpoint.getPort();
    if (host != null && host.startsWith("[") && host.endsWith("]")) {
      host = host.substring(1, host.length() - 1);
    }
    if (host == null || host.isBlank() || port < 1 || port > 65535) {
      throw new IllegalArgumentException("Invalid REST API endpoint: " + original);
    }
    if (host.equals("0.0.0.0") || host.equals("::") || host.equals("0:0:0:0:0:0:0:0")) {
      throw new IllegalArgumentException("REST API endpoint must not use a wildcard address: " + original);
    }
    return new String[]{host, String.valueOf(port)};
  }

  private static String formatHostPort(String host, int port) {
    return host.contains(":") ? "[" + host + "]:" + port : host + ":" + port;
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
