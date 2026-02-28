package de.manus.svc.velocity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.PluginMessageSource;
import de.maxhenkel.voicechat.api.Group;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GroupSyncManager {

    private final ProxyServer server;
    private final Logger logger;
    private final Gson gson = new Gson();

    // Gruppen-ID -> SyncedGroup (vereinfacht, da wir nur die Metadaten speichern)
    private final Map<UUID, JsonObject> groups = new ConcurrentHashMap<>();
    // Spieler-UUID -> Gruppen-ID (null, wenn keine Gruppe)
    private final Map<UUID, UUID> playerGroupAssignments = new ConcurrentHashMap<>();

    public GroupSyncManager(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
            return;
        }

        PluginMessageSource source = event.getSource();
            return;
        }

        String message = new String(event.getData(), StandardCharsets.UTF_8);
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String action = json.get("action").getAsString();
            UUID playerUUID = UUID.fromString(json.get("player_uuid").getAsString());

            switch (action) {
                case "GROUP_CREATE":
                    handleGroupCreate(json, playerUUID);
                    break;
                case "GROUP_JOIN":
                    handleGroupJoin(json, playerUUID);
                    break;
                case "GROUP_LEAVE":
                    handleGroupLeave(playerUUID);
                    break;
                default:
                    logger.warn("Unknown action from PaperMC: " + action);
            }
            
            sendSyncUpdateToAllServers();

        } catch (Exception e) {
            logger.error("Error processing plugin message: " + message, e);
        }
    }

    private void handleGroupCreate(JsonObject json, UUID creatorUUID) {
        try {
            UUID groupId = UUID.fromString(json.get("group_id").getAsString());
            
            // Speichere die gesamte Gruppen-JSON-Struktur
            groups.put(groupId, json);
            playerGroupAssignments.put(creatorUUID, groupId);
            logger.info("Group created: " + json.get("group_name").getAsString() + " (" + groupId + ") by " + creatorUUID);

        } catch (Exception e) {
            logger.error("Error creating group", e);
        }
    }

    private void handleGroupJoin(JsonObject json, UUID playerUUID) {
        try {
            UUID groupId = UUID.fromString(json.get("group_id").getAsString());
            if (groups.containsKey(groupId)) {
                playerGroupAssignments.put(playerUUID, groupId);
                logger.info("Player " + playerUUID + " joined group " + groupId + ".");
            } else {
                logger.warn("Player " + playerUUID + " tried to join unknown group " + groupId + ".");
            }
        } catch (Exception e) {
            logger.error("Error joining group", e);
        }
    }

    private void handleGroupLeave(UUID playerUUID) {
        UUID oldGroupId = playerGroupAssignments.remove(playerUUID);
        if (oldGroupId != null) {
            logger.info("Player " + playerUUID + " left group " + oldGroupId + ".");
            
            // Check if the group is empty
                groups.remove(oldGroupId);
                logger.info("Group " + oldGroupId + " removed as it is empty.");
            }
        }
    }

    public void sendSyncUpdateToAllServers() {
        JsonObject syncData = new JsonObject();
        syncData.addProperty("action", "SYNC_UPDATE");

        // 1. Gruppen-Daten
        JsonObject groupsJson = new JsonObject();
        for (Map.Entry<UUID, JsonObject> entry : groups.entrySet()) {
            // Wir speichern die Gruppen-JSON direkt, um sie im Paper-Plugin zu verwenden
            groupsJson.add(entry.getKey().toString(), entry.getValue());
        }
        syncData.add("groups", groupsJson);

        // 2. Spieler-Gruppen-Zuweisungen
        JsonObject assignmentsJson = new JsonObject();
        for (Map.Entry<UUID, UUID> entry : playerGroupAssignments.entrySet()) {
            assignmentsJson.addProperty(entry.getKey().toString(), entry.getValue().toString());
        }
        syncData.add("assignments", assignmentsJson);

        String message = syncData.toString();
        byte[] data = message.getBytes(StandardCharsets.UTF_8);

        server.getAllServers().forEach(serverInfo -> {
            serverInfo.sendPluginMessage(SimpleVoiceChatSyncVelocity.CHANNEL, data);
        });
        
        logger.info("Sync update sent to all " + server.getAllServers().size() + " servers. Groups: " + groups.size() + ", Assignments: " + playerGroupAssignments.size());
    }
}
