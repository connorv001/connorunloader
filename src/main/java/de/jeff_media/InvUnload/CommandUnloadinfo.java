package de.jeff_media.InvUnload;

import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.sun.tools.javac.code.TypeTag.NumericClasses;

import de.jeff_media.InvUnload.UnloadSummary.PrintRecipient;

public class CommandUnloadinfo implements CommandExecutor {
	
	Main main;
	
	CommandUnloadinfo(Main main) {
		this.main=main;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
			@NotNull String[] args) {
		
		if(!(sender instanceof Player)) {
			if(args.length==0) {
				sender.sendMessage("Error: On Console, you must specify a player as parameter.");
				return true;
			}
			Player p = main.getServer().getPlayer(args[0]);
			if(p==null) {
				sender.sendMessage("Error: Player "+args[0]+" not found.");
				return true;
			}
			if(main.visualizer.unloadSummaries.containsKey(p.getUniqueId())) {
				UnloadSummary summary = main.visualizer.unloadSummaries.get(p.getUniqueId());
				if(summary!=null) {
					summary.print(PrintRecipient.CONSOLE, p);
					return true;
				}
			}
			sender.sendMessage("Player "+p.getName()+" did not unload or dump their inventory.");
			return true;
		}
		
		Player p = (Player) sender;
		
		int duration = main.getConfig().getInt("laser-default-duration");
		if(args.length>0 && StringUtils.isNumeric(args[0])) {
			duration = Integer.parseInt(args[0]);
			if(duration > main.getConfig().getInt("laser-max-duration")) {
				duration = main.getConfig().getInt("laser-max-duration");
			}
		}

		ArrayList<Block> affectedChests = main.visualizer.lastUnloads.get(p.getUniqueId());
		if(affectedChests==null || affectedChests.size()==0) {
			return true;
		}
		ArrayList<Laser> lasers = main.visualizer.activeLasers.get(p.getUniqueId());
		if(lasers != null) {
			for(Laser laser : lasers) {
				if(laser.isStarted()) laser.stop();
			}
		}
		if(main.visualizer.unloadSummaries.containsKey(p.getUniqueId())) {
			UnloadSummary summary = main.visualizer.unloadSummaries.get(p.getUniqueId());
			if(summary!=null) {
				summary.print(PrintRecipient.PLAYER, p);
			}
		}
		main.visualizer.activeLasers.remove(p.getUniqueId());
		for(Block block : affectedChests) {
			main.visualizer.chestAnimation(block, p);
		}
		main.visualizer.playLaser(affectedChests,p,duration);
		boolean newVisualizer=true;

		return true;
	}

}
