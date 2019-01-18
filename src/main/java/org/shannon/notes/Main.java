package org.shannon.notes;

import lombok.val;
import org.apache.commons.cli.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;

@SpringBootApplication
@PropertySources(value = { @PropertySource(value = "classpath:/application.properties") })
public class Main {
    /**
     * The requested directory to place the memory mapped files.
     */
    public static Directory directory;

    /**
     * Print the help
     *
     * @param options   The options to print
     */
    private static void printHelp(Options options) {
        new HelpFormatter().printHelp("java -jar noteRepository.jar <args>", options);
        System.exit(0);
    }

    public static void main(String[] args) {
        val options = generateOptions();
        try {
            val line = new DefaultParser().parse(options, args);

            if (line.hasOption("help")) {
                printHelp(options);
            } else if (line.hasOption("tempDir")) {
                directory = new MMapDirectory(Files.createTempDirectory("LuceneNotes"));
            } else if (line.hasOption("directory")) {
                directory = new MMapDirectory(FileSystems.getDefault().getPath(line.getOptionValue("directory")));
            } else {
                printHelp(options);
            }
            SpringApplication.run(Main.class, args);

        } catch (ParseException e) {
            printHelp(options);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create some options for parsing a command line
     *
     * @return  Some Options for parsing a command line
     */
    private static Options generateOptions() {
        val help = Option.builder("h")
                .required(false)
                .desc("Display this message")
                .longOpt("help")
                .hasArg(false)
                .build();

        val directory = Option.builder("d")
                .required(false)
                .desc("Some directory to put the Lucene index for persistence. Either this option or tempDir must be supplied")
                .longOpt("directory")
                .hasArg()
                .build();

        val temp = Option.builder("t")
                .required(false)
                .desc("Takes precedence over supplied directory. Just creates some temp directory according to java.nio.Files.createTempDirectory. Either this option or directory must be supplied")
                .longOpt("tempDir")
                .hasArg(false)
                .build();

        return new Options()
                .addOption(help)
                .addOption(directory)
                .addOption(temp);
    }
}