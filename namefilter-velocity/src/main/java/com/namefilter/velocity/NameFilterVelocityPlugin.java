package com.namefilter.velocity;

import com.google.inject.Inject;
import com.namefilter.core.DailyAuditLogger;
import com.namefilter.core.HandlingMode;
import com.namefilter.core.MatchResult;
import com.namefilter.core.NameFilterConfig;
import com.namefilter.core.NameFilterConfigParser;
import com.namefilter.core.NameMatcher;
import com.velocitypowered.api.event.ResultedEvent.ComponentResult;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Velocity entry point. */
@Plugin(
        id = "namefilter",
        name = "NameFilter",
        version = "1.1.0",
        description = "Check player name when join in server",
        authors = "NameFilter"
)
public final class NameFilterVelocityPlugin {
    private static final String CONFIG_FILE = "config.properties";

    private final Path dataDirectory;
    private final Logger logger;
    private NameFilterConfig filterConfig;
    private NameMatcher matcher;
    private DailyAuditLogger auditLogger;

    @Inject
    public NameFilterVelocityPlugin(@DataDirectory Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        try {
            this.filterConfig = loadConfig();
        } catch (IOException error) {
            throw new IllegalStateException("Could not load NameFilter configuration", error);
        }
        this.matcher = new NameMatcher(filterConfig);
        this.auditLogger = new DailyAuditLogger(dataDirectory.resolve("log"));
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        if (!event.getResult().isAllowed()) {
            return;
        }

        String playerName = event.getPlayer().getUsername();
        MatchResult match = matcher.match(playerName);
        if (!match.isMatch()) {
            return;
        }

        Component reason = filterConfig.handlingMode() == HandlingMode.SILENT
                ? Component.empty()
                : Component.text(filterConfig.rejectionMessage());
        event.setResult(ComponentResult.denied(reason));

        String remoteAddress = String.valueOf(event.getPlayer().getRemoteAddress());
        auditLogger.record(playerName, remoteAddress, match, "Velocity", filterConfig.handlingMode())
                .exceptionally(error -> {
                    logger.warn("Could not write NameFilter audit record", error);
                    return null;
                });
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (auditLogger != null) {
            auditLogger.close();
        }
    }

    private NameFilterConfig loadConfig() throws IOException {
        Files.createDirectories(dataDirectory);
        Path configPath = dataDirectory.resolve(CONFIG_FILE);
        if (Files.notExists(configPath)) {
            try (InputStream defaults = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                if (defaults == null) {
                    throw new IOException("Missing bundled " + CONFIG_FILE);
                }
                Files.copy(defaults, configPath);
            }
        }

        Properties properties = new Properties();
        try (Reader input = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            properties.load(input);
        }
        return NameFilterConfigParser.parse(properties);
    }
}
