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
package io.mapsmessaging.logging;

import lombok.Getter;

/**
 * This is a simple implementation, to use the logging framework, simply take this
 * class, rename it and then add your message enums ( Remove the TRACE,DEBUG ones, since they are simply for testing)
 *
 * The enum is simply, the log level for the message, an arbitrary category that makes sense to your application and then the log test itself
 */
@Getter
public enum ConfigLogMessages implements LogMessage {
  //<editor-fold desc="System and Environment property access">
  CONFIG_PROPERTY_ACCESS(LEVEL.ERROR, CONFIG_CATEGORY.CONFIGURATION, "Getting property {} from system resulted in {}"),
  PROPERTY_MANAGER_START(LEVEL.DEBUG, CONFIG_CATEGORY.CONFIGURATION, "Starting Property Manager"),
  PROPERTY_MANAGER_FOUND(LEVEL.DEBUG, CONFIG_CATEGORY.CONFIGURATION, "Found and loaded property {}"),
  PROPERTY_MANAGER_LOOKUP(LEVEL.DEBUG, CONFIG_CATEGORY.CONFIGURATION, "Looking failed for {} config"),
  PROPERTY_MANAGER_LOOKUP_FAILED(LEVEL.DEBUG, CONFIG_CATEGORY.CONFIGURATION, "Looking for {} config, found in {}"),
  PROPERTY_MANAGER_SCANNING(LEVEL.DEBUG, CONFIG_CATEGORY.CONFIGURATION, "Scanning property with {} entries"),
  PROPERTY_MANAGER_INDEX_DETECTED(LEVEL.DEBUG, CONFIG_CATEGORY.CONFIGURATION, "Detected an indexed property file, parsing into different properties"),
  PROPERTY_MANAGER_COMPLETED_INDEX(LEVEL.DEBUG, CONFIG_CATEGORY.CONFIGURATION, "Completed indexed property with {} for index {}"),
  PROPERTY_MANAGER_SCAN_FAILED(LEVEL.WARN, CONFIG_CATEGORY.CONFIGURATION, "Failed to scan for property files"),
  PROPERTY_MANAGER_LOAD_FAILED(LEVEL.WARN, CONFIG_CATEGORY.CONFIGURATION, "Failed to load property {}"),
  PROPERTY_MANAGER_ENTRY_LOOKUP(LEVEL.DEBUG, CONFIG_CATEGORY.CONFIGURATION, "Lookup for {} found {} in {}"),
  PROPERTY_MANAGER_ENTRY_LOOKUP_FAILED(LEVEL.DEBUG, CONFIG_CATEGORY.CONFIGURATION, "Lookup for {} not found, returning default {}"),
  PROPERTY_SMART_QUOTES_DETECTED(LEVEL.FATAL, CONFIG_CATEGORY.CONFIGURATION, "Detected smart quotes for key {}, please use normal quotes for for strings, converted {} to {}"),

  //</editor-fold>

  //<editor-fold desc="CONSUL agent logging">
  CONSUL_STARTUP(LEVEL.DEBUG, CONFIG_CATEGORY.CONFIGURATION, "Agent startup"),
  CONSUL_CLIENT_LOG(LEVEL.WARN, CONFIG_CATEGORY.CONFIGURATION, "Consul Client state {} with config {}"),
  CONSUL_CLIENT_EXCEPTION(LEVEL.WARN, CONFIG_CATEGORY.CONFIGURATION, "Consul Client raised exception {}"),

  CONSUL_SHUTDOWN(LEVEL.DEBUG, CONFIG_CATEGORY.CONFIGURATION, "Agent shutdown"),
  CONSUL_REGISTER(LEVEL.DEBUG, CONFIG_CATEGORY.CONFIGURATION, "Registering with local agent"),
  CONSUL_PING_EXCEPTION(LEVEL.DEBUG, CONFIG_CATEGORY.CONFIGURATION, "Ping failed with exception {}"),
  //</editor-fold>

  //<editor-fold desc="CONSUL management log messages">
  CONSUL_MANAGER_START(LEVEL.INFO, CONFIG_CATEGORY.CONFIGURATION, "Manager starting up for id {}"),
  CONSUL_MANAGER_STOP(LEVEL.INFO, CONFIG_CATEGORY.CONFIGURATION, "Manager shutting down"),
  CONSUL_KEY_VALUE_MANAGER(LEVEL.ERROR, CONFIG_CATEGORY.CONFIGURATION, "Consul Key/Value, Action:{}, Key: \"{}\""),
  CONSUL_INVALID_KEY(LEVEL.ERROR, CONFIG_CATEGORY.CONFIGURATION, "Consul Key/Value, invalid key received {}, changed to {}"),
  CONSUL_MANAGER_START_ABORTED(LEVEL.ERROR, CONFIG_CATEGORY.CONFIGURATION, "Startup aborted due to configuration, id {}"),
  CONSUL_MANAGER_START_DELAYED(LEVEL.ERROR, CONFIG_CATEGORY.CONFIGURATION, "Startup delaying server startup due to configuration for id {}"),
  CONSUL_MANAGER_START_SERVER_NOT_FOUND(LEVEL.ERROR, CONFIG_CATEGORY.CONFIGURATION, "Startup aborted since Consul Server is not responding, id {}"),
  //</editor-fold>

  //<editor-fold desc="AWS Key/Value management log messages">
  REMOTE_PROPERTY_MANAGER_NO_KEY_VALUES(LEVEL.ERROR, CONFIG_CATEGORY.CONFIGURATION, "No keys found in AWS Key/Value for id {}"),
  REMOTE_PROPERTY_MANAGER_KEY_LOOKUP_EXCEPTION(LEVEL.ERROR, CONFIG_CATEGORY.CONFIGURATION, "Key {}, lookup failed with exception"),
  REMOTE_PROPERTY_MANAGER_KEY_LOOKUP_SUCCESS(LEVEL.INFO, CONFIG_CATEGORY.CONFIGURATION, "Key {}, lookup success, returned {} bytes"),

  REMOTE_PROPERTY_MANAGER_INVALID_JSON(LEVEL.ERROR, CONFIG_CATEGORY.CONFIGURATION, "Value returned is not valid json for key {}"),
  REMOTE_PROPERTY_MANAGER_SAVE_ALL(LEVEL.ERROR, CONFIG_CATEGORY.CONFIGURATION, "Saving all entries for {}"),
  REMOTE_PROPERTY_MANAGER_STORE(LEVEL.ERROR, CONFIG_CATEGORY.CONFIGURATION, "Storing entry for {}"),
  //</editor-fold>
  ;

  private final String message;
  private final LEVEL level;
  private final Category category;
  private final int parameterCount;

  ConfigLogMessages(LEVEL level, Category category, String message) {
    this.message = message;
    this.level = level;
    this.category = category;
    int location = message.indexOf("{}");
    int count = 0;
    while (location != -1) {
      count++;
      location = message.indexOf("{}", location + 2);
    }
    this.parameterCount = count;
  }

  @Getter
  public enum CONFIG_CATEGORY implements Category {
    CONFIGURATION("Config");

    public final String description;

    public String getDivision(){
      return "Test";
    }

    CONFIG_CATEGORY(String description) {
      this.description = description;
    }
  }

}
