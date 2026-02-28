package de.leonmonkeygamer.SVCGroupSyncPaper;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class GroupListCommand implements CommandExecutor {

    private final GroupSyncPaperPlugin plugin;

    public GroupListCommand(GroupSyncPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        PluginMessaging messaging = plugin.getPluginMessaging();
        
        if (messaging == null) {
            player.sendMessage("§cPlugin messaging not initialized.");
            return true;
        }

        UUID playerGroup = messaging.getPlayerGroup(player.getUniqueId());
        if (playerGroup == null) {
            player.sendMessage("§eYou are not in a group.");
            return true;
        }

        Set<UUID> allMembers = messaging.getGroupMembers(playerGroup);
        Set<UUID> onlineMembers = messaging.getOnlineGroupMembers(playerGroup);
        Set<UUID> crossServerMembers = messaging.getCrossServerGroupMembers(playerGroup);

        player.sendMessage("§6=== Group Members ===");
        player.sendMessage("§7Total members: §f" + allMembers.size());
        
        if (!onlineMembers.isEmpty()) {
            player.sendMessage("§aOnline on this server:");
            for (UUID memberId : onlineMembers) {
                Player member = plugin.getServer().getPlayer(memberId);
                if (member != null) {
                    player.sendMessage("§7  - §f" + member.getName());
                }
            }
        }

        if (!crossServerMembers.isEmpty()) {
            player.sendMessage("§eOn other servers:");
            for (UUID memberId : crossServerMembers) {
                // Try to get player name from server (might not work for offline players)
                Player member = plugin.getServer().getPlayer(memberId);
                if (member != null) {
                    player.sendMessage("§7  - §f" + member.getName() + " §7(offline)");
                } else {
                    // Could use a UUID-to-name cache here
                    player.sendMessage("§7  - §f" + memberId.toString().substring(0, 8) + "...");
                }
            }
        }

        return true;
    }
}


