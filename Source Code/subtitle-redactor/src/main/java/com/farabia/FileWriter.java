package com.farabia;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileWriter implements AutoCloseable {

    private BufferedWriter writer;

    public FileWriter(String path) throws IOException {
        writer = Files.newBufferedWriter(Path.of(path), StandardCharsets.UTF_8);
    }

    public void writeOnFile(String text) throws IOException {
        writer.write(text);
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}