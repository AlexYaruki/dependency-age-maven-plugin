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
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

class InfoGenerator {

    static Map<String, String> generateInfoMap(MavenProject project, String ignoreString) {
        Map<String, String> pdaInfo = new HashMap<>();
        project.getDependencies()
               .stream()
               .filter((dependency -> {
                   if (ignoreString != null && ignoreString.length() != 0) {
                       if (dependency.getGroupId().contains(ignoreString) || dependency.getArtifactId().contains(ignoreString)) {
                           return false;
                       }
                       return true;
                   }
                   return true;
               }))
               .map((dependency -> dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion()))
               .map((name) -> {
                   String[] nameParts = name.split(":");
                   Map.Entry<String,Long> entry = entry(name, downloadTimestamp(nameParts[0], nameParts[1], nameParts[2]));
                   return entry;
               })
               .sorted(Comparator.comparingLong(Map.Entry::getValue))
               .map((e) -> entry(e.getKey(),generateInfo(e.getValue()))).forEach((e) -> {
                   pdaInfo.entrySet().add(e);
               });
        return pdaInfo;
    }

    private static <K,V> Map.Entry<K,V> entry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
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

    private static String generateInfo(long timestamp) {
        long temp = timestamp;
        if (temp == -1) {
            return "Maven Central HTTP Error - Try again ?";
        } else {
            temp = Instant.now().toEpochMilli() - temp;
            temp /= 1000;
            long seconds = temp % 60;
            temp /= 60;
            long minutes = temp % 60;
            temp /= 60;
            long hours = temp % 24;
            temp /= 24;
            long days = temp % 365;
            temp /= 365;
            long years = temp;
            StringBuilder infoBuilder = new StringBuilder();
            infoBuilder.append(years).append(years == 1 ? " year, " : " years, ")
                    .append(days).append(days == 1 ? " day, " : " days, ")
                    .append(hours).append(hours == 1 ? " hour, " : " hours, ")
                    .append(minutes).append(minutes == 1 ? " minute, " : " minutes, ")
                    .append(seconds).append(seconds == 1 ? " second" : " seconds");
            return infoBuilder.toString();
        }
    }


}
