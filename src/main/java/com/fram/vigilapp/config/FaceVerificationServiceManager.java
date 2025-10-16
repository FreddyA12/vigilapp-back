package com.fram.vigilapp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FaceVerificationServiceManager implements ApplicationRunner, DisposableBean {

    @Value("${face.verification.service.enabled:true}")
    private boolean enabled;

    @Value("${face.verification.service.path:face-verification-service}")
    private String servicePath;

    @Value("${face.verification.service.python.command:python}")
    private String pythonCommand;

    @Value("${face.verification.service.startup.wait.seconds:10}")
    private int startupWaitSeconds;

    private Process pythonProcess;
    private Thread outputThread;
    private Thread errorThread;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled) {
            log.info("Face Verification Service is disabled");
            return;
        }

        log.info("Starting Face Verification Service...");

        try {
            File serviceDir = new File(servicePath);
            if (!serviceDir.exists()) {
                log.error("Face Verification Service directory not found: {}", serviceDir.getAbsolutePath());
                return;
            }

            File mainPy = new File(serviceDir, "main.py");
            if (!mainPy.exists()) {
                log.error("main.py not found in: {}", serviceDir.getAbsolutePath());
                return;
            }

            // Construir comando según el sistema operativo
            List<String> command = new ArrayList<>();
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                // Windows: Intentar usar el entorno virtual si existe
                File venvPython = new File(serviceDir, "venv/Scripts/python.exe");
                if (venvPython.exists()) {
                    command.add(venvPython.getAbsolutePath());
                } else {
                    command.add(pythonCommand);
                }
            } else {
                // Linux/Mac: Intentar usar el entorno virtual si existe
                File venvPython = new File(serviceDir, "venv/bin/python");
                if (venvPython.exists()) {
                    command.add(venvPython.getAbsolutePath());
                } else {
                    command.add(pythonCommand);
                }
            }

            command.add("main.py");

            log.info("Executing command: {} in directory: {}", String.join(" ", command), serviceDir.getAbsolutePath());

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(serviceDir);
            processBuilder.redirectErrorStream(false);

            pythonProcess = processBuilder.start();

            // Capturar salida estándar
            outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("[Python Service] {}", line);
                    }
                } catch (Exception e) {
                    if (!e.getMessage().contains("Stream closed")) {
                        log.error("Error reading Python service output", e);
                    }
                }
            });
            outputThread.setName("python-service-output");
            outputThread.setDaemon(true);
            outputThread.start();

            // Capturar salida de error
            errorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(pythonProcess.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.warn("[Python Service Error] {}", line);
                    }
                } catch (Exception e) {
                    if (!e.getMessage().contains("Stream closed")) {
                        log.error("Error reading Python service error stream", e);
                    }
                }
            });
            errorThread.setName("python-service-error");
            errorThread.setDaemon(true);
            errorThread.start();

            // Esperar un poco para que el servicio inicie
            log.info("Waiting {} seconds for Face Verification Service to start...", startupWaitSeconds);
            Thread.sleep(startupWaitSeconds * 1000L);

            if (pythonProcess.isAlive()) {
                log.info("Face Verification Service started successfully on port 8000");
            } else {
                log.error("Face Verification Service failed to start. Exit code: {}", pythonProcess.exitValue());
            }

        } catch (Exception e) {
            log.error("Failed to start Face Verification Service", e);
        }
    }

    @Override
    public void destroy() throws Exception {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            log.info("Stopping Face Verification Service...");

            try {
                // Intentar detener gracefully
                pythonProcess.destroy();

                // Esperar hasta 5 segundos
                if (!pythonProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    log.warn("Face Verification Service didn't stop gracefully, forcing...");
                    pythonProcess.destroyForcibly();
                }

                log.info("Face Verification Service stopped");
            } catch (Exception e) {
                log.error("Error stopping Face Verification Service", e);
            }
        }

        // Interrumpir threads de lectura
        if (outputThread != null && outputThread.isAlive()) {
            outputThread.interrupt();
        }
        if (errorThread != null && errorThread.isAlive()) {
            errorThread.interrupt();
        }
    }
}
