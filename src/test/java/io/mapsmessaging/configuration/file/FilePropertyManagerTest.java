package io.mapsmessaging.configuration.file;

import io.mapsmessaging.configuration.PropertyManager;
import io.mapsmessaging.configuration.PropertyManagerTest;

class FilePropertyManagerTest extends PropertyManagerTest {

  @Override
  protected PropertyManager create() {
    return new FileYamlPropertyManager();
  }

}