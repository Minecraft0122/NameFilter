package com.namefilter.velocity;

import com.google.inject.Inject;
import com.namefilter.core.DailyAuditLogger;
import com.namefilter.core.ForbiddenPatternLoader;
import com.namefilter.core.HandlingMode;
import com.namefilter.core.MatchResult;
import com.namefilter.core.NameFilterConfig;
import com.namefilter.core.NameFilterConfigParser;
import com.namefilter.core.NameFilterRuntime;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.event.ResultedEvent.ComponentResult;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        version = "1.2.0",
        description = "Check player name when join in server",
        authors = "NameFilter"
)
public final class NameFilterVelocityPlugin {
    private static final String CONFIG_FILE = "config.properties";

    private final Path dataDirectory;
    private final Logger logger;
    private final ProxyServer proxyServer;
    private NameFilterRuntime runtime;
    private DailyAuditLogger auditLogger;

    @Inject
    public NameFilterVelocityPlugin(@DataDirectory Path dataDirectory, Logger logger, ProxyServer proxyServer) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.proxyServer = proxyServer;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        try {
            NameFilterConfig config = loadConfig();
            this.runtime = new NameFilterRuntime(
                    config,
                    ForbiddenPatternLoader.load(dataDirectory.resolve(ForbiddenPatternLoader.FILE_NAME)));
        } catch (IOException error) {
            throw new IllegalStateException("Could not load NameFilter configuration or forbidden patterns", error);
        }
        this.auditLogger = new DailyAuditLogger(dataDirectory.resolve("log"));
        proxyServer.getCommandManager().register(
                proxyServer.getCommandManager().metaBuilder("namefilter").plugin(this).aliases("nf").build(),
                new ReloadCommand());
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        if (!event.getResult().isAllowed()) {
            return;
        }

        String playerName = event.getPlayer().getUsername();
        NameFilterRuntime.Snapshot state = runtime.snapshot();
        MatchResult match = state.matcher().match(playerName);
        if (!match.isMatch()) {
            return;
        }

        Component reason = state.config().handlingMode() == HandlingMode.SILENT
                ? Component.empty()
                : Component.text(state.config().rejectionMessage());
        event.setResult(ComponentResult.denied(reason));

        String remoteAddress = String.valueOf(event.getPlayer().getRemoteAddress());
        auditLogger.record(playerName, remoteAddress, match, "Velocity", state.config().handlingMode())
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

    private final class ReloadCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            if (invocation.arguments().length != 1
                    || !invocation.arguments()[0].equalsIgnoreCase("reload")) {
                invocation.source().sendMessage(Component.text("Usage: /namefilter reload", NamedTextColor.YELLOW));
                return;
            }
            if (!invocation.source().hasPermission("namefilter.admin")) {
                invocation.source().sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
                return;
            }
            try {
                NameFilterConfig config = loadConfig();
                runtime.reload(config, dataDirectory.resolve(ForbiddenPatternLoader.FILE_NAME));
                invocation.source().sendMessage(Component.text(
                        "NameFilter configuration and forbidden patterns reloaded.", NamedTextColor.GREEN));
            } catch (IOException | RuntimeException error) {
                logger.warn("Could not reload NameFilter; previous settings remain active", error);
                invocation.source().sendMessage(Component.text(
                        "NameFilter reload failed; the previous settings remain active.", NamedTextColor.RED));
            }
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return invocation.source().hasPermission("namefilter.admin");
        }
    }
}
