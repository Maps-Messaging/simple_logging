package io.mapsmessaging.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class SystemPropertiesTest {

  @Test
  void getProperty() throws IOException {
    System.setProperty("testLong", "10");
    System.setProperty("testBoolean", "true");
    System.setProperty("testDouble", "10.12");
    SystemProperties instance = SystemProperties.getInstance();
    Assertions.assertEquals(10, instance.getLongProperty("testLong", 0));
    Assertions.assertEquals(10.12, instance.getDoubleProperty("testDouble", 0));
    Assertions.assertTrue(instance.getBooleanProperty("testBoolean", false));
  }
}
