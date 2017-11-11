package com.github.alexyaruki.pda;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;


/**
 * Base class for all mojo's in plugin.
 */
public abstract class AbstractPDAMojo extends AbstractMojo {

    /**
     * Object representing current Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * Object representing current Maven session.
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    /**
     * Parameter for ignoring dependencies containing specified string in group
     * or artifact id.
     */
    @Parameter(property = "pda.ignoreString")
    protected String ignoreString;

    /**
     * Parameter-less constructor.
     */
    protected AbstractPDAMojo() {
        super();
    }


}
