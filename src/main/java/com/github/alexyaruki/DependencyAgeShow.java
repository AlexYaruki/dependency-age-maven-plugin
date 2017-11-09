package com.github.alexyaruki;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.Map;

@Mojo(name = "show")
class DependencyAgeShow extends BaseMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Map<String, String> pdaInfo = InfoGenerator.generateInfoMap(project,ignoreString);
        pdaInfo.keySet().stream().mapToInt(String::length).max().ifPresent((maxInfoLength) -> {
            for (Map.Entry<String, String> entry : pdaInfo.entrySet()) {
                System.out.println(StringUtils.rightPad(entry.getKey(), maxInfoLength) + " -> " + entry.getValue());
            }
        });
    }

}
