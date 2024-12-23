package com.hasunemiku2015.metrofare.ticketing.sign;

import com.hasunemiku2015.metrofare.MetroConfiguration;
import com.hasunemiku2015.metrofare.MetroFare;
import com.hasunemiku2015.metrofare.VaultIntegration;
import com.hasunemiku2015.metrofare.ticketing.commands.DebitCardCommand;
import com.hasunemiku2015.metrofare.ticketing.types.DebitCard;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.Objects;

public class DebitCardEditor implements Listener {
    private static Inventory inventoryGUI;
    private static AnvilGUI.Builder valueAdd;
    private static AnvilGUI.Builder bankIn;
    private static AnvilGUI.Builder addAmount;
    private static AnvilGUI.Builder dailyLimit;

    public static void init() {
        inventoryGUI = Bukkit.createInventory(null, 9, MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix());

        inventoryGUI.setItem(0, newItem(Material.NAME_TAG, MetroConfiguration.INSTANCE.getBase() + "New " + MetroConfiguration.INSTANCE.getDebitCardName()));
        inventoryGUI.setItem(1, newItem(Material.GOLD_INGOT, MetroConfiguration.INSTANCE.getBase() + "Add Value to " + MetroConfiguration.INSTANCE.getDebitCardName()));
        inventoryGUI.setItem(2, newItem(Material.IRON_INGOT, MetroConfiguration.INSTANCE.getBase() + "Bank in from " + MetroConfiguration.INSTANCE.getDebitCardName()));
        inventoryGUI.setItem(3, newItem(Material.BOOK, MetroConfiguration.INSTANCE.getBase() + "View Transaction Records"));
        inventoryGUI.setItem(4, newItem(Material.REDSTONE, MetroConfiguration.INSTANCE.getBase() + "Enable/Disable Auto Top-Up"));
        inventoryGUI.setItem(5, newItem(Material.REPEATER, MetroConfiguration.INSTANCE.getBase() + "Change Add Amount of Auto Top-Up"));
        inventoryGUI.setItem(6, newItem(Material.COMPARATOR, MetroConfiguration.INSTANCE.getBase() + "Change Daily Limit of Auto Top-Up"));
        inventoryGUI.setItem(7, newItem(Material.CAULDRON, MetroConfiguration.INSTANCE.getBase() + "Reset Entry data of " + MetroConfiguration.INSTANCE.getDebitCardName()));
        inventoryGUI.setItem(8, newItem(Material.BARRIER, ChatColor.RED + "Cancel"));

        valueAdd = newInputInventory(MetroConfiguration.INSTANCE.getPromptAddDCE());
        valueAdd.onClick((slot, stateSnapshot) -> {
            if (slot != AnvilGUI.Slot.OUTPUT) {
                return Collections.emptyList();
            }
            Player player = stateSnapshot.getPlayer();
            try {
                double value = Double.parseDouble(stateSnapshot.getText());
                DebitCard card = new DebitCard(stateSnapshot.getPlayer().getInventory().getItemInMainHand());
                if (value > 0 && value < 2000000) {
                    if (VaultIntegration.hasEnough(stateSnapshot.getPlayer(), value)) {
                        VaultIntegration.deduct(stateSnapshot.getPlayer(), value);
                        card.setBalance(card.getBalance() + (int) (value * 1000));
                        card.addPaymentRecord(MetroConfiguration.INSTANCE.getNameDCE(), false, (int) (value * 1000));
                        card.updateCard();

                        stateSnapshot.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getSuccessDCE()));
                        Bukkit.getScheduler().runTaskLater(MetroFare.PLUGIN, () -> stateSnapshot.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getNewBalanceDCE() + card.getBalance() / 1000.0)), 5);
                        return Collections.singletonList(AnvilGUI.ResponseAction.close());
                    }
                }
            } catch (Exception ignored) {
            }

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getFailDCE()));
            return Collections.singletonList(AnvilGUI.ResponseAction.close());
        });
        bankIn = newInputInventory(MetroConfiguration.INSTANCE.getPromptRemoveDCE());
        bankIn.onClick((slot, stateSnapshot) -> {
            if (slot != AnvilGUI.Slot.OUTPUT) {
                return Collections.emptyList();
            }
            Player player = stateSnapshot.getPlayer();
            try {
                double value = Double.parseDouble(stateSnapshot.getText());
                DebitCard card = new DebitCard(player.getInventory().getItemInMainHand());
                if (card.getBalance() / 1000.0 >= value) {
                    card.setBalance(card.getBalance() - (int) (value * 1000));
                    card.addPaymentRecord(MetroConfiguration.INSTANCE.getNameDCE(), true, (int) (value * 1000));
                    card.updateCard();
                    VaultIntegration.add(player, value);

                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getSuccessDCE()));
                    Bukkit.getScheduler().runTaskLater(MetroFare.PLUGIN, () -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getNewBalanceDCE() + card.getBalance() / 1000.0)), 5);
                    return Collections.singletonList(AnvilGUI.ResponseAction.close());
                }
            } catch (Exception ignored) {
            }

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getFailDCE()));
            return Collections.singletonList(AnvilGUI.ResponseAction.close());
        });
        addAmount = newInputInventory(MetroConfiguration.INSTANCE.getPromptAutoAddAmountDCE());
        addAmount.onClick((slot, stateSnapshot) -> {
            if (slot != AnvilGUI.Slot.OUTPUT) {
                return Collections.emptyList();
            }
            Player player = stateSnapshot.getPlayer();
            DebitCard card = new DebitCard(player.getInventory().getItemInMainHand());
            try {
                double value = Double.parseDouble(stateSnapshot.getText());
                if (value <= card.getDailyLimit() / 1000.0) {
                    if (value > 0 && value < 2000000) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getSuccessDCE()));
                        card.setAddAmount((int) (value * 1000));
                        card.updateCard();
                        return Collections.singletonList(AnvilGUI.ResponseAction.close());
                    }
                }
            } catch (Exception ignored) {
            }

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getFailDCE()));
            return Collections.singletonList(AnvilGUI.ResponseAction.close());
        });
        dailyLimit = newInputInventory(MetroConfiguration.INSTANCE.getPromptAutoDailyLimitDCE());
        dailyLimit.onClick((slot, stateSnapshot) -> {
            if (slot != AnvilGUI.Slot.OUTPUT) {
                return Collections.emptyList();
            }
            Player player = stateSnapshot.getPlayer();
            DebitCard card = new DebitCard(player.getInventory().getItemInMainHand());
            try {
                double value = Double.parseDouble(stateSnapshot.getText());

                if (value >= card.getAddAmount() / 1000.0) {
                    if (value > 0 && value < 2000000) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getSuccessDCE()));
                        card.setDailyLimit((int) (value * 1000));
                        card.updateCard();
                        return Collections.singletonList(AnvilGUI.ResponseAction.close());
                    }
                }
            } catch (Exception ignored) {
            }
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getFailDCE()));
            return Collections.singletonList(AnvilGUI.ResponseAction.close());
        });
    }

    private static ItemStack newItem(Material material, String name) {
        ItemStack stack = new ItemStack(material, 1);
        stack.addUnsafeEnchantment(Enchantment.LURE, 1);
        ItemMeta stackMeta = stack.getItemMeta();
        assert stackMeta != null;
        stackMeta.setDisplayName(name);
        stackMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        stack.setItemMeta(stackMeta);
        return stack;
    }

    private static AnvilGUI.Builder newInputInventory(String name) {
        AnvilGUI.Builder var0 = new AnvilGUI.Builder();
        var0.plugin(MetroFare.PLUGIN);
        var0.itemLeft(new ItemStack(Material.PAPER, 1));
        var0.title(name);
        return var0;
    }

    private static void removeAutoTopUp(DebitCard card) {
        card.removeAddAmount();
        card.removeAddedAmount();
        card.removeDailyLimit();
        card.removeLastAddedAuto();
        card.updateCard();
    }

    @EventHandler
    public void onBuild(SignChangeEvent event) {
        if (!(event.getBlock().getBlockData() instanceof WallSign)) return;
        if (!Objects.requireNonNull(event.getLine(0)).equalsIgnoreCase(MetroConfiguration.INSTANCE.getPrefixDCE())) return;
        if (!MetroConfiguration.INSTANCE.hasBuildEditorPermission(event.getPlayer())) {
            event.getBlock().setType(Material.AIR);
            return;
        }

        event.setLine(1, MetroConfiguration.INSTANCE.getInfo1DCE());
        event.setLine(2, MetroConfiguration.INSTANCE.getInfo2DCE());
        event.setLine(3, MetroConfiguration.INSTANCE.getInfo3DCE());
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getPlayer().isSneaking()) return;
        if (event.getClickedBlock() == null || !(event.getClickedBlock().getBlockData() instanceof WallSign)) return;
        if (((WallSign) event.getClickedBlock().getBlockData()).getFacing() != event.getBlockFace()) return;
        Sign sign = (Sign) event.getClickedBlock().getState();
        if (!sign.getLine(0).equalsIgnoreCase(MetroConfiguration.INSTANCE.getPrefixDCE())) return;
        event.setCancelled(true);
        event.getPlayer().openInventory(inventoryGUI);
    }

    @EventHandler
    public void onIGUIDrag(InventoryDragEvent event) {
        if (event.getInventory().equals(inventoryGUI)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onIGUIClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventoryGUI)) {
            Bukkit.getScheduler().runTaskLater(MetroFare.PLUGIN, () -> ((Player) event.getPlayer()).updateInventory(), 1);
        }
    }

    //Main Logic
    @EventHandler
    public void onIGUIClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventoryGUI)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        DebitCard card = new DebitCard(player.getInventory().getItemInMainHand());
        boolean valid = card.isValid();
        switch (event.getRawSlot()) {
            case 0: {
                //New Debit Card
                player.closeInventory();
                ItemStack newCard = DebitCard.newCard(player);
                for (int i = 0; i < 36; i++) {
                    if (player.getInventory().getItem(i) == null) {
                        player.getInventory().setItem(i, newCard);
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getSuccessDCE()));
                        return;
                    }
                }
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getFailDCE()));
                return;
            }
            case 1: {
                if (valid) {
                    valueAdd.open(player);
                } else {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getFailDCE()));
                    player.closeInventory();
                }
                return;
            }
            case 2: {
                if (valid) {
                    bankIn.open(player);
                } else {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getFailDCE()));
                    player.closeInventory();
                }
                return;
            }
            case 3: {
                //Transaction Records
                if (valid) {
                    DebitCardCommand.printPaymentRecord(player, card);
                    player.closeInventory();
                    return;
                }
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getFailDCE()));
                player.closeInventory();
                return;
            }
            case 4: {
                //Toggle Auto Top-Up
                if (valid) {
                    if (card.getLastAddedAuto() == 0) {
                        card.setLastAddedAuto(System.currentTimeMillis());
                        card.updateCard();
                    } else {
                        removeAutoTopUp(card);
                    }
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getSuccessDCE()));
                    player.closeInventory();
                    return;
                }
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getFailDCE()));
                player.closeInventory();
                break;
            }
            case 5: {
                //Change Add Amount
                if (valid && card.getLastAddedAuto() != 0) {
                    addAmount.open(player);
                } else {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getFailDCE()));
                    player.closeInventory();
                }
                return;
            }
            case 6: {
                //Change Daily Limit
                if (valid && card.getLastAddedAuto() != 0) {
                    dailyLimit.open(player);
                } else {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getFailDCE()));
                    player.closeInventory();
                }
                return;
            }
            case 7: {
                //Reset Entry Data
                if (valid) {
                    card.removeEntryData();
                    card.updateCard();
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getSuccessDCE()));
                    player.closeInventory();
                    return;
                }
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getFailDCE()));
                player.closeInventory();
            }
            case 8: {
                //Cancel
                player.closeInventory();
                break;
            }
        }
    }

}
