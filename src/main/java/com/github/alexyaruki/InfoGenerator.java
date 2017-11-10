package com.github.alexyaruki;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;


/**
 * Class for generating dependency age information per dependency specified in pom.xml
 */
final class InfoGenerator {

    private InfoGenerator() {
    }

    /**
     * Generates map of dependency name (groupId:artifactId:version) to textual
     * description of its age
     *
     * @param project      - current Maven project
     * @param log
     *@param ignoreString - which string to ignore in dependency groupId or artifactId  @return
     */
    static Map<String, String> generateInfoMap(final MavenProject project,final Log log, final String ignoreString) {
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
                    final long timestamp = downloadTimestamp(log,nameParts[0], nameParts[1], nameParts[2]);
                    if(log.isDebugEnabled()) {
                        log.debug(name + " -> " + timestamp + " ms");
                    }
                    return createEntry(name, timestamp);
                })
                .sorted(Comparator.comparingLong(Map.Entry::getValue))
                .map(entry -> createEntry(entry.getKey(), generateInfo(entry.getValue())))
                .forEach((entry) -> pdaInfo.put(entry.getKey(), entry.getValue()));
        return pdaInfo;
    }

    private static <K, V> Map.Entry<K, V> createEntry(final K key, final V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    private static long downloadTimestamp(final Log log, final String group, final String artifact, final String version) {
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpGet request = new HttpGet("http://search.maven.org/solrsearch/select?q=g%3A%22" + group + "%22+AND+a%3A%22" + artifact + "%22&core=gav&rows=1000&wt=json");
            if(log.isDebugEnabled()) {
                log.debug("GET - > " + request.getURI().toString());
            }
            final ByteArrayOutputStream jsonStream = new ByteArrayOutputStream();
            try (final CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() == 200) {
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
                    if(log.isDebugEnabled()) {
                        log.debug("timestamp(RAW) - > " + doc.get("timestamp"));
                    }
                    timestamp = Long.parseLong(doc.get("timestamp").asText());
                    if(log.isDebugEnabled()) {
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

    private static String generateInfo(final long timestamp) {
        if (timestamp == -1) {
            return "Maven Central HTTP Error - Try again ?";
        }

        return generateInfoString(timestamp);
    }

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
