package com.namefilter.paper;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.namefilter.core.DailyAuditLogger;
import com.namefilter.core.ForbiddenPatternLoader;
import com.namefilter.core.HandlingMode;
import com.namefilter.core.MatchResult;
import com.namefilter.core.NameFilterConfig;
import com.namefilter.core.NameFilterConfigParser;
import com.namefilter.core.NameMatcher;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

/** Paper entry point. */
public final class NameFilterPaperPlugin extends JavaPlugin implements Listener {
    private NameFilterConfig filterConfig;
    private NameMatcher matcher;
    private DailyAuditLogger auditLogger;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.filterConfig = NameFilterConfigParser.parse(getConfig().getValues(false));
        try {
            this.matcher = new NameMatcher(
                    filterConfig,
                    ForbiddenPatternLoader.load(
                            getDataFolder().toPath().resolve(ForbiddenPatternLoader.FILE_NAME)));
        } catch (IOException error) {
            throw new IllegalStateException("Could not load forbidden name patterns", error);
        }
        this.auditLogger = new DailyAuditLogger(getDataFolder().toPath().resolve("log"));
        getServer().getPluginManager().registerEvents(this, this);
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
        MatchResult match = matcher.match(playerName);
        if (!match.isMatch()) {
            return;
        }

        Component reason = filterConfig.handlingMode() == HandlingMode.SILENT
                ? Component.empty()
                : Component.text(filterConfig.rejectionMessage());
        event.kickMessage(reason);

        String remoteAddress = String.valueOf(connection.getClientAddress());
        auditLogger.record(playerName, remoteAddress, match, "Paper", filterConfig.handlingMode())
                .exceptionally(error -> {
                    getLogger().warning("Could not write NameFilter audit record: " + error.getMessage());
                    return null;
                });
    }
}
