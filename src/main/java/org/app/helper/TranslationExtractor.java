package org.app.helper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class TranslationExtractor {
    public static Path unpackTranslator() throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        String resourcePath;
        String suffix;

        if (os.contains("win")) {
            resourcePath = "/native/windows/translate_excel.exe";
            suffix = ".exe";
        } else if (os.contains("mac")) {
            resourcePath = "/native/macos/translate_excel";
            suffix = "";
        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + os);
        }

        try (InputStream in = TranslationExtractor.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found on classpath: " + resourcePath);
            }
            Path tmp = Files.createTempFile("excel-translator-", suffix);
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            tmp.toFile().setExecutable(true, true);
            tmp.toFile().deleteOnExit();
            return tmp;
        }
    }
}
