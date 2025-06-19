package org.app.service;

import org.app.helper.TranslationExtractor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Consumer;

public class ExcelTranslationService {
    private final Consumer<String> logger;

    public ExcelTranslationService(Consumer<String> logger) {
        this.logger = logger;
    }

    public File processExcel(File excelFile) throws Exception {
        // 1) Run the extractor/translator
        Path translator = TranslationExtractor.unpackTranslator();
        ProcessBuilder pb = new ProcessBuilder(
                translator.toAbsolutePath().toString(),
                excelFile.getAbsolutePath()
        );
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        // 2) Collect stdout
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream is = proc.getInputStream()) {
            byte[] buf = new byte[4096];
            int r;
            while ((r = is.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
        }
        int exit = proc.waitFor();
        String output = out.toString(String.valueOf(StandardCharsets.UTF_8));

        // 3) Emit each line to logger
        String outputFile = null;
        for (String line : output.split("\n")) {
            String trimmed = line.trim();
            logger.accept(trimmed);
            if (trimmed.endsWith(".xlsx")) {
                outputFile = trimmed;
            }
        }

        if (exit != 0) {
            throw new RuntimeException("Translation failed:\n" + output);
        }

        if (outputFile == null) {
            throw new RuntimeException("Could not find output Excel file in script output:\n" + output);
        }

        return new File(outputFile);
    }
}
