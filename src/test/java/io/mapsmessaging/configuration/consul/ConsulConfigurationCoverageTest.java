/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 */
package io.mapsmessaging.configuration.consul;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ConsulConfigurationCoverageTest {

  private final Map<String, String> original = new HashMap<>();

  @AfterEach
  void restoreProperties() {
    for (String key : original.keySet()) {
      String value = original.get(key);
      if (value == null) {
        System.clearProperty(key);
      } else {
        System.setProperty(key, value);
      }
    }
  }

  @Test
  void defaultsUseLoopbackAndStandardPort() {
    clear("ConsulUrl");
    clear("ConsulHost");
    clear("ConsulPort");
    clear("ConsulToken");
    clear("ConsulPath");
    clear("ConsulAcl");
    clear("ConsulAgentRegister");
    clear("ConsulServiceAddress");

    ConsulConfiguration configuration = new ConsulConfiguration();

    assertEquals("http://127.0.0.1:8500", configuration.getConsulUrl());
    assertEquals("/", configuration.getUrlPath());
    assertNull(configuration.getConsulToken());
    assertNull(configuration.getConsulAcl());
    assertNull(configuration.getServiceAddress());
    assertFalse(configuration.registerAgent());
  }

  @Test
  void explicitHostPortAclPathServiceAddressAndAgentFlagAreApplied() {
    clear("ConsulUrl");
    set("ConsulHost", "consul.internal");
    set("ConsulPort", "9500");
    set("ConsulAcl", "acl-value");
    set("ConsulPath", "/override");
    set("ConsulServiceAddress", " 192.0.2.20 ");
    set("ConsulAgentRegister", "true");

    ConsulConfiguration configuration = new ConsulConfiguration();

    assertEquals("http://consul.internal:9500", configuration.getConsulUrl());
    assertEquals("acl-value", configuration.getConsulAcl());
    assertEquals("/override", configuration.getUrlPath());
    assertEquals("192.0.2.20", configuration.getServiceAddress());
    assertTrue(configuration.registerAgent());
  }

  @Test
  void urlTokenAndPathAreExtractedAndRemovedFromBaseUrl() {
    set("ConsulUrl", "https://secret@example.com:8443/config/path");
    clear("ConsulToken");
    clear("ConsulPath");

    ConsulConfiguration configuration = new ConsulConfiguration();

    assertEquals("https://example.com:8443", configuration.getConsulUrl());
    assertEquals("secret", configuration.getConsulToken());
    assertEquals("/config/path", configuration.getUrlPath());
  }

  @Test
  void tokenPropertyOverridesUrlTokenAndWhitespaceBecomesNull() {
    set("ConsulUrl", "http://embedded@example.com:8500/path");
    set("ConsulToken", " explicit ");

    ConsulConfiguration configuration = new ConsulConfiguration();

    assertEquals("explicit", configuration.getConsulToken());

    set("ConsulToken", "   ");
    ConsulConfiguration blankToken = new ConsulConfiguration();
    assertNull(blankToken.getConsulToken());
  }

  @Test
  void malformedUrlFallsBackToOriginalTextAndRootPath() {
    set("ConsulUrl", "http://[");

    ConsulConfiguration configuration = new ConsulConfiguration();

    assertEquals("http://[", configuration.getConsulUrl());
    assertEquals("/", configuration.getUrlPath());
  }

  @Test
  void urlWithoutExplicitPortRemovesPathCleanly() {
    set("ConsulUrl", "https://example.com/config");

    ConsulConfiguration configuration = new ConsulConfiguration();

    assertEquals("https://example.com", configuration.getConsulUrl());
    assertEquals("/config", configuration.getUrlPath());
  }

  @Test
  void blankServiceAddressIsIgnored() {
    set("ConsulServiceAddress", "   ");

    ConsulConfiguration configuration = new ConsulConfiguration();

    assertNull(configuration.getServiceAddress());
  }

  private void set(String key, String value) {
    remember(key);
    System.setProperty(key, value);
  }

  private void clear(String key) {
    remember(key);
    System.clearProperty(key);
  }

  private void remember(String key) {
    original.putIfAbsent(key, System.getProperty(key));
  }
}
