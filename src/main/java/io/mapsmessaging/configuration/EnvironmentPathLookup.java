package io.mapsmessaging.configuration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EnvironmentPathLookup {
  private final String name;
  private final String defaultPath;
  private final boolean create;
}
