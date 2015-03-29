package com.gmail.falistos.KillRewards;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
	
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
    
		this.getServer().getPluginManager().registerEvents(this, this);
	}
  
	private void reload() {
		this.reloadConfig();
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
				this.reload();
        
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
  
	private void sendMessage(Player player, String killerName, String victimName, String message, Double reward) {
		if (message == null) return;
		
		message = message.replace("%player", killerName);
		message = message.replace("%victim", victimName);
		message = message.replace("%reward", reward.toString());
		message = message.replace("%currency", getConfig().getString("currency", "$"));
    
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messagePrefix") + message));
	}
  
	private static double round(double value, int places) {
		if (places < 0) throw new IllegalArgumentException();
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}
  
	@EventHandler
	public void onCreatureEntitySpawn(CreatureSpawnEvent event) {
		if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
     	this.spawnerEntities.add(event.getEntity().getUniqueId());
		}
	}
  
	private boolean isFromSpawner(UUID uuid) {
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
  
	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		Player killer = event.getEntity().getKiller();
		LivingEntity victim = event.getEntity();
		if (killer == null) return;
		String section = victim.getType().toString();
		
		if ((killer instanceof Player)) {
			if (!killer.hasPermission("killrewards.earn")) return;
			if (!getConfig().getBoolean("rewards." + section + ".enabled")) return;
			if ((this.isFromSpawner(victim.getUniqueId())) && (!getConfig().getBoolean("spawners.enable"))) return;
			
			double reward = 0.0D;
      
			String victimName = null;
			String message = null;
			
			if ((victim instanceof Player)) {
				Player victimPlayer = (Player)victim;
        
				victimName = victimPlayer.getDisplayName();
        
				double percentageTransfert = getConfig().getDouble("rewards.PLAYER.percentageTransfert");
				reward = this.economy.getBalance(victimPlayer.getName()) * percentageTransfert;
				reward = round(reward, 2);
				if (reward > getConfig().getDouble("rewards.PLAYER.maximumTransfert")) {
					reward = getConfig().getDouble("rewards.PLAYER.maximumTransfert");
				}
				this.economy.withdrawPlayer(victimPlayer.getName(), reward);
        
				this.sendMessage(victimPlayer, killer.getDisplayName(), victimName, getConfig().getString("rewards.PLAYER.victimMessage"), Double.valueOf(reward));
			}
			else {
				victimName = getConfig().getString("rewards." + section + ".name");
				
				if ((victim.getCustomName() != null) && (getConfig().isConfigurationSection("rewards." + ChatColor.stripColor(victim.getCustomName())))) {
					victimName = victim.getCustomName();
					section = ChatColor.stripColor(victim.getCustomName());
				}
				
				double minReward = getConfig().getDouble("rewards." + section + ".minReward");
				double maxReward = getConfig().getDouble("rewards." + section + ".maxReward");
        
				double random = new Random().nextDouble();
				reward = minReward + random * (maxReward - minReward);
				
				if (getConfig().getDouble("globalMultiplier") != 0.0D) {
					reward *= getConfig().getDouble("globalMultiplier");
				}
				
				reward *= getPermissionMultiplier(killer);
				
				if ((isFromSpawner(victim.getUniqueId())) && (getConfig().getBoolean("spawners.enable"))) {
					reward *= getConfig().getDouble("spawners.multiplier");
          
					int droppedExp = (int)(event.getDroppedExp() * getConfig().getDouble("spawners.experienceMultiplier"));
					if (droppedExp <= 0) {
						droppedExp = 1;
					}
					event.setDroppedExp(droppedExp);
				}
				reward = round(reward, 2);
			}
			
			if (getConfig().getString("rewards." + section + ".killerMessage") != null) {
				message = getConfig().getString("rewards." + section + ".killerMessage");
			}
			else message = getConfig().getString("rewards.DEFAULT.killerMessage");
			
			this.economy.depositPlayer(killer.getName(), reward);
      
			this.sendMessage(killer, killer.getDisplayName(), victimName, message, Double.valueOf(reward));
		}
	}
}