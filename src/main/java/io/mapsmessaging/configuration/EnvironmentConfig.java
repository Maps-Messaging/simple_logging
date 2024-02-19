/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.configuration;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@SuppressWarnings("java:S6548") // yes it is a singleton
public class EnvironmentConfig {
  private static class Holder {
    static final EnvironmentConfig INSTANCE = new EnvironmentConfig();
  }
  public static EnvironmentConfig getInstance() {
    return EnvironmentConfig.Holder.INSTANCE;
  }
  private final Logger logger = LoggerFactory.getLogger(EnvironmentConfig.class);

  private final Map<String, String> pathLookups;
  private final Map<String, File> pathLocations;

  private EnvironmentConfig() {
    pathLocations = new LinkedHashMap<>();
    pathLookups = new LinkedHashMap<>();
  }

  public void clearAll(){
    pathLocations.clear();
    pathLookups.clear();
  }

  public boolean registerPath(EnvironmentPathLookup pathConfig) throws IOException {
    String path = loadAndCreatePath(pathConfig.getName(), pathConfig.getDefaultPath(), pathConfig.isCreate());
    if(path != null) {
      pathLookups.put(pathConfig.getName(), path);
      pathLocations.put(pathConfig.getName(), new File(path));
      return true;
    }
    return false;
  }

  private String loadAndCreatePath(String name, String defaultPath, boolean create) throws IOException {
    String directoryPath = SystemProperties.getInstance().locateProperty(name, defaultPath);
    directoryPath = translatePath(directoryPath);
    File testPath = new File(directoryPath);
    if (!testPath.exists()) {
      if (create) {
        Files.createDirectories(testPath.toPath());
      }
      else{
        return null;
      }
    }
    if (!directoryPath.endsWith(File.separator)) {
      directoryPath = directoryPath + File.separator;
    }
    return directoryPath;
  }


  public String translatePath(String path) {
    String updated = path;
    for(Map.Entry<String, String> entry:pathLookups.entrySet()){
      updated = updated.replace("{{"+entry.getKey()+"}}", entry.getValue());
    }

    while (updated.contains("{{") && updated.contains("}}")) {
      updated = scanForNonStandardSub(updated);
    }
    while (updated.contains("//")) {
      updated = updated.replace("//", File.separator);
    }
    while (updated.contains("\\\\")) {
      updated = updated.replace("\\\\", File.separator);
    }
    while (updated.contains("/\\")) {
      updated = updated.replace("/\\", File.separator);
    }
    while (updated.contains("\\/")) {
      updated = updated.replace("\\/", File.separator);
    }
    return updated;
  }

  public String scanForNonStandardSub(String path) {
    if(path.contains("{{") && path.contains("}}")) {
      int start = path.indexOf("{{");
      int end = path.indexOf("}}");
      String env = path.substring(start + 2, end);
      String located = SystemProperties.getInstance().locateProperty(env, "");
      path = path.replace("{{" + env + "}}", located);
      while (path.contains("\"")) {
        path = path.replace("\"", "");
      }
    }
    return path;
  }

}
