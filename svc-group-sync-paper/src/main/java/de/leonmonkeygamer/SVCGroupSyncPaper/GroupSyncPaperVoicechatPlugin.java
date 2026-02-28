package de.leonmonkeygamer.SVCGroupSyncPaper;

import com.google.gson.JsonObject;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;

public class GroupSyncPaperVoicechatPlugin implements VoicechatPlugin {

    private final JavaPlugin plugin;
    private static volatile boolean syncInProgress = false;

    public GroupSyncPaperVoicechatPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private static VoicechatServerApi api;

    public final Logger LOGGER = LogManager.getLogger(this.getPluginId());
    /**
     * @return the unique ID for this voice chat plugin
     */
    @Override
    public String getPluginId() {
        return GroupSyncPaperPlugin.PLUGIN_ID;
    }

    /**
     * Called when the voice chat initializes the plugin.
     *
     * @param api the voice chat API
     */
    @Override
    public void initialize(VoicechatApi api) {
        GroupSyncPaperVoicechatPlugin.api = (VoicechatServerApi) api;
        LOGGER.info("GroupSyncPaperVoiceChatPlugin.initialize + API: " + api);
        LOGGER.info("GroupSyncPaperVoiceChatPlugin.getapi + API: " + GroupSyncPaperVoicechatPlugin.getApi());
        if(api == null) {
            LOGGER.error("Voice Chat API is null! Plugin cannot function.");
        }
    }

    /**
     * Called once by the voice chat to register all events.
     *
     * @param registration the event registration
     */
    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(CreateGroupEvent.class, this::onGroupCreate);
        registration.registerEvent(JoinGroupEvent.class, this::onJoinGroup);
        registration.registerEvent(LeaveGroupEvent.class, this::onLeaveGroup);

    }

    public void onGroupCreate(CreateGroupEvent event) {
        if (isSyncInProgress()) {
            return;
        }
        VoicechatConnection connection = event.getConnection();

        if (connection == null) return;

        Player player = plugin.getServer().getPlayer(connection.getPlayer().getUuid());
        if (player == null) return;

        JsonObject json = createMessage("GROUP_CREATE", player, event);
        if (json == null) {
            return;
        }
        sendToVelocity(json);

        plugin.getLogger().info("GROUP_CREATE from " + player.getName() + " sent to Velocity.");
    }

    public void onRemoveGroup(RemoveGroupEvent event) {
        if (isSyncInProgress()) {
            return;
        }
        event.cancel();
        VoicechatConnection connection = event.getConnection();
        if (connection == null) return;

        Player player = plugin.getServer().getPlayer(connection.getPlayer().getUuid());
        if (player == null) return;

        JsonObject json = createMessage("GROUP_REMOVE", player, event);
        if (json == null) {
            return;
        }
        sendToVelocity(json);
        LOGGER.info("Group removed: " + event.getGroup().getId());

    }

    public void onJoinGroup(JoinGroupEvent event) {
        if (isSyncInProgress()) {
            return;
        }
        VoicechatConnection connection = event.getConnection();
        if (connection == null) return;

        Player player = plugin.getServer().getPlayer(connection.getPlayer().getUuid());
        if (player == null) return;

        JsonObject json = createMessage("GROUP_JOIN", player, event);
        if (json == null) {
            return;
        }
        sendToVelocity(json);
        LOGGER.info("Player joined group: " + event.getGroup().getId());
    }

    public void onLeaveGroup(LeaveGroupEvent event) {
        if (isSyncInProgress()) {
            return;
        }
        VoicechatConnection connection = event.getConnection();
        if (connection == null) return;

        Player player = plugin.getServer().getPlayer(connection.getPlayer().getUuid());
        if (player == null) return;

        JsonObject json = createMessage("GROUP_LEAVE", player, event);
        if (json == null) {
            return;
        }
        sendToVelocity(json);
        LOGGER.info("Player left group: " + event.getGroup().getId());
    }

    private JsonObject createMessage(String action, Player player, GroupEvent event){
        if (event.getGroup() == null) {
            LOGGER.warn("Skipping {} for player {} because event group is null.", action, player.getName());
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("action", action);
        json.addProperty("player_uuid", player.getUniqueId().toString());
        json.addProperty("group_id", event.getGroup().getId().toString());
        json.addProperty("group_name", event.getGroup().getName());
        json.addProperty("group_type", event.getGroup().getType().toString()); //TODO Not Returning a String OPEN/CLOSE/ISOLATED

        //        if(event.getGroup().hasPassword()){
        //            json.addProperty("password", event.getGroup().);
        //        }else{
        //            json.addProperty("password", "");
        //        } TODO Password support

        return json;
    }

    private void sendToVelocity(JsonObject json) {
        if (json == null) {
            return;
        }
        String message = json.toString();
        byte[] data = message.getBytes(StandardCharsets.UTF_8);

        // Einen beliebigen online-Spieler finden, um die Nachricht zu senden
        Player sender = plugin.getServer().getOnlinePlayers().stream().findAny().orElse(null);
        if (sender != null) {
            sender.sendPluginMessage(plugin, "svc_group_sync:main", data);
        }
    }

    public static void setSyncInProgress(boolean syncing) {
        syncInProgress = syncing;
    }

    public static boolean isSyncInProgress() {
        return syncInProgress;
    }
    public static VoicechatServerApi getApi() {
        return api;
    }
}