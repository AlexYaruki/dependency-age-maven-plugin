package com.github.alexyaruki.pda;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    private static final String LOG_PART = " is older than (";

    /**
     * Check log suffix for years.
     */
    private static final String YEARS_SUFFIX = ") years: ";

    /**
     * Check log suffix for days.
     */
    private static final String DAYS_SUFFIX = ") days: ";

    /**
     * Check log suffix for hours.
     */
    private static final String HOURS_SUFFIX = ") hours: ";

    /**
     * Check log suffix for minutes.
     */
    private static final String MINUTES_SUFFIX = ") minutes: ";

    /**
     * Mojo parameter - years limiter.
     */
    @Parameter(defaultValue = "0", property = "pda.yearsLimit")
    private int years; //NOPMD

    /**
     * Mojo parameter - days limiter.
     */
    @Parameter(defaultValue = "0", property = "pda.daysLimit")
    private int days; //NOPMD

    /**
     * Mojo parameter - hours limiter.
     */
    @Parameter(defaultValue = "0", property = "pda.hoursLimit")
    private int hours; //NOPMD

    /**
     * Mojo parameter - minutes limiter.
     */
    @Parameter(defaultValue = "0", property = "pda.minutesLimit")
    private int minutes; //NOPMD

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (IntStream.of(years, days, hours, minutes).allMatch(param -> param == 0)) {
            throw new MojoExecutionException("No limiters selected");
        }
        final long validParams = IntStream.of(years, days, hours, minutes).filter((param) -> param != 0).count();
        if (validParams > 1) {
            throw new MojoExecutionException("More than one limiter used.");
        }
        final List<String> checkLog = createCheckLog();
        if (!checkLog.isEmpty()) {
            final Log log = getLog();
            checkLog.forEach(log::error);
            throw new MojoFailureException("Dependencies do not meet age requirements, see logs");
        }
    }

    /**
     * Creates check log based on current Maven project dependencies.
     *
     * @return complete log for checks
     */
    private List<String> createCheckLog() {
        final Map<Dependency, Long> timestampMap = InfoGenerator.generateTimestampMap(project, getLog(), ignoreString);
        return timestampMap.entrySet()
            .stream()
            .map((entry) -> {
                final long timestampAge = System.currentTimeMillis() - entry.getValue();
                final DurationSplitter durationSplitter = new DurationSplitter(timestampAge);
                if (years > 0 && durationSplitter.getYears() >= years) {
                    return entry.getKey().toString() + LOG_PART + years + YEARS_SUFFIX + InfoGenerator.generateInfo(entry.getValue());
                } else if (days > 0 && durationSplitter.getDays() >= days) {
                    return entry.getKey().toString() + LOG_PART + days + DAYS_SUFFIX + InfoGenerator.generateInfo(entry.getValue());
                } else if (hours > 0 && durationSplitter.getHours() >= hours) {
                    return entry.getKey().toString() + LOG_PART + hours + HOURS_SUFFIX + InfoGenerator.generateInfo(entry.getValue());
                } else if (minutes > 0 && durationSplitter.getMinutes() >= minutes) {
                    return entry.getKey().toString() + LOG_PART + minutes + MINUTES_SUFFIX + InfoGenerator.generateInfo(entry.getValue());
                }
                return "";
            })
            .filter((info) -> info.length() != 0)
            .collect(Collectors.toList());
    }

}
