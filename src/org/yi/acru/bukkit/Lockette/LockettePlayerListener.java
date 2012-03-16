//
// This file is a component of Lockette for Bukkit, and was written by Acru Jovian.
// Distributed under the The Non-Profit Open Software License version 3.0 (NPOSL-3.0)
// http://www.opensource.org/licenses/NOSL3.0
//


package org.yi.acru.bukkit.Lockette;

// Imports.
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;

import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;



public class LockettePlayerListener implements Listener{
	private static Lockette		plugin;
	
	final static int		materialFenceGate = 107;
	
	
	public LockettePlayerListener(Lockette instance){
		plugin = instance;
	}
	
	
	protected void registerEvents(){
		PluginManager	pm = plugin.getServer().getPluginManager();

		pm.registerEvents(this, plugin);
	}
	
	
	//********************************************************************************************************************
	// Start of event section
	
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event){
		String[]	command = event.getMessage().split(" ", 3);
		
		if(command.length < 1) return;
		if(!(command[0].equalsIgnoreCase("/lockette") || command[0].equalsIgnoreCase("/lock"))) return;
		event.setCancelled(true);
		
		Player		player = event.getPlayer();
		
		
		// Reload config files, for admins only.
		if(command.length == 2){
			if(command[1].equalsIgnoreCase("reload")){
				if(!plugin.hasPermission(player.getWorld(), player, "lockette.admin.reload")) return;
				
				plugin.loadProperties(true);
				
				plugin.localizedMessage(player, Lockette.broadcastReloadTarget, "msg-admin-reload");
				return;
			}
			
			if(command[1].equalsIgnoreCase("version")){
				player.sendMessage(ChatColor.RED + "Lockette version " + plugin.getDescription().getVersion() + " loaded.  (Core: " + Lockette.getCoreVersion() + ")");
				return;
			}
			
			if(command[1].equalsIgnoreCase("fix")){
				if(fixDoor(player)){
					plugin.localizedMessage(player, null, "msg-error-fix");
				}
				return;
			}
		}
		
		
		// Edit sign text.
		if((command.length == 2) || (command.length == 3)){
			if(command[1].equals("1") || command[1].equals("2") || command[1].equals("3") || command[1].equals("4")){
				Block		block = plugin.playerList.get(player.getName());
				//boolean		error = false;
				
				// Check if the selected block is a valid sign.
				
				if(block == null){plugin.localizedMessage(player, null, "msg-error-edit"); return;}
				else if(block.getTypeId() != Material.WALL_SIGN.getId()){plugin.localizedMessage(player, null, "msg-error-edit"); return;}
				

				Sign		sign = (Sign) block.getState();
				Sign		owner = sign;
				String		text = sign.getLine(0).replaceAll("(?i)\u00A7[0-F]", "").toLowerCase();
				boolean		privateSign;
				
				// Check if it is our sign that is selected.
				
				if(text.equals("[private]") || text.equalsIgnoreCase(Lockette.altPrivate)) privateSign = true;
				else if(text.equals("[more users]") || text.equalsIgnoreCase(Lockette.altMoreUsers)){
					privateSign = false;
					
					Block		checkBlock = Lockette.getSignAttachedBlock(block);
					if(checkBlock == null){plugin.localizedMessage(player, null, "msg-error-edit"); return;}
					
					Block		signBlock = Lockette.findBlockOwner(checkBlock);
					if(signBlock == null){plugin.localizedMessage(player, null, "msg-error-edit"); return;}
					
					owner = (Sign) signBlock.getState();
				}
				else{plugin.localizedMessage(player, null, "msg-error-edit"); return;}
				
				
				int			length = player.getName().length();
				
				if(length > 15) length = 15;

				// Check owner.
				if(owner.getLine(1).replaceAll("(?i)\u00A7[0-F]", "").equals(player.getName().substring(0, length)) || player.getName().equals("Acru")){	// Temp for Debugging.
				//if(sign.getLine(1).replaceAll("(?i)\u00A7[0-F]", "").equals(player.getName().substring(0, length))){
					int			line = Integer.parseInt(command[1]) - 1;
					
					// Disallow editing [Private] line 1/2 here.
					if(!player.getName().equals("Acru")){	// For debugging.
						if(line <= 0) return;
						else if(line <= 1) if(privateSign) return;
					}
					
					
					if(command.length == 3){
						length = command[2].length();
						
						if(length > 15) length = 15;
						if(Lockette.colorTags) sign.setLine(line, command[2].substring(0, length).replaceAll("&([0-9A-Fa-f])", "\u00A7$1"));
						else sign.setLine(line, command[2].substring(0, length));
					}
					else sign.setLine(line, "");
					sign.update();
					
					plugin.localizedMessage(player, null, "msg-owner-edit");
					return;
				}
				else{plugin.localizedMessage(player, null, "msg-error-edit"); return;}
			}
		}
		
		
		// If none of the above, print out the help text.
		// Commands:
		// reload
		// 2-4 <text> - sign editing
		// link - linking?
		// set <value> <string> - config?

		plugin.localizedMessage(player, null, "msg-help-command1");
		plugin.localizedMessage(player, null, "msg-help-command2");
		plugin.localizedMessage(player, null, "msg-help-command3");
		plugin.localizedMessage(player, null, "msg-help-command4");
		plugin.localizedMessage(player, null, "msg-help-command5");
		plugin.localizedMessage(player, null, "msg-help-command6");
		plugin.localizedMessage(player, null, "msg-help-command7");
		plugin.localizedMessage(player, null, "msg-help-command8");
		plugin.localizedMessage(player, null, "msg-help-command9");
	}
	
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerInteract(PlayerInteractEvent event){
		if(!event.hasBlock()) return;
		
		Action		action = event.getAction();
		Player		player = event.getPlayer();
		Block		block = event.getClickedBlock();
		int			type = block.getTypeId();
		BlockFace	face = event.getBlockFace();
		
		
		if(action == Action.RIGHT_CLICK_BLOCK){
			if(Lockette.protectTrapDoors) if(type == Material.TRAP_DOOR.getId()){
				if(interactDoor(block, player)) return;
				
				event.setUseInteractedBlock(Result.DENY);
				event.setUseItemInHand(Result.DENY);
				return;
			}
			
			if(Lockette.protectDoors) if((type == Material.WOODEN_DOOR.getId()) || (type == Material.IRON_DOOR_BLOCK.getId()) || (type == materialFenceGate)){
				if(interactDoor(block, player)) return;
				
				event.setUseInteractedBlock(Result.DENY);
				event.setUseItemInHand(Result.DENY);
				return;
			}
			
			if(type == Material.WALL_SIGN.getId()){
				interactSign(block, player);
				return;
			}
			
			if(type == Material.CHEST.getId()){
				// Try at making a 1.7->1.8 chest fixer.
				Lockette.rotateChestOrientation(block, face);
			}
			
			if((type == Material.CHEST.getId()) || (type == Material.DISPENSER.getId()) ||
					(type == Material.FURNACE.getId()) || (type == Material.BURNING_FURNACE.getId()) ||
					(type == Material.BREWING_STAND.getId()) || Lockette.isInList(type, Lockette.customBlockList)){
				
				// Trying something out....
				if(Lockette.directPlacement) if(event.hasItem()) if((face != BlockFace.UP) && (face != BlockFace.DOWN)){
					ItemStack		item = event.getItem();
					
					if(item.getTypeId() == Material.SIGN.getId()){
						Block		checkBlock = block.getRelative(face);
						
						type = checkBlock.getTypeId();
						
						if(type == Material.AIR.getId()){
							boolean		place = false;
							
							if(Lockette.isProtected(block)){
								// Add a users sign only if owner.
								if(Lockette.isOwner(block, player.getName())) place = true; 
							}
							else place = true;
							//if(Lockette.altPrivate == null){}//if(Lockette.altMoreUsers == null){}
							if(place){
								//player.sendMessage(ChatColor.RED + "Lockette: Using a sign on a container");
								
								event.setUseItemInHand(Result.ALLOW); //? seems to work in 568
								event.setUseInteractedBlock(Result.DENY);
								return;
							}
						}
					}
				}
				
				
				if(interactContainer(block, player)) return;
				
				event.setUseInteractedBlock(Result.DENY);
				event.setUseItemInHand(Result.DENY);
				return;
			}
			
			if(type == Material.DIRT.getId()) if(event.hasItem()){
				ItemStack		item = event.getItem();
				
				type = item.getTypeId();
				
				if((type == Material.DIAMOND_HOE.getId()) || (type == Material.GOLD_HOE.getId()) || (type == Material.IRON_HOE.getId()) || 
						(type == Material.STONE_HOE.getId()) || (type == Material.WOOD_HOE.getId())){
					Block		checkBlock = block.getRelative(BlockFace.UP);
					
					type = checkBlock.getTypeId();
					
					if((type == Material.WOODEN_DOOR.getId()) || (type == Material.IRON_DOOR_BLOCK.getId()) || (type == materialFenceGate)){
						event.setUseInteractedBlock(Result.DENY);
						return;
					}
				}
			}
		}
		else if(action == Action.LEFT_CLICK_BLOCK){
			if(Lockette.protectTrapDoors) if(type == Material.TRAP_DOOR.getId()){
				if(interactDoor(block, player)) return;
				
				event.setUseInteractedBlock(Result.DENY);
				event.setUseItemInHand(Result.DENY);
				return;
			}
			
			if(Lockette.protectDoors) if((type == Material.WOODEN_DOOR.getId()) || (type == Material.IRON_DOOR_BLOCK.getId()) || (type == materialFenceGate)){
				if(interactDoor(block, player)) return;
				
				event.setUseInteractedBlock(Result.DENY);
				event.setUseItemInHand(Result.DENY);
				return;
			}
		}
	}
	
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerQuit(PlayerQuitEvent event){
		Player		player = event.getPlayer();
		
		// Player left, so forget about them.
		plugin.playerList.remove(player.getName());
	}
	
	
	//********************************************************************************************************************
	// Start of interact section
	
	
	// Returns true if it should be allowed, false if it should be canceled.
	private static boolean interactDoor(Block block, Player player){
		Block		signBlock = Lockette.findBlockOwner(block);
		
		if(signBlock == null) return(true);
		
		boolean		wooden = (block.getTypeId() == Material.WOODEN_DOOR.getId());
		boolean		trap = false;
		
		if(block.getTypeId() == materialFenceGate) wooden = true;
		if(Lockette.protectTrapDoors) if(block.getTypeId() == Material.TRAP_DOOR.getId()){
			wooden = true;
			trap = true;
		}
		
		
		// Someone touched an owned door, lets see if they are allowed.
		
		boolean		allow = false;
		
		if(canInteract(block, signBlock, player, true)) allow = true;
		/*
		// Fee stuff...
		if(!allow){
			// Check if we can pay a fee to activate.
			int fee = getSignOption(signBlock, "fee", Lockette.altFee, 0);
			
			if(signBlock.equals(plugin.playerList.get(player.getName()))){
				if(fee == 0){
					player.sendMessage("unable to pay fee");
				}
				else{
					player.sendMessage("Fee of " +plugin.economyFormat(fee)+" paid (fake)");
					plugin.playerList.put(player.getName(), block);
					allow = true;
				}
				/
				if(fee != 0){
					Sign		sign = (Sign) signBlock.getState();
					String		text = sign.getLine(1).replaceAll("(?i)\u00A7[0-F]", "");
					
					if(plugin.economyTransfer(player.getName(), text, fee)){
						allow = true;
						plugin.playerList.put(player.getName(), block);
					}
					else{}
				}/
			}
			else if(fee != 0){
				player.sendMessage("first touch sign to pay fee.");
			}
		}
		*/
		if(allow){
			List<Block> list = Lockette.toggleDoors(block, Lockette.getSignAttachedBlock(signBlock), wooden, trap);
			
			int delta = Lockette.getSignOption(signBlock, "timer", Lockette.altTimer, Lockette.defaultDoorTimer);
			
			plugin.doorCloser.add(list, delta != 0, delta);
			return(true);
		}
		
		
		// Don't have permission.
		
		//event.setCancelled(true);
		
		//if(Lockette.oldListener){
		//	if(wooden) Lockette.toggleSingleDoor(block);
		//}
		
		// Report only once, unless a different block is clicked.
		if(block.equals(plugin.playerList.get(player.getName()))) return(false);
		plugin.playerList.put(player.getName(), block);
		plugin.localizedMessage(player, null, "msg-user-denied-door");
		return(false);
	}
	
	
	private static void interactSign(Block block, Player player){
		Sign		sign = (Sign) block.getState();
		String		text = sign.getLine(0).replaceAll("(?i)\u00A7[0-F]", "").toLowerCase();
		Block		signBlock = block;
		
		
		// Check if it is our sign that was clicked.
		
		if(text.equals("[private]") || text.equalsIgnoreCase(Lockette.altPrivate)){}
		else if(text.equals("[more users]") || text.equalsIgnoreCase(Lockette.altMoreUsers)){
			Block		checkBlock = Lockette.getSignAttachedBlock(block);
			if(checkBlock == null) return;
			
			signBlock = Lockette.findBlockOwner(checkBlock);
			if(signBlock == null) return;
			
			sign = (Sign) signBlock.getState();
		}
		else return;
		
		int			length = player.getName().length();
		
		if(length > 15) length = 15;
		
		// Check owner.
		if(sign.getLine(1).replaceAll("(?i)\u00A7[0-F]", "").equals(player.getName().substring(0, length)) || player.getName().equals("Acru")){	// Temp for Debugging.
		//if(sign.getLine(1).replaceAll("(?i)\u00A7[0-F]", "").equals(player.getName().substring(0, length))){
			if(!block.equals(plugin.playerList.get(player.getName()))){
				// Associate the user with the owned sign.
				plugin.playerList.put(player.getName(), block);
				plugin.localizedMessage(player, null, "msg-help-select");
			}
		}
		else{/*
			int fee = getSignOption(signBlock, "fee", Lockette.altFee, 0);
			
			if(fee != 0){
				if(!signBlock.equals(plugin.playerList.get(player.getName()))){
					// First half of fee approval.
					plugin.playerList.put(player.getName(), signBlock);
					plugin.localizedMessage(player, null, "msg-user-touch-fee", sign.getLine(1), plugin.economyFormat(fee));
				}
			}
			else{*/
				if(!block.equals(plugin.playerList.get(player.getName()))){
					// Only print this message once as well.
					plugin.playerList.put(player.getName(), block);
					plugin.localizedMessage(player, null, "msg-user-touch-owned", sign.getLine(1));
				}
			//}
		}
	}
	
	
	// Returns true if it should be allowed, false if it should be canceled.
	private static boolean interactContainer(Block block, Player player){
		Block		signBlock = Lockette.findBlockOwner(block);
		
		if(signBlock == null) return(true);
		
		
		// Someone touched an owned container, lets see if they are allowed.
		
		if(canInteract(block, signBlock, player, false)) return(true);
		
		
		// Don't have permission.
		
		// Report only once, unless a different block is clicked.
		if(block.equals(plugin.playerList.get(player.getName()))) return(false);
		plugin.playerList.put(player.getName(), block);
		plugin.localizedMessage(player, null, "msg-user-denied");
		return(false);
	}
	
	
	// Block is the container or door, signBlock is the owning [Private] sign.
	// Returns true if it should be allowed, false if it should be canceled.
	private static boolean canInteract(Block block, Block signBlock, Player player, boolean isDoor){
		// Check if the block is owned first.
		
		// Moved to outer..
		
		
		// Lets see if the player is allowed to touch...
		
		Sign		sign = (Sign) signBlock.getState();
		int			length = player.getName().length();
		String		line;
		
		if(length > 15) length = 15;
		
		
		// Check owner.
		
		line = sign.getLine(1).replaceAll("(?i)\u00A7[0-F]", "");
		
		if(line.equals(player.getName().substring(0, length))) return(true);
		if(plugin.inGroup(block.getWorld(), player, line)) return(true);
		
		
		// Check main two users.
		
		int			y;
		
		for(y = 2; y <= 3; ++y) if(!sign.getLine(y).isEmpty()){
			line = sign.getLine(y).replaceAll("(?i)\u00A7[0-F]", "");
			
			if(plugin.inGroup(block.getWorld(), player, line)) return(true);
			if(line.equalsIgnoreCase(player.getName().substring(0, length))) return(true);
		}
		
		
		// Check for more users.
		
		List<Block>	list = Lockette.findBlockUsers(block, signBlock);
		int			x, count = list.size();
		Sign		sign2;
		
		for(x = 0; x < count; ++x){
			sign2 = (Sign) list.get(x).getState();
			
			for(y = 1; y <= 3; ++y) if(!sign2.getLine(y).isEmpty()){
				line = sign2.getLine(y).replaceAll("(?i)\u00A7[0-F]", "");
				
				if(plugin.inGroup(block.getWorld(), player, line)) return(true);
				if(line.equalsIgnoreCase(player.getName().substring(0, length))) return(true);
			}
		}
		
		
		// Check admin list last.
		
		boolean		snoop = false;
		
		if(isDoor){
			if(Lockette.adminBypass){
				if(plugin.hasPermission(block.getWorld(), player, "lockette.admin.bypass")) snoop = true;
				
				if(snoop){
					Lockette.log.info("[" + plugin.getDescription().getName() + "] (Admin) " + player.getName() + " has bypassed a door owned by " + sign.getLine(1));
					
					plugin.localizedMessage(player, null, "msg-admin-bypass", sign.getLine(1));
					return(true);
				}
			}
		}
		else if(Lockette.adminSnoop){
			if(plugin.hasPermission(block.getWorld(), player, "lockette.admin.snoop")) snoop = true;
			
			if(snoop){
				Lockette.log.info("[" + plugin.getDescription().getName() + "] (Admin) " + player.getName() + " has snooped around in a container owned by " + sign.getLine(1) + "!");
				
				plugin.localizedMessage(player, Lockette.broadcastSnoopTarget, "msg-admin-snoop", sign.getLine(1));
				return(true);
			}
		}
		
		
		// Don't have permission.
		
		return(false);
	}
	
	
	//********************************************************************************************************************
	// Start of utility section
	
	
	// Returns true if a door wasn't changed.
	private static boolean fixDoor(Player player){
		Block		block = player.getTargetBlock(null, 10);
		int			type = block.getTypeId();
		boolean		doCheck = false;
		
		
		// Check if the block being looked at is a door block.
		
		if(Lockette.protectTrapDoors){
			if(type == Material.TRAP_DOOR.getId()) doCheck = true;
		}
		
		if(Lockette.protectDoors){
			if((type == Material.WOODEN_DOOR.getId()) || (type == Material.IRON_DOOR_BLOCK.getId()) || (type == materialFenceGate)) doCheck = true;
		}
		
		if(!doCheck) return(true);
		
		
		Block		signBlock = Lockette.findBlockOwner(block);
		
		if(signBlock == null) return(true);
		
		Sign		sign = (Sign) signBlock.getState();
		int			length = player.getName().length();
		
		if(length > 15) length = 15;
		
		// Check owner only.
		if(sign.getLine(1).replaceAll("(?i)\u00A7[0-F]", "").equals(player.getName().substring(0, length))){
			Lockette.toggleSingleDoor(block);
			return(false);
		}
		
		return(true);
	}
}

