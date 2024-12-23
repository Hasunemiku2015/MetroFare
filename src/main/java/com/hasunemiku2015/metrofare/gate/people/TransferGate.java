package com.hasunemiku2015.metrofare.gate.people;

import com.hasunemiku2015.metrofare.company.AbstractCompany;
import com.hasunemiku2015.metrofare.company.CompanyStore;
import com.hasunemiku2015.metrofare.company.CompanyType;
import com.hasunemiku2015.metrofare.MetroConfiguration;
import com.hasunemiku2015.metrofare.MetroFare;
import com.hasunemiku2015.metrofare.ticketing.types.Ticket;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class TransferGate implements Listener {
    @EventHandler
    public void onTransferGateBuild(SignChangeEvent event) {
        if (event.getLine(0) == null) {
            return;
        }
        if (!MetroConfiguration.INSTANCE.getPrefixTransfer().equalsIgnoreCase(event.getLine(0))) {
            return;
        }

        if (!(event.getBlock().getState().getBlockData() instanceof WallSign)) {
            event.getBlock().setType(Material.AIR);
            return;
        }
        if (!MetroConfiguration.INSTANCE.hasBuildGatePermission(event.getPlayer())) {
            event.getBlock().setType(Material.AIR);
            return;
        }

        // Syntax Check
        if (event.getLine(1) == null || event.getLine(2) == null) {
            event.getBlock().setType(Material.AIR);
            return;
        }
        String[] data1 = GateUtil.parseData(Objects.requireNonNull(event.getLine(1)));
        String[] data2 = GateUtil.parseData(Objects.requireNonNull(event.getLine(2)));
        if (data1.length != 2 || data2.length != 2) {
            event.getBlock().setType(Material.AIR);
            return;
        }
        if (!CompanyStore.CompanyTable.containsKey(data1[0]) || !CompanyStore.CompanyTable.containsKey(data2[0])) {
            event.getBlock().setType(Material.AIR);
            return;
        }

        if ((CompanyStore.CompanyTable.get(data1[0])).getType() != CompanyType.UNIFORM && data1[1] == null) {
            event.getBlock().setType(Material.AIR);
            return;
        }
        if ((CompanyStore.CompanyTable.get(data2[0])).getType() != CompanyType.UNIFORM && data2[1] == null) {
            event.getBlock().setType(Material.AIR);
            return;
        }

        //Prettify
        String line2 = event.getLine(1);
        String line3 = event.getLine(2);
        event.setLine(1, line2 + ";" + line3);
        event.setLine(2, MetroConfiguration.INSTANCE.getInfo1Transfer() + data2[0]);
        event.setLine(3, MetroConfiguration.INSTANCE.getInfo2Transfer() + data2[0]);
    }

    @EventHandler
    public void onTransferGateUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        if (!((event.getClickedBlock().getState()) instanceof Sign)) {
            return;
        }
        Sign sign = (Sign) event.getClickedBlock().getState();

        if (GateUtil.checkValid(sign, MetroConfiguration.INSTANCE.getPrefixTransfer()) && GateUtil.validFace(sign, event.getBlockFace())) {
            event.setCancelled(true);
            String[] var0 = sign.getLine(1).split(";");
            AbstractCompany exitCompany = CompanyStore.CompanyTable.get(var0[0].split(",")[0]);
            AbstractCompany enterCompany = CompanyStore.CompanyTable.get(var0[1].split(",")[0]);

            ItemStack stack = event.getItem();
            if (stack == null) {
                return;
            }

            boolean openGate = false;
            if (stack.getType().equals(Material.NAME_TAG)) {
                boolean openGate1 = GateExecutionOut.DCExitLogic(event.getPlayer(), exitCompany, stack, var0[0]);
                if (openGate1) {
                    openGate = GateExecutionIn.DCEntryLogic(event.getPlayer(), stack, var0[1], true);
                }
            } else if (stack.getType().equals(Material.PAPER)) {
                Ticket ticket = new Ticket(stack);
                if (!ticket.isValid()) {
                    return;
                }
                if (!ticket.hasEntered()) {
                    return;
                }

                if (ticket.checkExitCompany(enterCompany)) {
                    event.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
                            MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getChatTicketTransfer()));
                    openGate = true;
                } else {
                    event.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
                            MetroConfiguration.INSTANCE.getBase() + MetroConfiguration.INSTANCE.getPrefix() + " " + MetroConfiguration.INSTANCE.getChatTicketErrorTransfer()));
                }
            }

            if (openGate) {
                sign.setLine(2, MetroConfiguration.INSTANCE.getTransient1Transfer());
                sign.setLine(3, MetroConfiguration.INSTANCE.getTransient2Transfer());
                sign.update();

                final Location signCoordinate = sign.getLocation();
                Bukkit.getScheduler().runTaskLater(MetroFare.PLUGIN, () -> {
                    Sign updateSign = ((Sign) signCoordinate.getBlock());
                    updateSign.setLine(2, MetroConfiguration.INSTANCE.getInfo1Transfer() + enterCompany.getName() +
                            MetroConfiguration.INSTANCE.getInfo3Transfer());
                    updateSign.setLine(3, MetroConfiguration.INSTANCE.getInfo2Transfer() + enterCompany.getName() +
                            MetroConfiguration.INSTANCE.getInfo4Transfer());
                    sign.update();
                }, MetroConfiguration.INSTANCE.getOpenTime());

                GateUtil.setBlock(sign);
            }
        }
    }
}
