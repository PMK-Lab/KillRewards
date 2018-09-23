package com.gmail.falistos.KillRewards;

import com.gmail.falistos.KillRewards.EventListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class KillRewards extends JavaPlugin implements Listener {

   public Economy economy;
   public List<UUID> spawnerEntities = new ArrayList<UUID>();


   public void onEnable() {
      if(this.getServer().getPluginManager().getPlugin("Vault") == null) {
         this.getLogger().severe("Vault was not found, disabling.");
         this.getServer().getPluginManager().disablePlugin(this);
      } else if(!this.setupEconomy()) {
         this.getLogger().severe("Vault supported economy plugin was not found, disabling.");
         this.getServer().getPluginManager().disablePlugin(this);
      } else {
         this.getConfig().options().copyDefaults(true);
         this.saveDefaultConfig();
         this.getServer().getPluginManager().registerEvents(new EventListener(this), this);
      }
   }

   private boolean setupEconomy() {
      RegisteredServiceProvider<Economy> economyProvider = this.getServer().getServicesManager().getRegistration(Economy.class);
      if(economyProvider != null) {
         this.economy = (Economy)economyProvider.getProvider();
      }

      return this.economy != null;
   }

   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if(cmd.getName().equalsIgnoreCase("killrewards")) {
         if(args.length == 0) {
            sender.sendMessage(ChatColor.RED + "/killrewards [reload]");
            return true;
         }

         if(args[0].equalsIgnoreCase("reload")) {
            if(!sender.hasPermission("killrewards.reload")) {
               sender.sendMessage(ChatColor.RED + "You don\'t have permission to do this.");
               return true;
            }

            this.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + this.getName() + " version " + this.getDescription().getVersion() + " reloaded.");
            return true;
         }

         if(args[0].equalsIgnoreCase("info")) {
            sender.sendMessage(ChatColor.GOLD + this.getName() + "\n" + ChatColor.GREEN + "Version " + this.getDescription().getVersion() + "\n" + ChatColor.LIGHT_PURPLE + "Created by Falistos (falistos@gmail.fr)");
            return true;
         }
      }

      return false;
   }

   protected void sendMessage(Player player, String killerName, String victimName, String message, Double reward) {
      if(message != null) {
         message = message.replace("%player", killerName);
         message = message.replace("%victim", victimName);
         message = message.replace("%reward", reward.toString());
         message = message.replace("%currency", this.getConfig().getString("currency", "$"));
         player.sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("messagePrefix") + message));
      }
   }

   protected boolean isFromSpawner(UUID uuid) {
      return this.spawnerEntities.contains(uuid);
   }

   public double getPermissionMultiplier(Player player) {
      double multiplier = 1.0D;
      ConfigurationSection permissionsSection = this.getConfig().getConfigurationSection("permissionMultiplier");
      Iterator<String> var6 = permissionsSection.getKeys(false).iterator();

      while(var6.hasNext()) {
         String key = (String)var6.next();
         if(player.hasPermission("killrewards.multiplier." + key)) {
            Double permissionMultiplier = Double.valueOf(this.getConfig().getDouble("permissionMultiplier." + key));
            if(permissionMultiplier.doubleValue() != 0.0D && permissionMultiplier.doubleValue() > multiplier) {
               multiplier = permissionMultiplier.doubleValue();
            }
         }
      }

      return multiplier;
   }
}