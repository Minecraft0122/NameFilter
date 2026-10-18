package com.namefilter.paper;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.namefilter.core.DailyAuditLogger;
import com.namefilter.core.ForbiddenPatternLoader;
import com.namefilter.core.HandlingMode;
import com.namefilter.core.MatchResult;
import com.namefilter.core.NameFilterConfig;
import com.namefilter.core.NameFilterConfigParser;
import com.namefilter.core.NameFilterRuntime;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

/** Paper entry point. */
public final class NameFilterPaperPlugin extends JavaPlugin implements Listener {
    private NameFilterRuntime runtime;
    private DailyAuditLogger auditLogger;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        NameFilterConfig filterConfig = NameFilterConfigParser.parse(getConfig().getValues(false));
        try {
            this.runtime = new NameFilterRuntime(
                    filterConfig,
                    ForbiddenPatternLoader.load(getForbiddenPatternsFile()));
        } catch (IOException error) {
            throw new IllegalStateException("Could not load forbidden name patterns", error);
        }
        this.auditLogger = new DailyAuditLogger(getDataFolder().toPath().resolve("log"));
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("namefilter").setExecutor(new ReloadCommand());
    }

    @Override
    public void onDisable() {
        if (auditLogger != null) {
            auditLogger.close();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onValidateLogin(PlayerConnectionValidateLoginEvent event) {
        if (!event.isAllowed() || !(event.getConnection() instanceof PlayerLoginConnection connection)) {
            return;
        }

        PlayerProfile profile = connection.getAuthenticatedProfile();
        if (profile == null) {
            profile = connection.getUnsafeProfile();
        }
        if (profile == null || profile.getName() == null) {
            return;
        }

        String playerName = profile.getName();
        NameFilterRuntime.Snapshot state = runtime.snapshot();
        MatchResult match = state.matcher().match(playerName);
        if (!match.isMatch()) {
            return;
        }

        Component reason = state.config().handlingMode() == HandlingMode.SILENT
                ? Component.empty()
                : Component.text(state.config().rejectionMessage());
        event.kickMessage(reason);

        String remoteAddress = String.valueOf(connection.getClientAddress());
        auditLogger.record(playerName, remoteAddress, match, "Paper", state.config().handlingMode())
                .exceptionally(error -> {
                    getLogger().warning("Could not write NameFilter audit record: " + error.getMessage());
                    return null;
                });
    }

    private java.nio.file.Path getForbiddenPatternsFile() {
        return getDataFolder().toPath().resolve(ForbiddenPatternLoader.FILE_NAME);
    }

    private final class ReloadCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
                sender.sendMessage("Usage: /" + label + " reload");
                return true;
            }
            try {
                reloadConfig();
                NameFilterConfig config = NameFilterConfigParser.parse(getConfig().getValues(false));
                runtime.reload(config, getForbiddenPatternsFile());
                sender.sendMessage("NameFilter configuration and forbidden patterns reloaded.");
            } catch (IOException | RuntimeException error) {
                getLogger().warning("Could not reload NameFilter: " + error.getMessage());
                sender.sendMessage("NameFilter reload failed; the previous settings remain active.");
            }
            return true;
        }
    }
}
