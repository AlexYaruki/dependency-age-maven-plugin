package com.github.alexyaruki;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

@Mojo(name = "report")
public class DependencyAgeReport extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(property = "pda.ignoreString")
    private String ignoreString;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Map<String, String> pdaInfo = InfoGenerator.generateInfoMap(project,ignoreString);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode dependencies = mapper.createArrayNode();
        pdaInfo.entrySet().stream().forEach((entry) -> {
            ObjectNode dependency = mapper.createObjectNode();
            dependency.put("name",entry.getKey());
            dependency.put("age",entry.getValue());
            dependencies.add(dependency);
        });
        root.set("dependencies",dependencies);

        try {
            Path reportPath = Paths.get(project.getBuild().getDirectory(), "dependency-age-report.json");
            Files.write(reportPath,mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root).getBytes(), StandardOpenOption.CREATE);
            getLog().info("Report saved to " + reportPath.toString());
        } catch (IOException e) {
            getLog().error("Error when saving report: " + e.getMessage());
        }
    }
}
