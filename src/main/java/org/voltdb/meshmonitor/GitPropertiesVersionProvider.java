/*
 * Copyright (C) 2024-2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor;

import picocli.CommandLine;

import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

public class GitPropertiesVersionProvider implements CommandLine.IVersionProvider {

    public static final String GIT_PROPERTIES_FILENAME = "git.properties";

    public static final String GIT_TAGS_PROPERTY = "git.tags";
    public static final String GIT_BUILD_TIME_PROPERTY = "git.build.time";
    public static final String GIT_COMMIT_ID_PROPERTY = "git.commit.id.abbrev";

    public static final String VERSION_UNKNOWN = "unknown";
    public static final String VERSION_PREFIX = "${COMMAND-FULL-NAME} version ";

    private final Supplier<InputStream> propertiesFileLoader;

    // Visible for testing
    public GitPropertiesVersionProvider(Supplier<InputStream> propertiesFileLoader) {
        this.propertiesFileLoader = propertiesFileLoader;
    }

    public GitPropertiesVersionProvider() {
        propertiesFileLoader = () ->
                GitPropertiesVersionProvider.class
                        .getClassLoader()
                        .getResourceAsStream(GIT_PROPERTIES_FILENAME);
    }

    public String[] getVersion() {
        String simpleVersion = getSimpleVersion();
        return new String[]{VERSION_PREFIX + simpleVersion};
    }

    public String getSimpleVersion() {
        try {
            Properties properties = new Properties();
            InputStream inStream = propertiesFileLoader.get();
            if (inStream == null) {
                return VERSION_UNKNOWN;
            }

            properties.load(inStream);

            String maybeTags = Objects.toString(properties.get(GIT_TAGS_PROPERTY), "");
            String maybeBuildTime = Objects.toString(properties.get(GIT_BUILD_TIME_PROPERTY), "");
            String maybeCommitId = Objects.toString(properties.get(GIT_COMMIT_ID_PROPERTY), "");

            StringBuilder version = new StringBuilder();
            if (!maybeTags.trim().isEmpty()) {
                version.append(maybeTags);
            } else if (!maybeCommitId.trim().isEmpty()) {
                version.append(maybeCommitId);
            } else {
                version.append(VERSION_UNKNOWN);
            }

            if (!maybeBuildTime.trim().isEmpty()) {
                version.append(" built on ").append(maybeBuildTime);
            }

            return version.toString();
        } catch (Exception e) {
            return VERSION_UNKNOWN;
        }
    }
}
