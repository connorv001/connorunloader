package de.jeff_media.InvUnload;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import de.jeff_media.ChestSort.ChestSortAPI;
import de.jeff_media.ChestSort.ChestSortPlugin;
import de.jeff_media.InvUnload.Hooks.ChestSortHook;
import de.jeff_media.InvUnload.Hooks.PlotSquaredHook;

public class Main extends JavaPlugin implements Listener {

	@Nullable
	public ChestSortAPI chestSortAPI;

	String mcVersion; // 1.13.2 = 1_13_R2
						// 1.14.4 = 1_14_R1
						// 1.8.0 = 1_8_R1
	int mcMinorVersion; // 14 for 1.14, 13 for 1.13, ...

	private int currentConfigVersion = 17;

	protected Messages messages;
	protected BlockUtils blockUtils;

	protected int defaultChestRadius = 10;
	protected int maxChestRadius = 20;

	public boolean usingMatchingConfig = true;

	protected UpdateChecker updateChecker;
	protected ChestSortHook chestSortHook;
	protected PlotSquaredHook plotSquaredHook;
	protected Visualizer visualizer;
	protected GroupUtils groupUtils;

	private int updateCheckInterval = 86400;

	public void onEnable() {

		String tmpVersion = getServer().getClass().getPackage().getName();
		mcVersion = tmpVersion.substring(tmpVersion.lastIndexOf('.') + 1);
		tmpVersion = mcVersion.substring(mcVersion.indexOf("_") + 1);
		mcMinorVersion = Integer.parseInt(tmpVersion.substring(0, tmpVersion.indexOf("_")));

		reloadCompleteConfig();

		ChestSortPlugin chestSort = (ChestSortPlugin) getServer().getPluginManager().getPlugin("ChestSort");
		if (getConfig().getBoolean("use-chestsort") == false ||chestSort == null || !(chestSort instanceof ChestSortPlugin)) {
			//getLogger().warning("Warning: ChestSort is not installed.");
		} else {
			chestSortAPI = chestSort.getAPI();
			getLogger().info("Succesfully hooked into ChestSort");
		}
		
		chestSortHook = new ChestSortHook(this);
		plotSquaredHook = new PlotSquaredHook(this);
		
		registerCommands();
		initUpdateChecker();
	}

	private void createConfig() {
		
		saveResource("groups.example.yml", true);

		// This saves the config.yml included in the .jar file, but it will not
		// overwrite an existing config.yml
		this.saveDefaultConfig();

		if (getConfig().getInt("config-version", 0) != currentConfigVersion) {
			showOldConfigWarning();
			ConfigUpdater configUpdater = new ConfigUpdater(this);
			configUpdater.updateConfig();
			configUpdater = null;
			usingMatchingConfig = true;
			// createConfig();
		}

		setDefaultConfigValues();

	}

	private void setDefaultConfigValues() {
		// If you use an old config file with missing options, the following default
		// values will be used instead
		// for every missing option.
		getConfig().addDefault("max-chest-radius", 20);
		maxChestRadius = getConfig().getInt("max-chest-radius");
		
		getConfig().addDefault("default-chest-radius", 10);
		defaultChestRadius = getConfig().getInt("default-chest-radius");
		
		getConfig().addDefault("unload-before-dumping", true);
		
		getConfig().addDefault("check-interval", 4);
		updateCheckInterval = (int) (getConfig().getDouble("check-interval")*60*60);
		
		getConfig().addDefault("use-chestsort", true);
		
		getConfig().addDefault("use-playerinteractevent", true);
		
		getConfig().addDefault("use-plotsquared", true);
		getConfig().addDefault("plotsquared-allow-when-trusted", true);
		getConfig().addDefault("plotsquared-allow-outside-plots", true);
		
		getConfig().addDefault("spawn-particles", true); 
		getConfig().addDefault("particle-type", "SPELL_WITCH");
		getConfig().addDefault("particle-count", 100);
		
		getConfig().addDefault("always-show-summary", true);
		
		getConfig().addDefault("laser-animation", true);
		getConfig().addDefault("laser-default-duration", 5);
		getConfig().addDefault("laser-max-distance", 30);
		getConfig().addDefault("laser-max-distance", 50);
		getConfig().addDefault("laser-moves-with-player", false);
		
		if(!EnumUtils.particleExists(getConfig().getString("particle-type"))) {
			getLogger().warning("Specified particle type \"" + getConfig().getString("particle-type") + "\" does not exist! Please check your config.yml");
			getConfig().set("error-particles",true);
		}
		if(!EnumUtils.soundExists(getConfig().getString("sound-effect"))) {
			getLogger().warning("Specified sound effect \"" + getConfig().getString("sound-effect") + "\" does not exist! Please check your config.yml");
			getConfig().set("error-sound", true);
		}
	
	}

	private void registerCommands() {
		CommandUnload commandUnload = new CommandUnload(this);
		getCommand("unload").setExecutor(commandUnload);
		getCommand("dump").setExecutor(commandUnload);
		getCommand("unloadinfo").setExecutor(new CommandUnloadinfo(this));
	}
	
	private void initUpdateChecker() {
		// Check for updates (async, of course)
		// When set to true, we check for updates right now, and every X hours (see
		// updateCheckInterval)
		if (getConfig().getString("check-for-updates", "true").equalsIgnoreCase("true")) {
			Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
				@Override
				public void run() {
					updateChecker.checkForUpdate();
				}
			}, 0L, updateCheckInterval  * 20);

		} // When set to on-startup, we check right now (delay 0)
		else if (getConfig().getString("check-for-updates", "true").equalsIgnoreCase("on-startup")) {
			updateChecker.checkForUpdate();
		}
	}

	public void reloadCompleteConfig() {
		reloadConfig();
		createConfig();
		messages = new Messages(this);
		updateChecker = new UpdateChecker(this);
		blockUtils = new BlockUtils(this);
		visualizer = new Visualizer(this);
		File groupsFile = new File(this.getDataFolder()+File.separator+"groups.yml");
		groupUtils = new GroupUtils(this,groupsFile);
		
	}

}
