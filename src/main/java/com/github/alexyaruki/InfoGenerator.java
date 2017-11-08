package com.github.alexyaruki;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

class InfoGenerator {

    static Map<String, String> generateInfoMap(MavenProject project, String ignoreString) {
        Map<String, String> pdaInfo = new HashMap<>();
        for (Dependency dependency : project.getDependencies()) {
            String group = dependency.getGroupId();
            String artifact = dependency.getArtifactId();
            String version = dependency.getVersion();
            String name = group + ":" + artifact + ":" + version;
            if (ignoreString != null && ignoreString.length() != 0) {
                if (group.contains(ignoreString) || artifact.contains(ignoreString)) {
                    continue;
                }
            }
            String info = generateInfo(dependency);
            pdaInfo.put(name, info);
        }
        return pdaInfo;
    }

    private static String generateInfo(Dependency dependency) {
        String group = dependency.getGroupId();
        String artifact = dependency.getArtifactId();
        String version = dependency.getVersion();
        long timestamp = downloadTimestamp(group, artifact, version);
        if (timestamp == -1) {
            return "Maven Central HTTP Error - Try again ?";
        } else {
            timestamp = Instant.now().toEpochMilli() - timestamp;
            timestamp /= 1000;
            long seconds = timestamp % 60;
            timestamp /= 60;
            long minutes = timestamp % 60;
            timestamp /= 60;
            long hours = timestamp % 24;
            timestamp /= 24;
            long days = timestamp % 365;
            timestamp /= 365;
            long years = timestamp;
            StringBuilder infoBuilder = new StringBuilder();
            infoBuilder.append(years).append(years == 1 ? " year, " : " years, ")
                    .append(days).append(days == 1 ? " day, " : " days, ")
                    .append(hours).append(hours == 1 ? " hour, " : " hours, ")
                    .append(minutes).append(minutes == 1 ? " minute, " : " minutes, ")
                    .append(seconds).append(seconds == 1 ? " second" : " seconds");
            return infoBuilder.toString();
        }
    }

    private static long downloadTimestamp(String group, String artifact, String version) {
        try {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet request = new HttpGet("http://search.maven.org/solrsearch/select?q=g%3A%22" + group + "%22+AND+a%3A%22" + artifact + "%22&core=gav&rows=1000&wt=json");
                ByteArrayOutputStream boas = new ByteArrayOutputStream();
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    if (response.getStatusLine().getStatusCode() == 200) {
                        response.getEntity().writeTo(boas);
                    } else {
                        return -1;
                    }
                }
                String json = boas.toString();
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(json);
                JsonNode docs = root.get("response").get("docs");
                long timestamp = 0;
                for (JsonNode doc : docs) {
                    if (doc.get("v").asText().equals(version)) {
                        timestamp = doc.get("timestamp").asLong();
                        break;
                    }
                }
                return timestamp;
            }
        } catch (Exception e) {
            return -1;
        }
    }
}