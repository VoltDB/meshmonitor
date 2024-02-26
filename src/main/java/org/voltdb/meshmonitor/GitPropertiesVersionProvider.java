package org.voltdb.meshmonitor;

import picocli.CommandLine;

import java.io.IOException;
import java.util.Properties;

public class GitPropertiesVersionProvider implements CommandLine.IVersionProvider {

    public static final String GIT_PROPERTIES_FILENAME = "git.properties";

    public static final String GIT_TAGS_PROPERTY = "git.tags";
    public static final String GIT_BUILD_TIME_PROPERTY = "git.build.time";
    public static final String GIT_COMMIT_ID_PROPERTY = "git.commit.id.abbrev";

    public static final String VERSION_UNKNOWN = "unknown";
    public static final String VERSION_PREFIX = "${COMMAND-FULL-NAME} version ";

    public String[] getVersion() {
        String simpleVersion = getSimpleVersion();
        return new String[]{VERSION_PREFIX + simpleVersion};
    }

    public static String getSimpleVersion() {
        try {
            Properties properties = new Properties();

            properties.load(
                    GitPropertiesVersionProvider.class.getClassLoader().getResourceAsStream(GIT_PROPERTIES_FILENAME)
            );

            Object maybeTags = properties.get(GIT_TAGS_PROPERTY);
            Object maybeBuildTime = properties.get(GIT_BUILD_TIME_PROPERTY);
            Object maybeCommitId = properties.get(GIT_COMMIT_ID_PROPERTY);

            StringBuilder version = new StringBuilder();
            if (maybeTags != null) {
                version.append(maybeTags);
            } else if (maybeCommitId != null) {
                version.append(maybeCommitId);
            }

            if (maybeBuildTime != null) {
                version.append(" built on ").append(maybeBuildTime);
            }

            return version.toString();
        } catch (IOException e) {
            return VERSION_UNKNOWN;
        }
    }
}
