package com.github.alexyaruki.pda;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Map;
import java.util.stream.IntStream;

/**
 * Plugin mojo - Dependency Age Check.
 * <p>
 * Validates age of dependencies
 */
@Mojo(name = "check")
class DependencyAgeCheck extends AbstractPDAMojo {

    /**
     * Prefix used for check log generation per dependency.
     */
    private static final String LOG_PREFIX = "Dependency is older than (";

    /**
     * Check log suffix for years.
     */
    private static final String LOG_YEARS_SUFFIX = ") years";

    /**
     * Check log suffix for days.
     */
    private static final String LOG_DAYS_SUFFIX = ") days";

    /**
     * Check log suffix for hours.
     */
    private static final String LOG_HOURS_SUFFIX = ") hours";

    /**
     * Check log suffix for minutes.
     */
    private static final String LOG_MINUTES_SUFFIX = ") minutes";

    /**
     * Mojo parameter - years limiter.
     */
    @Parameter(defaultValue = "0")
    private int years; //NOPMD

    /**
     * Mojo parameter - days limiter.
     */
    @Parameter(defaultValue = "0")
    private int days; //NOPMD

    /**
     * Mojo parameter - hours limiter.
     */
    @Parameter(defaultValue = "0")
    private int hours; //NOPMD

    /**
     * Mojo parameter - minutes limiter.
     */
    @Parameter(defaultValue = "0")
    private int minutes; //NOPMD

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final long validParams = IntStream.of(years, days, hours, minutes).filter((param) -> param != 0).count();
        if (validParams > 1) {
            throw new MojoExecutionException("More than one limiter used.");
        }
        final String checkLog = createCheckLog();
        if (checkLog.length() != 0) {
            throw new MojoFailureException(checkLog);
        }
    }

    /**
     * Creates check log based on current Maven project dependencies.
     *
     * @return complete log for checks
     */
    private String createCheckLog() {
        final Map<Dependency, Long> timestampMap = InfoGenerator.generateTimestampMap(project, getLog(), ignoreString);
        return timestampMap.entrySet()
            .stream()
            .map((entry) -> {
                final DurationSplitter durationSplitter = new DurationSplitter(System.currentTimeMillis() - entry.getValue());
                if (years > 0 && durationSplitter.getYears() > years) {
                    return LOG_PREFIX + years + LOG_YEARS_SUFFIX;
                } else if (days > 0 && durationSplitter.getDays() > days) {
                    return LOG_PREFIX + days + LOG_DAYS_SUFFIX;
                } else if (hours > 0 && durationSplitter.getHours() > hours) {
                    return LOG_PREFIX + hours + LOG_HOURS_SUFFIX;
                } else if (minutes > 0 && durationSplitter.getMinutes() > minutes) {
                    return LOG_PREFIX + minutes + LOG_MINUTES_SUFFIX;
                }
                return "";
            })
            .filter((info) -> info.length() != 0)
            .reduce("\n", String::concat);
    }

}
