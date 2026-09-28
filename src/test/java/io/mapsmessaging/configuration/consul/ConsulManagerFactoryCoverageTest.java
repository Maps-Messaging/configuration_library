/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 */
package io.mapsmessaging.configuration.consul;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ConsulManagerFactoryCoverageTest {

  private final ConsulManagerFactory factory = ConsulManagerFactory.getInstance();

  @AfterEach
  void resetManager() throws Exception {
    setManager(null);
  }

  @Test
  void singletonIsStableAndStartsEmpty() throws Exception {
    setManager(null);

    assertSame(factory, ConsulManagerFactory.getInstance());
    assertFalse(factory.isStarted());
    assertNull(factory.getManager());
    assertNull(factory.getPath());

    factory.stop();
    assertFalse(factory.isStarted());
  }

  @Test
  void accessorsReflectInjectedManagerAndStopDelegates() throws Exception {
    FakeConsulServerApi manager = new FakeConsulServerApi();
    setManager(manager);

    assertTrue(factory.isStarted());
    assertSame(manager, factory.getManager());
    assertEqualsPath("/", factory.getPath());

    factory.stop();

    assertTrue(manager.stopped);
  }

  private void setManager(ConsulServerApi manager) throws Exception {
    Field field = ConsulManagerFactory.class.getDeclaredField("manager");
    field.setAccessible(true);
    field.set(factory, manager);
  }

  private void assertEqualsPath(String expected, String actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
  }

  private static final class FakeConsulServerApi extends ConsulServerApi {
    private boolean stopped;

    private FakeConsulServerApi() {
      super("fake");
    }

    @Override
    public void stop() {
      stopped = true;
    }

    @Override
    protected void pingService() {
    }

    @Override
    public void register(Map<String, String> meta) {
    }

    @Override
    public List<String> getKeys(String key) {
      return List.of();
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
