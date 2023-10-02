/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m;

import com.google.common.base.Preconditions;
import com.meta.cp4m.configuration.ConfigurationUtils;
import com.meta.cp4m.configuration.RootConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Main {

  static Path configurationFile(String[] strings) {
    int configKeyLocation = Arrays.binarySearch(strings, "--config");
    @Nullable String propertyConfigPath = System.getProperty("cp4m_configuration_file");
    @Nullable String envConfigPath = System.getenv("CP4M_CONFIGURATION_FILE");
    Path configPath;
    if (configKeyLocation >= 0) {
      int configPathLocation = configKeyLocation + 1;
      Preconditions.checkArgument(
          configPathLocation < strings.length,
          "A path to the location of the file must be given if --config is provided");
      configPath = Path.of(strings[configPathLocation]);
    } else if (propertyConfigPath != null) {
      configPath = Path.of(propertyConfigPath);
    } else if (envConfigPath != null) {
      configPath = Path.of(envConfigPath);
    } else {
      throw new IllegalArgumentException(
          "No configuration file found. A configuration file must be provided via the commandline argument '--config', via the system property cp4m_configuration_file, or via the environment variable CP4M_CONFIGURATION_FILE");
    }
    Preconditions.checkArgument(
        Files.exists(configPath), "given configuration at " + configPath + " does not exist");
    return configPath;
  }

  public static void main(String[] strings) throws IOException {
    Path configurationFile = configurationFile(strings);
    RootConfiguration configuration = ConfigurationUtils.loadConfigurationFile(configurationFile);
    configuration.toServicesRunner().start();
  }
}
