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

package io.mapsmessaging.configuration.parsers;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.mapsmessaging.configuration.SystemProperties;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class JsonParser {

  protected JsonObject json;

  protected JsonParser() {
  }

  public JsonParser(JsonObject json) {
    this.json = json;
  }

  public JsonObject getJson() {
    return json;
  }

  public Map<String, Object> parse() {
    Type type = new TypeToken<LinkedHashMap<String, Object>>() {}.getType();
    Map<String, Object> result = SystemProperties.getInstance().getGson().fromJson(json, type);
    return removeUnnecessaryLists(result);
  }

  private Map<String, Object> removeUnnecessaryLists(Map<String, Object> map) {
    for (Entry<String, Object> entry : map.entrySet()) {
      if (entry.getValue() instanceof List) {
        List<Object> list = objectToList(entry.getValue());
        if (list.size() == 1) {
          entry.setValue(list.remove(0));
        }
      }
      if (entry.getValue() instanceof Map) {
        entry.setValue(removeUnnecessaryLists(objectToMap(entry.getValue())));
      }
    }
    return map;
  }


  protected List<Object> objectToList(Object list) {
    return (List<Object>) list;
  }

  protected Map<String, Object> objectToMap(Object list) {
    return (Map<String, Object>) list;
  }
}
