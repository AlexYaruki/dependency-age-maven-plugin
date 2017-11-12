package com.github.alexyaruki.pda;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class for generating dependency age information per dependency specified in pom.xml.
 */
final class InfoGenerator {

    /**
     * Hidden default constructor.
     */
    private InfoGenerator() {
    }

    /**
     * Generates map of dependency name (groupId:artifactId:version) to textual
     * description of its age.
     *
     * @param project      current Maven project
     * @param log          current logger
     * @param ignoreString which string to ignore in dependency groupId or artifactId
     * @return map of infos (name -> textual description of its age)
     */
    static Map<String, String> generateInfoMap(final MavenProject project, final Log log, final String ignoreString) {
        final Map<String, String> pdaInfo = new HashMap<>();
        project.getDependencies()
            .stream()
            .filter(dependency -> {
                if (ignoreString != null && ignoreString.length() != 0) {
                    if (dependency.getGroupId().contains(ignoreString) || dependency.getArtifactId().contains(ignoreString)) {
                        return false;
                    }
                    return true;
                }
                return true;
            })
            .map(dependency -> dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion())
            .map(name -> {
                final String[] nameParts = name.split(":");
                final long timestamp = downloadTimestamp(log, nameParts[0], nameParts[1], nameParts[2]);
                if (log.isDebugEnabled()) {
                    log.debug(name + " -> " + timestamp + " ms");
                }
                return createEntry(name, timestamp);
            })
            .sorted(Comparator.comparingLong(Map.Entry::getValue))
            .map(entry -> createEntry(entry.getKey(), generateInfo(entry.getValue())))
            .forEach((entry) -> pdaInfo.put(entry.getKey(), entry.getValue()));
        return pdaInfo;
    }

    /**
     * Generates map of dependencies to theirs timestamps representing date that artifact was deployed to Maven Central.
     *
     * @param project      current Maven project
     * @param log          current Maven logger
     * @param ignoreString which string to ignore in dependency groupId or artifactId
     * @return timestamp map
     */
    static Map<Dependency, Long> generateTimestampMap(final MavenProject project, final Log log, final String ignoreString) {
        final Map<Dependency, Long> pdaInfo = new LinkedHashMap<>();
        project.getDependencies()
            .stream()
            .filter(dependency -> {
                if (ignoreString != null && ignoreString.length() != 0) {
                    if (dependency.getGroupId().contains(ignoreString) || dependency.getArtifactId().contains(ignoreString)) {
                        return false;
                    }
                    return true;
                }
                return true;
            })
            .map(dependency -> {
                final long timestamp = downloadTimestamp(log, dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
                if (log.isDebugEnabled()) {
                    log.debug(dependency.toString() + " -> " + timestamp + " ms");
                }
                return createEntry(dependency, timestamp);
            })
            .sorted(Comparator.comparingLong(Map.Entry::getValue))
            .forEach((entry) -> pdaInfo.put(entry.getKey(), entry.getValue()));
        return pdaInfo;
    }

    /**
     * Utility method for creating Map.Entry objects.
     *
     * @param <K>   type of key
     * @param key   key of entry
     * @param <V>   type of value
     * @param value value of entry
     * @return Map.Entry object of key to value
     */
    private static <K, V> Map.Entry<K, V> createEntry(final K key, final V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    /**
     * Downloads timestamp of dependency.
     *
     * @param log      Maven logger instance
     * @param group    groupId of dependency
     * @param artifact artifactId of dependency
     * @param version  version of dependency
     * @return timestamp of dependency
     */
    private static long downloadTimestamp(final Log log, final String group, final String artifact, final String version) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpGet request = new HttpGet("http://search.maven.org/solrsearch/select?q=g%3A%22" + group + "%22+AND+a%3A%22" + artifact + "%22&core=gav&rows=1000&wt=json");
            if (log.isDebugEnabled()) {
                log.debug("GET - > " + request.getURI().toString());
            }
            final ByteArrayOutputStream jsonStream = new ByteArrayOutputStream();
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    response.getEntity().writeTo(jsonStream);
                } else {
                    return -1;
                }
            }
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode root = mapper.readTree(jsonStream.toString(Charset.defaultCharset().name()));
            final JsonNode docs = root.get("response").get("docs");
            long timestamp = 0;
            for (final JsonNode doc : docs) {

                if (doc.get("v").asText().equals(version)) {
                    if (log.isDebugEnabled()) {
                        log.debug("timestamp(RAW) - > " + doc.get("timestamp"));
                    }
                    timestamp = Long.parseLong(doc.get("timestamp").asText());
                    if (log.isDebugEnabled()) {
                        log.debug("timestamp(Parsed) - > " + timestamp);
                    }
                    break;
                }
            }

            return timestamp;
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * Generate dependency age info from it's timestamp.
     *
     * @param timestamp - timestamp of dependency
     * @return dependency age info
     */
    static String generateInfo(final long timestamp) {
        if (timestamp == -1) {
            return "Maven Central HTTP Error - Try again ?";
        }

        return generateInfoString(timestamp);
    }

    /**
     * Generates dependency age info from it's checked timestamp.
     *
     * @param timestamp - timestamp of dependency
     * @return dependency age info
     */
    private static String generateInfoString(final long timestamp) {
        final long timeDifference = Instant.now().toEpochMilli() - timestamp;
        final DurationSplitter durationSplitter = new DurationSplitter(timeDifference);
        final StringBuilder infoStringBuilder = new StringBuilder(50);
        infoStringBuilder
            .append(durationSplitter.getYears()).append(" years, ")
            .append(durationSplitter.getDays()).append(" days, ")
            .append(durationSplitter.getHours()).append(" hours, ")
            .append(durationSplitter.getMinutes()).append(" minutes, ")
            .append(durationSplitter.getSeconds()).append(" seconds");
        return infoStringBuilder.toString();
    }
}
