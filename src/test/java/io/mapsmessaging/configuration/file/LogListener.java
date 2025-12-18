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

package io.mapsmessaging.configuration.file;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import lombok.Getter;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LogListener extends AppenderBase<ILoggingEvent> {

  @Getter
  private final List<String> logMessages;

  public LogListener() {
    this.logMessages = new CopyOnWriteArrayList<>();
  }

  public void register(){
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

    // Create and configure your custom appender
    this.setContext(loggerContext);
    this.start();

    // Attach the appender to the root logger
    Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    rootLogger.addAppender(this);
  }

  @Override
  protected void append(ILoggingEvent eventObject) {
    // Extract details from the ILoggingEvent
    String timestamp = Instant.ofEpochMilli(eventObject.getTimeStamp())
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    String level = eventObject.getLevel().toString();

    String loggerName = eventObject.getLoggerName();
    String formattedMessage = eventObject.getFormattedMessage();

    // Build the formatted message string
    String message = String.format("[%s] %-5s %s (%s)", timestamp, level, formattedMessage, loggerName);
    logMessages.add(message);
  }
}
