//
// This file is a component of Lockette for Bukkit, and was written by Acru Jovian.
// Distributed under the The Non-Profit Open Software License version 3.0 (NPOSL-3.0)
// http://www.opensource.org/licenses/NOSL3.0
//


package org.yi.acru.bukkit.Lockette;

// Imports.
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;

import org.bukkit.event.entity.EntityExplodeEvent;



public class LocketteEntityListener implements Listener{
	private static Lockette		plugin;
	
	
	public LocketteEntityListener(Lockette instance){
		plugin = instance;
	}
	
	
	protected void registerEvents(){
		PluginManager	pm = plugin.getServer().getPluginManager();
		
		pm.registerEvents(this, plugin);
	}
	
	
	//********************************************************************************************************************
	// Start of event section
	
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityExplode(EntityExplodeEvent event){
		if(event.isCancelled()) return;
		
		//List<Block> blockList = event.blockList();
		int			x;//, count = event.blockList().size();
		Block		block;
		
		
		// Check the block list for any protected blocks, and cancel the event if any are found.
		
		for(x = 0; x < event.blockList().size(); ++x){
			block = event.blockList().get(x);
			
			if(Lockette.isProtected(block)){
				//event.setCancelled(true);
				//return;
				event.blockList().remove(x);
				--x;
				//--count;
				continue;
			}
			
			if(Lockette.explosionProtectionAll){
				if((block.getTypeId() == Material.CHEST.getId()) || (block.getTypeId() == Material.DISPENSER.getId()) ||
						(block.getTypeId() == Material.FURNACE.getId()) || (block.getTypeId() == Material.BURNING_FURNACE.getId()) ||
						(block.getTypeId() == Material.BREWING_STAND.getId())){
					//event.setCancelled(true);
					//return;
					event.blockList().remove(x);
					--x;
					//--count;
					continue;
				}
			}
		}
	}
}

