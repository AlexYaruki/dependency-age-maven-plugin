package com.github.alexyaruki.pda;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.Map;

/**
 * Plugin mojo - Dependency Age Report presentation.
 * <p>
 * Presents report to Maven logger
 */
@Mojo(name = "show")
class DependencyAgeShow extends AbstractPDAMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Map<String, String> pdaInfo = InfoGenerator.generateInfoMap(project, getLog(), ignoreString);
        pdaInfo.keySet().stream().mapToInt(String::length).max().ifPresent((maxInfoLength) -> {
            for (final Map.Entry<String, String> entry : pdaInfo.entrySet()) {
                getLog().info(StringUtils.rightPad(entry.getKey(), maxInfoLength) + " -> " + entry.getValue());
            }
        });

    }

}
