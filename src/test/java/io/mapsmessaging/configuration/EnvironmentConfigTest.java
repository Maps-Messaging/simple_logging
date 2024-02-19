package io.mapsmessaging.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class EnvironmentConfigTest {

  @Test
  void getProperty() throws IOException {
    File file = new File(".");
    EnvironmentConfig instance = EnvironmentConfig.getInstance();
    Assertions.assertEquals("", instance.translatePath("{{TEST}}"));
    EnvironmentPathLookup pathLookup = new EnvironmentPathLookup("TEST", file.getAbsolutePath(), false);
    instance.registerPath(pathLookup);
    Assertions.assertNotNull(instance.translatePath("{{TEST}}"));
    Assertions.assertEquals(file.getAbsolutePath()+File.separator, instance.translatePath("{{TEST}}"));
  }
}
