/*
 *     Copyright [2020 - 2024]   [Matthew Buckton]
 *     Copyright [2024 - 2024]   [Maps Messaging B.V.]
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *
 */

package io.mapsmessaging.configuration.consul;

import io.mapsmessaging.configuration.SystemProperties;
import io.mapsmessaging.configuration.consul.ecwid.EcwidConsulManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.locks.LockSupport;

import static io.mapsmessaging.logging.ConfigLogMessages.*;

@SuppressWarnings("java:S6548") // yes it is a singleton
public class ConsulManagerFactory {
  private static class Holder {
    static final ConsulManagerFactory INSTANCE = new ConsulManagerFactory();
  }

  public static ConsulManagerFactory getInstance() {
    return Holder.INSTANCE;
  }

  private final Logger logger = LoggerFactory.getLogger(ConsulManagerFactory.class);
  private final boolean forceWait;
  private ConsulServerApi manager;
  private ConsulManagerFactory() {
    boolean config;
    try {
      config = SystemProperties.getInstance().getBooleanProperty("ForceConsul", false);
    } catch (Exception e) {
      config = false;
    }
    forceWait = config;
    manager = null;
  }

  public synchronized void start(String serverId) {
    stop(); // just to be sure
    logger.log(CONSUL_MANAGER_START, serverId);
    boolean retry = true;
    int counter = 0;
    while (retry && counter < Constants.RETRY_COUNT) {
      try {
        manager = constructManager(serverId);
        retry = false;
      } catch (IOException io) {
        logger.log(CONSUL_MANAGER_START_ABORTED, serverId, io);
        retry = false;
      } catch (Exception e) {
        LockSupport.parkNanos(1000000000L);
        counter++;
        if (!forceWait && e instanceof IOException) {
          Exception actual = (Exception) e.getCause();
          if (actual instanceof ConnectException) {
            logger.log(CONSUL_MANAGER_START_SERVER_NOT_FOUND, serverId);
          } else {
            logger.log(CONSUL_MANAGER_START_ABORTED, serverId, e);
          }
          return;
        }
        logger.log(CONSUL_MANAGER_START_DELAYED, serverId);
      }
    }
  }

  private ConsulServerApi constructManager(String serverId) throws IOException {
    return new EcwidConsulManager(serverId);
  }

  public String getPath() {
    if (manager != null) {
      return manager.getUrlPath();
    }
    return null;
  }


  public synchronized void stop() {
    if (manager != null) {
      logger.log(CONSUL_MANAGER_STOP);
      manager.stop();
    }
  }

  public synchronized ConsulServerApi getManager() {
    return manager;
  }

  public synchronized boolean isStarted() {
    return manager != null;
  }

}
