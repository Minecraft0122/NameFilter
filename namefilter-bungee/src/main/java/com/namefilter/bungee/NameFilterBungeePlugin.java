package com.namefilter.bungee;

import com.namefilter.core.DailyAuditLogger;
import com.namefilter.core.ForbiddenPatternLoader;
import com.namefilter.core.HandlingMode;
import com.namefilter.core.MatchResult;
import com.namefilter.core.NameFilterConfig;
import com.namefilter.core.NameFilterConfigParser;
import com.namefilter.core.NameFilterRuntime;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** BungeeCord entry point. */
public final class NameFilterBungeePlugin extends Plugin implements Listener {
    private static final String CONFIG_FILE = "config.properties";

    private NameFilterRuntime runtime;
    private DailyAuditLogger auditLogger;

    @Override
    public void onEnable() {
        try {
            this.runtime = new NameFilterRuntime(
                    loadConfig(),
                    ForbiddenPatternLoader.load(getForbiddenPatternsFile()));
        } catch (IOException error) {
            throw new IllegalStateException("Could not load NameFilter configuration or forbidden patterns", error);
        }
        this.auditLogger = new DailyAuditLogger(getDataFolder().toPath().resolve("log"));
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this, new ReloadCommand());
    }

    @Override
    public void onDisable() {
        if (auditLogger != null) {
            auditLogger.close();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(PreLoginEvent event) {
        if (event.isCancelled()) {
            return;
        }

        String playerName = event.getConnection().getName();
        NameFilterRuntime.Snapshot state = runtime.snapshot();
        MatchResult match = state.matcher().match(playerName);
        if (!match.isMatch()) {
            return;
        }

        String reason = state.config().handlingMode() == HandlingMode.SILENT
                ? ""
                : state.config().rejectionMessage();
        event.setReason(new TextComponent(reason));
        event.setCancelled(true);

        String remoteAddress = String.valueOf(event.getConnection().getSocketAddress());
        auditLogger.record(playerName, remoteAddress, match, "BungeeCord", state.config().handlingMode())
                .exceptionally(error -> {
                    getLogger().warning("Could not write NameFilter audit record: " + error.getMessage());
                    return null;
                });
    }

    private NameFilterConfig loadConfig() throws IOException {
        Path dataDirectory = getDataFolder().toPath();
        Files.createDirectories(dataDirectory);
        Path configPath = dataDirectory.resolve(CONFIG_FILE);
        if (Files.notExists(configPath)) {
            try (InputStream defaults = getResourceAsStream(CONFIG_FILE)) {
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

    private Path getForbiddenPatternsFile() {
        return getDataFolder().toPath().resolve(ForbiddenPatternLoader.FILE_NAME);
    }

    private final class ReloadCommand extends Command {
        private ReloadCommand() {
            super("namefilter", "namefilter.admin", "nf");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
                sender.sendMessage(new TextComponent("Usage: /namefilter reload"));
                return;
            }
            try {
                NameFilterConfig config = loadConfig();
                runtime.reload(config, getForbiddenPatternsFile());
                sender.sendMessage(new TextComponent(
                        "NameFilter configuration and forbidden patterns reloaded."));
            } catch (IOException | RuntimeException error) {
                getLogger().warning("Could not reload NameFilter; previous settings remain active: "
                        + error.getMessage());
                sender.sendMessage(new TextComponent(
                        "NameFilter reload failed; the previous settings remain active."));
            }
        }
    }
}
