package de.manus.svc.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import org.slf4j.Logger;

@Plugin(
    id = "simplevoicechatsync",
    name = "SimpleVoiceChatSync",
    version = "1.0-SNAPSHOT",
    description = "Synchronizes Simple Voice Chat groups across a Velocity network.",
    authors = {"Manus"}
)
public class SimpleVoiceChatSyncVelocity {

    public static final String CHANNEL_NAME = "simplevoicechatsync:main";
    public static final ChannelIdentifier CHANNEL = new LegacyChannelIdentifier(CHANNEL_NAME);

    private final ProxyServer server;
    private final Logger logger;
    private GroupSyncManager groupSyncManager;

    @Inject
    public SimpleVoiceChatSyncVelocity(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        this.groupSyncManager = new GroupSyncManager(server, logger);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("SimpleVoiceChatSyncVelocity initializing...");
        
        server.getChannelRegistrar().register(CHANNEL);
        server.getEventManager().register(this, groupSyncManager);

        logger.info("SimpleVoiceChatSyncVelocity initialized.");
    }

    public GroupSyncManager getGroupSyncManager() {
        return groupSyncManager;
    }
}
