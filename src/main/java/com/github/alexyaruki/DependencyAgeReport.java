package com.github.alexyaruki;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

@Mojo(name = "report")
public class DependencyAgeReport extends BaseMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Map<String, String> pdaInfo = InfoGenerator.generateInfoMap(project, ignoreString);
        ReportType reportType = ReportType.valueOf(reportTypeString.toUpperCase());
        switch (reportType) {
            case JSON: {
                generateJSONReport(pdaInfo);
                break;
            }
            case HTML: {
                generateHTMLReport(pdaInfo);
                break;
            }
            case EXCEL: {
                generateEXCELReport(pdaInfo);
            }
            default: {
                getLog().warn("Report not generated because of invalid report type: " + reportTypeString);
            }
        }
    }

    private void generateJSONReport(Map<String, String> pdaInfo) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode dependencies = mapper.createArrayNode();
        pdaInfo.entrySet().stream().forEach((entry) -> {
            ObjectNode dependency = mapper.createObjectNode();
            dependency.put("name", entry.getKey());
            dependency.put("age", entry.getValue());
            dependencies.add(dependency);
        });
        root.set("dependencies", dependencies);

        try {
            Path reportPath = Paths.get(project.getBuild().getDirectory(), "dependency-age-report.json");
            Files.write(reportPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root).getBytes(), StandardOpenOption.CREATE);
            getLog().info("Report saved to " + reportPath.toString());
        } catch (IOException e) {
            getLog().error("Error when saving report: " + e.getMessage());
        }
    }

    private void generateEXCELReport(Map<String, String> pdaInfo) {
    }

    private void generateHTMLReport(Map<String, String> pdaInfo) {
    }
}
