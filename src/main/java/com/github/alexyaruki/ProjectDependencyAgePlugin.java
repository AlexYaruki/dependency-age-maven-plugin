package com.github.alexyaruki;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.TimeZone;


@Mojo(name = "show")
public class ProjectDependencyAgePlugin extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        for (Dependency dependency : project.getDependencies()) {
            String group = dependency.getGroupId();
            String artifact = dependency.getArtifactId();
            String version = dependency.getVersion();
            long timestamp = 0;
            try {
                timestamp = downloadTimestamp(group, artifact, version);
            } catch (Exception e ) {
                System.out.println(group + ":" + artifact + " -> " + e.getMessage());
                continue;
            }
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
            System.out.println(group + ":" + artifact + " -> " + years + (years == 1 ? " year, " : " years, ")
                                                               + days + ( days == 1 ? " day, " :  " days, ")
                                                               + hours + ( hours == 1 ? " hour, " : " hours, ")
                                                               + minutes + ( minutes == 1 ? " minute, " : " minutes, ")
                                                               + seconds + ( seconds == 1 ? " second" : " seconds"));
        }
    }

    private static String millisToElapsedTime(long millis){
        DateFormat fmt = new SimpleDateFormat(":mm:ss.SSS");
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        return (millis/3600000/*hours*/)+fmt.format(new Date(millis));
    }

    private long downloadTimestamp(String group, String artifact, String version) {
        try {
            try (CloseableHttpClient httpClient = HttpClients.createDefault() ) {
                HttpGet request = new HttpGet("http://search.maven.org/solrsearch/select?q=g%3A%22" + group + "%22+AND+a%3A%22" + artifact + "%22&core=gav&rows=1000&wt=json");
                ByteArrayOutputStream boas = new ByteArrayOutputStream();
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    response.getEntity().writeTo(boas);
                }
                String json = boas.toString();
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(json);
                JsonNode docs = root.get("response").get("docs");
                long timestamp = 0;
                for (JsonNode doc : docs) {
                    if(doc.get("v").asText().equals(version)) {
                        timestamp = doc.get("timestamp").asLong();
                        break;
                    }
                }
                return timestamp;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
