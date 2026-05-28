package hu.bodiadam.rpg;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public final class LocalSecrets {
    private LocalSecrets() {
    }

    public static String require(String propertyKey, String envKey) {
        String envValue = normalizeSecret(System.getenv(envKey));
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        Properties properties = loadLocalProperties();
        String propertyValue = normalizeSecret(properties.getProperty(propertyKey));
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        throw new IllegalStateException(
                "Missing secret. Set environment variable " + envKey
                        + " or create ai-integration/local.properties from local.properties.");
    }

    private static Properties loadLocalProperties() {
        Properties properties = new Properties();
        for (Path candidate : localPropertiesCandidates()) {
            if (!Files.exists(candidate)) {
                continue;
            }

            try (InputStream inputStream = Files.newInputStream(candidate)) {
                properties.load(inputStream);
                return properties;
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read " + candidate.toAbsolutePath(), e);
            }
        }

        return properties;
    }

    private static List<Path> localPropertiesCandidates() {
        Set<Path> candidates = new LinkedHashSet<>();

        candidates.add(Path.of("local.properties"));
        candidates.add(Path.of("ai-integration", "local.properties"));

        Path moduleDir = detectModuleDirectory();
        if (moduleDir != null) {
            candidates.add(moduleDir.resolve("local.properties"));
        }

        return List.copyOf(candidates);
    }

    private static Path detectModuleDirectory() {
        try {
            Path codeSource = Path.of(LocalSecrets.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());

            if (Files.isDirectory(codeSource)) {
                Path parent = codeSource.getParent();
                return parent == null ? null : parent.getParent();
            }

            Path parent = codeSource.getParent();
            return parent == null ? null : parent.getParent();
        } catch (URISyntaxException | NullPointerException e) {
            return null;
        }
    }

    private static String normalizeSecret(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.length() >= 2) {
            boolean doubleQuoted = trimmed.startsWith("\"") && trimmed.endsWith("\"");
            boolean singleQuoted = trimmed.startsWith("'") && trimmed.endsWith("'");
            if (doubleQuoted || singleQuoted) {
                return trimmed.substring(1, trimmed.length() - 1).trim();
            }
        }

        return trimmed;
    }
}
