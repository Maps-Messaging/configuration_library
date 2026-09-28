/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 */
package io.mapsmessaging.configuration.consul;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ConsulServerApiCoverageTest {

  @AfterEach
  void clearAgentProperty() {
    System.clearProperty("ConsulAgentRegister");
    System.clearProperty("ConsulUrl");
  }

  @Test
  void scanForDefaultConfigWalksUpNamespaces() {
    TestConsulServerApi api = new TestConsulServerApi("node");
    api.match = "a/b/default";

    assertEquals("a/b/default", api.scanForDefaultConfig("a/b/c"));
    assertEquals(List.of("a/b/c/default", "a/b/default"), api.lookups);
  }

  @Test
  void scanForDefaultConfigReturnsEmptyAtRootAndOnLookupFailure() {
    TestConsulServerApi api = new TestConsulServerApi("node");
    assertEquals("", api.scanForDefaultConfig("single"));

    api.failLookups = true;
    assertEquals("", api.scanForDefaultConfig("a/b"));
  }

  @Test
  void runOnlyPingsWhenAgentRegistrationIsEnabled() {
    TestConsulServerApi api = new TestConsulServerApi("node");

    System.setProperty("ConsulAgentRegister", "false");
    api.run();
    assertEquals(0, api.pings);

    System.setProperty("ConsulAgentRegister", "true");
    api.run();
    assertEquals(1, api.pings);
  }

  @Test
  void stopCancelsExistingScheduledTask() {
    TestConsulServerApi api = new TestConsulServerApi("node");
    CompletableFuture<Void> task = new CompletableFuture<>();
    api.setScheduledTask(task);

    api.stop();

    assertTrue(task.isCancelled());
  }

  @Test
  void validateKeyPreservesValidAndSanitizesInvalidKeys() {
    TestConsulServerApi api = new TestConsulServerApi("node");

    assertEquals("valid/key-1.test", api.validate("valid/key-1.test"));
    assertEquals("invalidkey", api.validate("invalid key!"));
  }

  @Test
  void resolveLocalAddressRejectsMalformedAndMissingHosts() {
    assertThrows(IOException.class, () -> ConsulServerApi.resolveLocalAddress("http://["));
    assertThrows(IOException.class, () -> ConsulServerApi.resolveLocalAddress("file:/tmp/consul"));
  }

  @Test
  void resolveLocalAddressHandlesExplicitAndDefaultPorts() throws Exception {
    InetAddress explicit =
        ConsulServerApi.resolveLocalAddress("http://127.0.0.1:8500");
    InetAddress defaultHttp =
        ConsulServerApi.resolveLocalAddress("http://127.0.0.1");
    InetAddress defaultHttps =
        ConsulServerApi.resolveLocalAddress("https://127.0.0.1");

    assertNotNull(explicit);
    assertNotNull(defaultHttp);
    assertNotNull(defaultHttps);
    assertFalse(explicit.isAnyLocalAddress());
  }

  @Test
  void constructorInitializesServiceIdAndUrlPath() {
    TestConsulServerApi api = new TestConsulServerApi("node");

    assertEquals(List.of("node"), api.serviceIds());
    assertEquals("/", api.getUrlPath());
  }

  private static final class TestConsulServerApi extends ConsulServerApi {
    private final List<String> lookups = new ArrayList<>();
    private String match;
    private boolean failLookups;
    private int pings;

    private TestConsulServerApi(String name) {
      super(name);
    }

    private String validate(String key) {
      return validateKey(key);
    }

    private List<String> serviceIds() {
      return List.copyOf(serviceIds);
    }

    private void setScheduledTask(java.util.concurrent.Future<?> task) {
      scheduledTask = task;
    }

    @Override
    protected void pingService() {
      pings++;
    }

    @Override
    public void register(Map<String, String> meta) {
    }

    @Override
    public List<String> getKeys(String key) throws IOException {
      lookups.add(key);
      if (failLookups) {
        throw new IOException("lookup failed");
      }
      return key.equals(match) ? List.of(key) : List.of();
    }

    @Override
    public String getValue(String key) {
      return null;
    }

    @Override
    public void putValue(String key, String value) {
    }

    @Override
    public void deleteKey(String key) {
    }
  }
}
