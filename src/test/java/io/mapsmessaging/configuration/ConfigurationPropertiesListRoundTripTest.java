/*
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
 */
package io.mapsmessaging.configuration;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationPropertiesListRoundTripTest {

  @Test
  void preservesTrustedPeerAliasesWithOrderAndDuplicates() {
    List<String> aliases = List.of("server-b-current", "server-b-next", "server-b-current");
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("aliases", aliases);

    assertEquals(aliases, config.get("aliases"));
    assertEquals(aliases, config.getMap().get("aliases"));
    assertEquals(aliases, new ConfigurationProperties(config.getMap()).get("aliases"));
  }

  @Test
  void preservesScalarMixedAndEmptyLists() {
    ConfigurationProperties config = new ConfigurationProperties();
    List<Object> mixed = Arrays.asList("text", 42, 3.5, true, false, null, "text");
    config.put("mixed", mixed);
    config.put("empty", List.of());

    assertEquals(mixed, config.get("mixed"));
    assertEquals(mixed, config.getMap().get("mixed"));
    assertEquals(mixed, new ConfigurationProperties(config.getMap()).get("mixed"));
    assertEquals(List.of(), config.get("empty"));
    assertEquals(List.of(), config.getMap().get("empty"));
  }

  @Test
  void convertsMapsInObjectListsWithoutLosingOtherValues() {
    Map<String, Object> peer = new LinkedHashMap<>();
    peer.put("alias", "server-b-current");
    List<Object> entries = Arrays.asList(peer, "unmodified", 9, null);
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("entries", entries);

    List<?> internal = (List<?>) config.get("entries");
    assertInstanceOf(ConfigurationProperties.class, internal.get(0));
    assertEquals("unmodified", internal.get(1));
    assertEquals(entries, config.getMap().get("entries"));
    assertEquals(entries, new ConfigurationProperties(config.getMap()).getMap().get("entries"));
  }

  @Test
  void recursivelyConvertsAndPacksNestedMapsAndLists() {
    Map<String, Object> inner = new LinkedHashMap<>();
    inner.put("certificates", List.of("server-b-current", "server-b-next"));
    List<Object> nested = new ArrayList<>();
    nested.add(List.of(inner, "second"));
    nested.add(Arrays.asList(null, List.of(1, false, 1)));
    Map<String, Object> original = new LinkedHashMap<>();
    original.put("peers", nested);

    ConfigurationProperties config = new ConfigurationProperties(original);
    List<?> outer = (List<?>) config.get("peers");
    List<?> first = (List<?>) outer.get(0);
    assertInstanceOf(ConfigurationProperties.class, first.get(0));
    assertEquals(original, config.getMap());
    assertEquals(original, new ConfigurationProperties(config.getMap()).getMap());
  }

  @Test
  void putAllAndMapConstructorPreserveNullEntriesAndNestedLists() {
    Map<String, Object> source = new LinkedHashMap<>();
    source.put("values", Arrays.asList(null, List.of("alpha", "beta"), 7L, true));
    source.put("empty", List.of());

    ConfigurationProperties viaPutAll = new ConfigurationProperties();
    viaPutAll.putAll(source);
    ConfigurationProperties viaConstructor = new ConfigurationProperties(source);

    assertEquals(source, viaPutAll.getMap());
    assertEquals(source, viaConstructor.getMap());
    assertEquals(source, new ConfigurationProperties(viaPutAll.getMap()).getMap());
  }
}
