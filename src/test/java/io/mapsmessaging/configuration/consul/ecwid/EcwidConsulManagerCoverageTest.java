/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 */
package io.mapsmessaging.configuration.consul.ecwid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ecwid.consul.v1.agent.model.NewService;
import io.mapsmessaging.configuration.consul.Constants;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EcwidConsulManagerCoverageTest {

  @Test
  void createServiceUsesRestEndpointAndAdvertiseOverride() {
    Map<String, String> meta = Map.of(Constants.REST_API, "10.0.0.1:8080");

    NewService service = EcwidConsulManager.createService("node-1", meta, "192.0.2.10");

    assertEquals("node-1", service.getId());
    assertEquals(Constants.NAME, service.getName());
    assertEquals("192.0.2.10", service.getAddress());
    assertEquals(8080, service.getPort());
    assertEquals("192.0.2.10:8080", service.getCheck().getTcp());
  }

  @Test
  void createServiceRejectsMissingWildcardAndInvalidRestEndpoints() {
    assertThrows(
        IllegalArgumentException.class,
        () -> EcwidConsulManager.createService("node", null)
    );
    assertThrows(
        IllegalArgumentException.class,
        () -> EcwidConsulManager.createService(
            "node",
            Map.of(Constants.REST_API, "0.0.0.0:8080")
        )
    );
    assertThrows(
        IllegalArgumentException.class,
        () -> EcwidConsulManager.createService(
            "node",
            Map.of(Constants.REST_API, "not-an-endpoint")
        )
    );
  }

  @Test
  void listenerServicesFilterMetadataAndChooseCorrectHealthTargets() {
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put(Constants.REST_API, "10.0.0.1:8080");
    meta.put("mqtt", "tcp://0.0.0.0:1883/");
    meta.put("secure", "ssl://0.0.0.0:8883/");
    meta.put("websocket", "ws://0.0.0.0:9001/");
    meta.put("mavlink", "udp://0.0.0.0:14550/");
    meta.put("plain", "metadata");
    meta.put("invalid", "tcp://host:not-a-port");
    meta.put("missingPort", "tcp://host");

    List<NewService> services =
        EcwidConsulManager.createListenerServices("node", meta, "192.0.2.20");

    assertEquals(4, services.size());

    NewService mqtt = service(services, "maps-mqtt");
    assertEquals("192.0.2.20", mqtt.getAddress());
    assertEquals(1883, mqtt.getPort());
    assertEquals("192.0.2.20:1883", mqtt.getCheck().getTcp());

    NewService secure = service(services, "maps-secure");
    assertEquals("192.0.2.20:8883", secure.getCheck().getTcp());

    NewService websocket = service(services, "maps-websocket");
    assertEquals("192.0.2.20:9001", websocket.getCheck().getTcp());

    NewService mavlink = service(services, "maps-mavlink");
    assertEquals("192.0.2.20:8080", mavlink.getCheck().getTcp());
    assertEquals(List.of("mavlink", "node"), mavlink.getTags());
  }

  @Test
  void listenerServicesUseRestHostWhenAdvertiseAddressIsBlank() {
    Map<String, String> meta = Map.of(
        Constants.REST_API, "10.0.0.1:8080",
        "mqtt", "tcp://0.0.0.0:1883/"
    );

    List<NewService> services =
        EcwidConsulManager.createListenerServices("node", meta, " ");

    NewService mqtt = service(services, "maps-mqtt");
    assertEquals("10.0.0.1", mqtt.getAddress());
    assertEquals("10.0.0.1:1883", mqtt.getCheck().getTcp());
  }

  @Test
  void listenerServicesReturnEmptyForMissingMetadataOrRestEndpoint() {
    assertTrue(EcwidConsulManager.createListenerServices("node", null, null).isEmpty());
    assertTrue(
        EcwidConsulManager.createListenerServices(
            "node",
            Map.of("mqtt", "tcp://127.0.0.1:1883"),
            null
        ).isEmpty()
    );
  }

  private NewService service(List<NewService> services, String name) {
    return services.stream()
        .filter(service -> name.equals(service.getName()))
        .findFirst()
        .orElseThrow();
  }
}
