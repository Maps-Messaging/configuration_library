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

package io.mapsmessaging.configuration.yaml;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.mapsmessaging.configuration.SystemProperties;
import io.mapsmessaging.configuration.parsers.JsonParser;


import java.lang.reflect.Type;
import java.util.Map;

public class YamlParser extends JsonParser {

  public YamlParser(Object mapStructure) {
    json = convertToJson(mapStructure);
  }

  private JsonObject convertToJson(Object yamlLoad) {
    if (yamlLoad instanceof Map) {
      Map<String, Object> map = objectToMap(yamlLoad);
      Type type = new TypeToken<Map<String, Object>>() {}.getType();
      JsonElement element = SystemProperties.getInstance().getGson().toJsonTree(map, type);
      return element.getAsJsonObject();

    }
    return new JsonObject();
  }

}
