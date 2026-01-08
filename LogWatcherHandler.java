package com.example.demo;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class LogWatcherHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final Path logFilePath = Paths.get("test.log");
    private long lastKnownPosition = 0;

    @PostConstruct
    public void init() {
        // Create file if not exists
        if (!Files.exists(logFilePath)) {
            try {
                Files.createFile(logFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            lastKnownPosition = Files.size(logFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Start watching thread
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::checkLogFile, 0, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("Client connected: " + session.getId());

        // Send last 10 lines immediately
        try {
            List<String> lastLines = readLastNLines(logFilePath.toFile(), 10);
            for (String line : lastLines) {
                session.sendMessage(new TextMessage(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("Client disconnected: " + session.getId());

    }

    private void checkLogFile() {
        try {
            long currentSize = Files.size(logFilePath);

            if (currentSize > lastKnownPosition) {
                // Read new content
                try (RandomAccessFile file = new RandomAccessFile(logFilePath.toFile(), "r")) {
                    file.seek(lastKnownPosition);
                    byte[] buffer = new byte[(int) (currentSize - lastKnownPosition)];
                    file.readFully(buffer);
                    String content = new String(buffer, StandardCharsets.UTF_8);

                    String[] lines = content.split("\n");
                    for (String line : lines) {
                        broadcast(line);
                    }
                }
                lastKnownPosition = currentSize;
            } else if (currentSize < lastKnownPosition) {
                // File rotated/truncated
                lastKnownPosition = currentSize;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcast(String message) {
        List<WebSocketSession> deadSessions = new ArrayList<>();
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (Exception e) {
                deadSessions.add(session);
            }
        }
        sessions.removeAll(deadSessions);
    }

    public List<String> readLastNLines(File file, int n) throws IOException {
        List<String> lines = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long length = raf.length();
            long pointer = length - 1;
            int linesRead = 0;
            StringBuilder sb = new StringBuilder();

            while (pointer >= 0 && linesRead < n) {
                raf.seek(pointer);
                int c = raf.read();

                if (c == '\n') {
                    if (pointer < length - 1) {
                        lines.add(0, sb.reverse().toString());
                        sb.setLength(0);
                        linesRead++;
                    }
                } else if (c != '\r') {
                    sb.append((char) c);
                }
                pointer--;
            }
            // Add the incomplete line at the start of file
            if (sb.length() > 0) {
                lines.add(0, sb.reverse().toString());
            }
        }
        return lines;
    }
}
