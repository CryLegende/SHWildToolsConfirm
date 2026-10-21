package org.Cry_Legende.sHWildToolsConfirm;

import com.bgsoftware.wildtools.api.WildToolsAPI;
import com.bgsoftware.wildtools.api.events.SellWandUseEvent;
import com.bgsoftware.wildtools.api.objects.tools.SellTool;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SHWildToolsConfirm extends JavaPlugin implements Listener {

    private final Map<UUID, Confirmation> confirmations = new ConcurrentHashMap<>();
    private String confirmationMessage;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("WildTools") == null) {
            getLogger().severe("WildTools n'a pas été trouvé ! Le plugin SHWildToolsConfirm va se désactiver.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saveDefaultConfig();
        loadConfigMessage();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("SHWildToolsConfirm a été activé avec succès.");
    }

    @Override
    public void onDisable() {
        confirmations.clear();
    }

    private void loadConfigMessage() {
        this.confirmationMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("confirmation-message", "&eCliquez à nouveau sur le même coffre pour confirmer la vente de son contenu pour &6{price} &emoney."));
    }

    /**
     * ÉCOUTEUR N°1 : Le Gardien (Inchangé)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        SellTool sellTool = WildToolsAPI.getSellTool(itemInHand);

        if (sellTool == null) {
            return;
        }

        if (confirmations.containsKey(player.getUniqueId()) && confirmations.get(player.getUniqueId()).isFor(event.getClickedBlock())) {
            return;
        }

        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
    }

    /**
     * ÉCOUTEUR N°2 : Le Logicien (Version Corrigée)
     * Gère la confirmation de la vente en incluant le multiplicateur.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSellWandUse(SellWandUseEvent event) {
        if (event.getPrice() <= 0) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        Block containerBlock = event.getContainer().getBlock();

        if (confirmations.containsKey(playerUUID)) {
            Confirmation confirmation = confirmations.get(playerUUID);
            if (confirmation.isFor(containerBlock)) {
                confirmations.remove(playerUUID);
                return;
            }
        }

        event.setCancelled(true);
        confirmations.put(playerUUID, new Confirmation(containerBlock));

        // --- MODIFICATION ICI ---
        // On récupère le prix de base ET le multiplicateur
        double basePrice = event.getPrice();
        double multiplier = event.getMultiplier();
        // On calcule le prix final
        double finalPrice = basePrice * multiplier;

        // On affiche le prix final dans le message
        String message = confirmationMessage.replace("{price}", String.format("%,.2f", finalPrice));
        player.sendMessage(message);
    }

    // Classe interne de confirmation (Inchangée)
    private static class Confirmation {
        private final Block containerBlock;
        private final long timestamp;
        private static final long TIMEOUT = 10000;

        public Confirmation(Block containerBlock) {
            this.containerBlock = containerBlock;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isFor(Block block) {
            if (block == null) return false;
            boolean sameWorld = this.containerBlock.getWorld().equals(block.getWorld());
            boolean sameLocation = this.containerBlock.getX() == block.getX() &&
                this.containerBlock.getY() == block.getY() &&
                this.containerBlock.getZ() == block.getZ();
            boolean notExpired = (System.currentTimeMillis() - timestamp) < TIMEOUT;
            return sameWorld && sameLocation && notExpired;
        }
    }
}
