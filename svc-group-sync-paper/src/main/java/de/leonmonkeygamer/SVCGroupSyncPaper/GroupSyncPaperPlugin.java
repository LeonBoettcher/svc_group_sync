package de.leonmonkeygamer.SVCGroupSyncPaper;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public class GroupSyncPaperPlugin extends JavaPlugin {

    public static final String PLUGIN_ID = "svc_group_sync_paper";
    public static final Logger LOGGER = LogManager.getLogger(PLUGIN_ID);

    @Nullable
    private GroupSyncPaperVoicechatPlugin voicechatPlugin;

    @Nullable
    private PluginMessaging pluginMessaging;

    @Nullable
    private static GroupSyncPaperPlugin instance;

    @Override
    public void onEnable() {
        instance = this;

        BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            voicechatPlugin = new GroupSyncPaperVoicechatPlugin(this);
            service.registerPlugin(voicechatPlugin);
            LOGGER.info("Successfully registered example plugin");
        } else {
            LOGGER.info("Failed to register example plugin");
        }

        // Register Message Channel
        pluginMessaging = new PluginMessaging(this);
        getServer().getMessenger().registerIncomingPluginChannel(this, "svc_group_sync:main", pluginMessaging);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "svc_group_sync:main");

        // Register commands
        if (getServer().getPluginCommand("grouplist") != null) {
            getServer().getPluginCommand("grouplist").setExecutor(new GroupListCommand(this));
        }

    }

    @Nullable
    public static GroupSyncPaperPlugin getInstance() {
        return instance;
    }

    @Nullable
    public PluginMessaging getPluginMessaging() {
        return pluginMessaging;
    }

    @Override
    public void onDisable() {
        if (voicechatPlugin != null) {
            getServer().getServicesManager().unregister(voicechatPlugin);

            getServer().getMessenger().unregisterIncomingPluginChannel(this, "simplevoicechatsync:main");
            getServer().getMessenger().unregisterOutgoingPluginChannel(this, "simplevoicechatsync:main");
            LOGGER.info("Successfully unregistered example plugin");
        }
    }
}
