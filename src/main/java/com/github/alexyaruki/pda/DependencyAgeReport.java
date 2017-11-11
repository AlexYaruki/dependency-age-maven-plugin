package com.github.alexyaruki.pda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import j2html.tags.ContainerTag;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static j2html.TagCreator.body;
import static j2html.TagCreator.html;
import static j2html.TagCreator.table;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.tr;

/**
 * Plugin mojo - Dependency Age Report generation.
 * <p>
 * Generates report in selected format
 */
@Mojo(name = "report")
class DependencyAgeReport extends AbstractPDAMojo {

    /**
     * Parameter for selecting report type.
     */
    @Parameter(property = "pda.reportType")
    private String reportTypeString; //NOPMD

    /**
     * Executes report generation based on parsed report type.
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!getReportDestinationPath().toFile().exists()) {
            try {
                Files.createDirectories(getReportDestinationPath());
            } catch (IOException e) {
                throw new MojoExecutionException("Cannot create directory \"dependency-age\" in target build directory", e);
            }
        }
        final Map<String, String> pdaInfo = InfoGenerator.generateInfoMap(project, getLog(), ignoreString);
        if (Objects.isNull(reportTypeString)) {
            getLog().warn("Report not generated because of missing report type");
        } else {
            ReportType reportType = null;
            try {
                reportType = ReportType.valueOf(reportTypeString.toUpperCase(Locale.getDefault()));
            } catch (IllegalArgumentException e) {
                getLog().warn("Report not generated because of unknown report type: " + reportTypeString);
                return;
            }
            switch (reportType) {
                case JSON:
                    generateJSONReport(pdaInfo);
                    break;

                case HTML:
                    generateHTMLReport(pdaInfo);
                    break;

                case EXCEL:
                    generateExcelReport(pdaInfo);
                    break;

                default:
                    getLog().warn("Report not generated because of unknown report type: " + reportTypeString);
                    break;
            }
        }
    }

    /**
     * Generates report in JSON format.
     *
     * @param pdaInfo map describing information about dependencies age
     */

    private void generateJSONReport(final Map<String, String> pdaInfo) {
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode root = mapper.createObjectNode();
        final ArrayNode dependencies = mapper.createArrayNode();
        pdaInfo.entrySet().stream().forEach((entry) -> {
            final ObjectNode dependency = mapper.createObjectNode();
            dependency.put("name", entry.getKey());
            dependency.put("age", entry.getValue());
            dependencies.add(dependency);
        });
        root.set("dependencies", dependencies);
        try {
            final Path reportPath = getReportDestinationPath().resolve("dependency-age-report.json");
            Files.write(reportPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root).getBytes(Charset.defaultCharset()), StandardOpenOption.CREATE);
            getLog().info("Report saved to " + reportPath.toString());
        } catch (IOException e) {
            getLog().error("Error when saving report: " + e.getMessage());
        }
    }

    /**
     * Generates report in Excel format.
     *
     * @param pdaInfo map describing information about dependencies age
     */
    private void generateExcelReport(final Map<String, String> pdaInfo) {
        final XSSFWorkbook workbook = new XSSFWorkbook();
        final XSSFSheet summarySheet = workbook.createSheet("Dependency Age Summary");
        final XSSFRow rowFirst = summarySheet.createRow(0);
        final XSSFCell headerCell = rowFirst.createCell(0);
        headerCell.setCellValue(project.getName());
        summarySheet.addMergedRegion(CellRangeAddress.valueOf("A1:B1"));
        final XSSFRow columnHeaders = summarySheet.createRow(1);
        columnHeaders.createCell(0).setCellValue("Name");
        columnHeaders.createCell(1).setCellValue("Age");
        int dataRowId = 2;
        for (final Map.Entry<String, String> entry : pdaInfo.entrySet()) {
            final XSSFRow dataRow = summarySheet.createRow(dataRowId);
            dataRow.createCell(0).setCellValue(entry.getKey());
            dataRow.createCell(1).setCellValue(entry.getValue());
            dataRowId++;
        }
        summarySheet.autoSizeColumn(0);
        summarySheet.autoSizeColumn(1);
        try (FileOutputStream reportStream = new FileOutputStream(getReportDestinationPath().resolve("dependency-age-report.xlsx").toFile())) {
            workbook.write(reportStream);
        } catch (IOException e) {
            getLog().error("Error when saving Excel report: " + e.getMessage());
        }
    }

    /**
     * Generates report in HTML format.
     *
     * @param pdaInfo map describing information about dependencies age
     */
    private void generateHTMLReport(final Map<String, String> pdaInfo) {
        final ContainerTag table = table(tr(th(project.getName())).attr("colspan", "2"));
        for (final Map.Entry<String, String> entry : pdaInfo.entrySet()) {
            table.with(
                tr(
                    td(entry.getKey()),
                    td(entry.getValue())
                )
            );
        }
        final String htmlReportString = html(body(table)).render();
        try {

            final Path reportPath = getReportDestinationPath().resolve("dependency-age-report.html");
            Files.write(reportPath, htmlReportString.getBytes(Charset.defaultCharset()), StandardOpenOption.CREATE);
            getLog().info("Report saved to " + reportPath.toString());
        } catch (IOException e) {
            getLog().error("Error when saving report: " + e.getMessage());
        }
    }

    /**
     * Provides path to reports destination directory.
     * @return path to reports destination directory
     */
    private Path getReportDestinationPath() {
        return Paths.get(project.getBuild().getDirectory(), "dependency-age");
    }
}
