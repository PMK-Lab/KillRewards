package com.gmail.falistos.KillRewards;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			this.getLogger().severe("Vault was not found, disabling.");
			this.getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		if (!setupEconomy()) {
			this.getLogger().severe("Vault supported economy plugin was not found, disabling.");
			this.getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		this.getConfig().options().copyDefaults(true);
		this.saveDefaultConfig();
    
		this.getServer().getPluginManager().registerEvents(new EventListener(this), this);
	}
  
	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
		if (economyProvider != null) {
			this.economy = ((Economy)economyProvider.getProvider());
		}
		return this.economy != null;
	}
  
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("killrewards")) {
			if (args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/killrewards [reload]");
				return true;
			}
			if (args[0].equalsIgnoreCase("reload")) {
				if (!sender.hasPermission("killrewards.reload")) {
					sender.sendMessage(ChatColor.RED + "You don't have permission to do this.");
					return true;
				}
				this.reloadConfig();
        
				sender.sendMessage(ChatColor.GREEN + getName() + " version " + getDescription().getVersion() + " reloaded.");
				return true;
			}
			if (args[0].equalsIgnoreCase("info")) {
				sender.sendMessage(ChatColor.GOLD + getName() + "\n" + ChatColor.GREEN + "Version " + getDescription().getVersion() + "\n" + ChatColor.LIGHT_PURPLE + "Created by Falistos (falistos@gmail.fr)");
				return true;
			}
		}
		return false;
	}
  
	protected void sendMessage(Player player, String killerName, String victimName, String message, Double reward) {
		if (message == null) return;
		
		message = message.replace("%player", killerName);
		message = message.replace("%victim", victimName);
		message = message.replace("%reward", reward.toString());
		message = message.replace("%currency", getConfig().getString("currency", "$"));
    
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messagePrefix") + message));
	}

	protected boolean isFromSpawner(UUID uuid) {
		if (this.spawnerEntities.contains(uuid)) return true;
		return false;
 	}
  
	public double getPermissionMultiplier(Player player) {
		double multiplier = 1.0D;
    
		ConfigurationSection permissionsSection = getConfig().getConfigurationSection("permissionMultiplier");
		for (String key : permissionsSection.getKeys(false)) {
			if (player.hasPermission("killrewards.multiplier." + key)) {
				Double permissionMultiplier = Double.valueOf(getConfig().getDouble("permissionMultiplier." + key));
				if ((permissionMultiplier.doubleValue() != 0.0D) && (permissionMultiplier.doubleValue() > multiplier)) {
					multiplier = permissionMultiplier.doubleValue();
				}
			}
		}
		return multiplier;
	}

}