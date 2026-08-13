/*
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

import com.ecwid.consul.v1.agent.model.NewService;
import io.mapsmessaging.configuration.consul.Constants;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EcwidConsulManagerTest {

  @Test
  void createService_usesRestEndpointForRegistrationAndHealthCheck() {
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("mqtt", "tcp://0.0.0.0:1883/");
    meta.put(Constants.REST_API, "192.0.2.10:8081");

    NewService service = EcwidConsulManager.createService("server-1", meta);

    assertEquals("server-1", service.getId());
    assertEquals(Constants.NAME, service.getName());
    assertEquals("192.0.2.10", service.getAddress());
    assertEquals(8081, service.getPort());
    assertEquals(meta, service.getMeta());
    assertEquals("192.0.2.10:8081", service.getCheck().getTcp());
  }

  @Test
  void createService_supportsIpv6RestEndpoint() {
    Map<String, String> meta = Map.of(Constants.REST_API, "[2001:db8::10]:8443");

    NewService service = EcwidConsulManager.createService("server-1", meta);

    assertEquals("2001:db8::10", service.getAddress());
    assertEquals(8443, service.getPort());
    assertEquals("[2001:db8::10]:8443", service.getCheck().getTcp());
  }

  @Test
  void createService_rejectsMissingRestEndpoint() {
    assertThrows(IllegalArgumentException.class, () -> EcwidConsulManager.createService("server-1", Map.of()));
    assertThrows(IllegalArgumentException.class, () -> EcwidConsulManager.createService("server-1", null));
  }

  @Test
  void createService_rejectsMalformedRestEndpoint() {
    assertThrows(IllegalArgumentException.class, () -> EcwidConsulManager.createService("server-1", Map.of(Constants.REST_API, "192.0.2.10")));
    assertThrows(IllegalArgumentException.class, () -> EcwidConsulManager.createService("server-1", Map.of(Constants.REST_API, "192.0.2.10:not-a-port")));
  }

  @Test
  void createService_rejectsWildcardRestEndpoint() {
    assertThrows(IllegalArgumentException.class, () -> EcwidConsulManager.createService("server-1", Map.of(Constants.REST_API, "0.0.0.0:8080")));
    assertThrows(IllegalArgumentException.class, () -> EcwidConsulManager.createService("server-1", Map.of(Constants.REST_API, "[::]:8080")));
  }

  @Test
  void createService_advertiseAddressOverridesRegistrationAndCheck() {
    Map<String, String> meta = Map.of(Constants.REST_API, "172.26.64.18:8080");

    NewService service = EcwidConsulManager.createService("server-1", meta, "100.87.176.28");

    assertEquals("100.87.176.28", service.getAddress());
    assertEquals(8080, service.getPort());
    assertEquals("100.87.176.28:8080", service.getCheck().getTcp());
  }

  @Test
  void createListenerServices_registersOneServicePerEndpointEntry() {
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put(Constants.REST_API, "172.26.64.18:8080");
    meta.put("mqtt", "tcp://0.0.0.0:1883/");
    meta.put("stomp", "tcp://0.0.0.0:8674/");
    meta.put("version", "4.5.0"); // plain metadata: not an endpoint, not a service

    List<NewService> services = EcwidConsulManager.createListenerServices("server-1", meta, null);

    assertEquals(2, services.size());
    NewService mqtt = services.get(0);
    assertEquals("server-1-mqtt", mqtt.getId());
    assertEquals(Constants.LISTENER_SERVICE_PREFIX + "mqtt", mqtt.getName());
    assertEquals("172.26.64.18", mqtt.getAddress());
    assertEquals(1883, mqtt.getPort());
    assertEquals("172.26.64.18:1883", mqtt.getCheck().getTcp());
  }

  @Test
  void createListenerServices_advertiseAddressAppliesToListeners() {
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put(Constants.REST_API, "172.26.64.18:8080");
    meta.put("mqtt", "tcp://0.0.0.0:1883/");

    List<NewService> services = EcwidConsulManager.createListenerServices("server-1", meta, "100.87.176.28");

    assertEquals("100.87.176.28", services.get(0).getAddress());
    assertEquals("100.87.176.28:1883", services.get(0).getCheck().getTcp());
  }

  @Test
  void createListenerServices_udpListenerChecksRestEndpointForLiveness() {
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put(Constants.REST_API, "172.26.64.18:8080");
    meta.put("mavlink", "udp://0.0.0.0:14450/");

    List<NewService> services = EcwidConsulManager.createListenerServices("server-1", meta, null);

    assertEquals(1, services.size());
    assertEquals(14450, services.get(0).getPort());
    // UDP cannot be TCP-probed; the REST endpoint stands in as process liveness
    assertEquals("172.26.64.18:8080", services.get(0).getCheck().getTcp());
  }

  @Test
  void createListenerServices_noRestEndpointRegistersNothing() {
    Map<String, String> meta = Map.of("mqtt", "tcp://0.0.0.0:1883/");
    assertEquals(0, EcwidConsulManager.createListenerServices("server-1", meta, null).size());
    assertEquals(0, EcwidConsulManager.createListenerServices("server-1", null, null).size());
  }
}
