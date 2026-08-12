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
}
