// java
package de.leonmonkeygamer.SVCGroupSyncPaper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PluginMessaging implements org.bukkit.plugin.messaging.PluginMessageListener {

    private final JavaPlugin plugin;
    // Store player group assignments locally so we can assign players when they
    // connect
    private final Map<UUID, UUID> playerGroupAssignments = new ConcurrentHashMap<>();
    // Store group members (group ID -> set of player UUIDs) to track all players in
    // each group
    private final Map<UUID, java.util.Set<UUID>> groupMembers = new ConcurrentHashMap<>();

    public PluginMessaging(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        String jsonString = new String(message, StandardCharsets.UTF_8);
        try {
            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
            String action = json.get("action").getAsString();

            if ("SYNC_UPDATE".equals(action)) {
                handleSyncUpdate(json);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Exception in plugin message handler: " + e.getMessage(), e);
        }
    }

    private void handleSyncUpdate(JsonObject json) {

        VoicechatServerApi api = (VoicechatServerApi) GroupSyncPaperVoicechatPlugin.getApi();

        plugin.getLogger().info("API: " + api);

        if (api == null) {
            plugin.getLogger().warning("SVC API not initialized yet.");
            return;
        }

        GroupSyncPaperVoicechatPlugin.setSyncInProgress(true);
        try {
            // Create Group
            JsonObject groupsJson = json.getAsJsonObject("groups");
            for (Map.Entry<String, JsonElement> entry : groupsJson.entrySet()) {
                try {
                    plugin.getLogger()
                            .info("[DEBUG SYNC] Entry key: " + entry.getKey() + " value: " + entry.getValue());

                    JsonElement value = entry.getValue();
                    JsonObject groupData;

                    if (value.isJsonObject()) {
                        groupData = value.getAsJsonObject();
                        plugin.getLogger().info("[DEBUG SYNC] value is JsonObject");
                    } else if (value.isJsonPrimitive()) {
                        groupData = JsonParser.parseString(value.getAsString()).getAsJsonObject();
                        plugin.getLogger().info("[DEBUG SYNC] value is JsonPrimitive, parsed string to JsonObject");
                    } else {
                        plugin.getLogger()
                                .warning("[DEBUG SYNC] Unexpected JSON element type for group: " + entry.getKey());
                        continue;
                    }

                    if (!groupData.has("group_id")) {
                        plugin.getLogger().warning("Skipping group without group_id: " + entry.getKey());
                        continue;
                    }

                    UUID groupId = UUID.fromString(groupData.get("group_id").getAsString());
                    String name = groupData.has("group_name") ? groupData.get("group_name").getAsString() : "unknown";
                    String groupTag = groupData.has("group_type") ? groupData.get("group_type").getAsString()
                            : "PUBLIC";

                    plugin.getLogger().info("[DEBUG SYNC] Group ID: " + groupId);
                    plugin.getLogger().info("[DEBUG SYNC] Name: " + name);
                    plugin.getLogger().info("[DEBUG GROUPTYPE] Received group type: " + groupTag);

                    Group.Type type;
                    switch (groupTag) {
                        case "PUBLIC":
                            type = Group.Type.NORMAL;
                            break;
                        case "OPEN":
                            type = Group.Type.OPEN;
                            break;
                        case "ISOLATED":
                            type = Group.Type.ISOLATED;
                            break;
                        default:
                            type = Group.Type.NORMAL;
                            plugin.getLogger()
                                    .info("No valid group type found for " + name + ", defaulting to NORMAL.");
                            break;
                    }

                    api.groupBuilder()
                            .setPersistent(true)
                            .setId(groupId)
                            .setName(name)
                            .setType(type)
                            .build();

                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Fehler beim Verarbeiten der Gruppe " + entry.getKey(), e);
                }
            }

            // Update local player group assignments and group members
            JsonObject assignmentsJson = json.getAsJsonObject("assignments");
            playerGroupAssignments.clear();
            groupMembers.clear();

            for (Map.Entry<String, JsonElement> entry : assignmentsJson.entrySet()) {
                try {
                    UUID playerUUID = UUID.fromString(entry.getKey());
                    UUID groupId = UUID.fromString(entry.getValue().getAsString());
                    playerGroupAssignments.put(playerUUID, groupId);

                    // Track group members
                    groupMembers.computeIfAbsent(groupId, k -> ConcurrentHashMap.newKeySet()).add(playerUUID);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error parsing assignment: " + entry.getKey(), e);
                }
            }

            // Add Players to Groups - assign all players who are online on this server
            plugin.getServer().getOnlinePlayers().forEach(onlinePlayer -> {
                UUID playerUUID = onlinePlayer.getUniqueId();
                VoicechatConnection connection = api.getConnectionOf(playerUUID);
                if (connection == null)
                    return;

                UUID groupId = playerGroupAssignments.get(playerUUID);
                if (groupId != null) {
                    try {
                        Group targetGroup = api.getGroup(groupId);
                        if (targetGroup != null) {
                            connection.setGroup(targetGroup);
                        } else {
                            connection.setGroup(null);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Error putting players in groups: " + e.getMessage(), e);
                    }
                } else {
                    connection.setGroup(null);
                }
            });

            plugin.getLogger().info("Sync-Update Done.");
        } finally {
            GroupSyncPaperVoicechatPlugin.setSyncInProgress(false);
        }
    }

    /**
     * Assigns a player to their group when they connect to voice chat.
     * This ensures that players who join a server are assigned to their groups.
     */
    public void assignPlayerToGroup(UUID playerUUID) {
        VoicechatServerApi api = (VoicechatServerApi) GroupSyncPaperVoicechatPlugin.getApi();
        if (api == null) {
            return;
        }

        UUID groupId = playerGroupAssignments.get(playerUUID);
        if (groupId != null) {
            try {
                VoicechatConnection connection = api.getConnectionOf(playerUUID);
                if (connection != null) {
                    Group targetGroup = api.getGroup(groupId);
                    if (targetGroup != null) {
                        connection.setGroup(targetGroup);
                        plugin.getLogger()
                                .info("Assigned player " + playerUUID + " to group " + groupId + " on connect.");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error assigning player to group: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Gets the group assignment for a player.
     */
    public UUID getPlayerGroup(UUID playerUUID) {
        return playerGroupAssignments.get(playerUUID);
    }

    /**
     * Gets all players in a group, including those on other servers.
     * This is useful for tracking cross-server group members.
     * 
     * @param groupId The group ID
     * @return Set of player UUIDs in the group, or empty set if group doesn't exist
     */
    public java.util.Set<UUID> getGroupMembers(UUID groupId) {
        return groupMembers.getOrDefault(groupId, java.util.Collections.emptySet());
    }

    /**
     * Gets all players in a group who are currently online on this server.
     * 
     * @param groupId The group ID
     * @return Set of player UUIDs in the group who are online on this server
     */
    public java.util.Set<UUID> getOnlineGroupMembers(UUID groupId) {
        java.util.Set<UUID> allMembers = getGroupMembers(groupId);
        java.util.Set<UUID> onlineMembers = new java.util.HashSet<>();

        for (UUID playerUUID : allMembers) {
            Player player = plugin.getServer().getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                onlineMembers.add(playerUUID);
            }
        }

        return onlineMembers;
    }

    /**
     * Gets all players in a group who are on other servers (not online on this
     * server).
     * These are the players that might need "fake" representation.
     * 
     * @param groupId The group ID
     * @return Set of player UUIDs in the group who are on other servers
     */
    public java.util.Set<UUID> getCrossServerGroupMembers(UUID groupId) {
        java.util.Set<UUID> allMembers = getGroupMembers(groupId);
        java.util.Set<UUID> crossServerMembers = new java.util.HashSet<>();

        for (UUID playerUUID : allMembers) {
            Player player = plugin.getServer().getPlayer(playerUUID);
            if (player == null || !player.isOnline()) {
                crossServerMembers.add(playerUUID);
            }
        }

        return crossServerMembers;
    }

}
