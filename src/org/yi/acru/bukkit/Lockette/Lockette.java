//
// This file is a component of Lockette for Bukkit, and was written by Acru Jovian.
// Distributed under the The Non-Profit Open Software License version 3.0 (NPOSL-3.0)
// http://www.opensource.org/licenses/NOSL3.0
//


package org.yi.acru.bukkit.Lockette;


// Imports.
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import org.yi.acru.bukkit.PluginCore;



public class Lockette extends PluginCore{
	private static Lockette					plugin;
	private static boolean					enabled = false;
	
	private static boolean					registered = false;
	private final LocketteBlockListener		blockListener = new LocketteBlockListener(this);
	private final LocketteEntityListener	entityListener = new LocketteEntityListener(this);
	private final LockettePlayerListener	playerListener = new LockettePlayerListener(this);
	private final LockettePrefixListener	prefixListener = new LockettePrefixListener(this);
	private final LocketteWorldListener		worldListener = new LocketteWorldListener(this);
	protected final LocketteDoorCloser		doorCloser = new LocketteDoorCloser(this);
	
	protected static boolean				explosionProtectionAll, rotateChests;
	protected static boolean				adminSnoop, adminBypass, adminBreak;
	protected static boolean				protectDoors, protectTrapDoors, usePermissions;
	protected static boolean				directPlacement, colorTags;
	protected static int					defaultDoorTimer;
	protected static String					broadcastSnoopTarget, broadcastBreakTarget, broadcastReloadTarget;
	
	protected static boolean				msgUser, msgOwner, msgAdmin, msgError, msgHelp;
	protected static String					altPrivate, altMoreUsers, altEveryone, altOperators, altTimer, altFee;
	protected static List<Object>			customBlockList = null;
	
	protected static FileConfiguration			strings = null;
	protected final HashMap<String, Block>	playerList = new HashMap<String, Block>();

	final static int		materialTrapDoor = 96;
	final static int		materialFenceGate = 107;
	
	
	public Lockette(){
		plugin = this;
	}
	
	
	public void onLoad(){}
	
	
	public void onEnable(){
		if(enabled) return;
		
		log.info("[" + getDescription().getName() + "] Version " + this.getDescription().getVersion() + " is being enabled!  Yay!  (Core version " + getCoreVersion() + ")");
		
		
		// Check build version.
		
		final int	recBuild = 1846;
		final int	minBuild = 1846;
		int			printBuild;
		float		build = getBuildVersion();
		
		if((build > 399) && (build < 400)) printBuild = (int) ((build - 399) * 100);
		else printBuild = (int) build;
		
		//if((printBuild >= 45) && (printBuild <= 49)) log.info("[" + getDescription().getName() + "] Ignore the warning about using the stupidly long constructor!");
		
		if(build == 0){
			log.warning("[" + getDescription().getName() + "] Craftbukkit build unrecognized, please be sure you have build [" + recBuild + "] or greater.");
		}
		else if(build < minBuild){
			log.severe("[" + getDescription().getName() + "] Detected craftbukkit build [" + printBuild + "], but requires requires build [" + minBuild + "] or greater!");
			log.severe("[" + getDescription().getName() + "] Aborting enable!");
			return;
		}
		else if(build < recBuild){
			log.warning("[" + getDescription().getName() + "] Detected craftbukkit build [" + printBuild + "], but the recommended build is [" + recBuild + "] or greater.");
		}
		else if((build >= 605) && (build <= 612)){
			log.warning("[" + getDescription().getName() + "] Detected craftbukkit build [" + printBuild + "], but this build is buggy!  Please upgrade to build 617 or greater.");
		}
		else if((build >= 685) && (build <= 703)){
			log.warning("[" + getDescription().getName() + "] Detected craftbukkit build [" + printBuild + "], but this build is buggy!  Please upgrade to build 704 or greater.");
		}
		else{
			log.info("[" + getDescription().getName() + "] Detected craftbukkit build [" + printBuild + "] ok.");
		}
		
		
		
		// Load properties and strings.
		
		loadProperties(false);
		
		
		// Load external permission/group plugins.
		
		super.onEnable();
		
		
		// Reg us some events yo!
		
		if(!registered){
			blockListener.registerEvents();
			entityListener.registerEvents();
			playerListener.registerEvents();
			prefixListener.registerEvents();
			worldListener.registerEvents();
			registered = true;
		}
		
		
		// All done.
		
		log.info("[" + getDescription().getName() + "] Ready to protect your containers.");
		enabled = true;
	}
	
	
	public void onDisable(){
		if(!enabled) return;
		log.info(this.getDescription().getName() + " is being disabled...  ;.;");
		
		if(protectDoors || protectTrapDoors){
			log.info("[" + getDescription().getName() + "] Closing all automatic doors.");
			doorCloser.cleanup();
		}
		
		super.onDisable();
		
		enabled = false;
	}
	
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		if(!cmd.getName().equalsIgnoreCase("lockette")) return(false);
		if(sender instanceof Player) return(true);	// Handling in command preprocess for now.
		
		if(args.length == 1){
			if(args[0].equalsIgnoreCase("reload")){
				loadProperties(true);
				
				localizedMessage(null, Lockette.broadcastReloadTarget, "msg-admin-reload");
				
				//String msgString = Lockette.strings.getString("msg-admin-reload");
				//selectiveBroadcast(Lockette.broadcastReloadTarget, ChatColor.RED + "Lockette: " + msgString);
			}
			else if(args[0].equalsIgnoreCase("coredump")){
				dumpCoreInfo();
			}
		}
		//sender.sendMessage("Lockette: Test");
		
		return(true);
	}
	
	
	@SuppressWarnings("unchecked")
	protected void loadProperties(boolean reload){
		if(reload){
			log.info("[" + getDescription().getName() + "] Reloading plugin configuration files.");
			this.reloadConfig();
		}
		
		
		FileConfiguration	properties = this.getConfig();
		boolean				propChanged = true;
		//boolean			tempBoolean;
		
		
		msgUser = properties.getBoolean("enable-messages-user", true);
		properties.set("enable-messages-user", msgUser);
		msgOwner = properties.getBoolean("enable-messages-owner", false);
		properties.set("enable-messages-owner", msgOwner);
		//msgAdmin = true;
		msgAdmin = properties.getBoolean("enable-messages-admin", true);
		properties.set("enable-messages-admin", msgAdmin);
		msgError = properties.getBoolean("enable-messages-error", true);
		properties.set("enable-messages-error", msgError);
		msgHelp = properties.getBoolean("enable-messages-help", true);
		properties.set("enable-messages-help", msgHelp);
		
		explosionProtectionAll = properties.getBoolean("explosion-protection-all", false);
		properties.set("explosion-protection-all", explosionProtectionAll);
		rotateChests = properties.getBoolean("enable-chest-rotation", false);
		properties.set("enable-chest-rotation", rotateChests);
		
		usePermissions = properties.getBoolean("enable-permissions", false);
		properties.set("enable-permissions", usePermissions);
		protectDoors = properties.getBoolean("enable-protection-doors", true);
		properties.set("enable-protection-doors", protectDoors);
		protectTrapDoors = properties.getBoolean("enable-protection-trapdoors", true);
		properties.set("enable-protection-trapdoors", protectTrapDoors);
		
		adminSnoop = properties.getBoolean("allow-admin-snoop", false);
		properties.set("allow-admin-snoop", adminSnoop);
		adminBypass = properties.getBoolean("allow-admin-bypass", true);
		properties.set("allow-admin-bypass", adminBypass);
		adminBreak = properties.getBoolean("allow-admin-break", true);
		properties.set("allow-admin-break", adminBreak);
		
		
		// Start a scheduled task, for closing doors.
		if(protectDoors || protectTrapDoors){
			if(doorCloser.start()){
				log.severe("[" + getDescription().getName() + "] Failed to register door closing task!");
			}
		}
		else doorCloser.stop();
		
		

		directPlacement = properties.getBoolean("enable-quick-protect", true);
		properties.set("enable-quick-protect", directPlacement);
		colorTags = properties.getBoolean("enable-color-tags", true);
		properties.set("enable-color-tags", colorTags);
		//directPlacement = true;
		
		
		// = properties.getBoolean("", true);
		//properties.set("", );
		
		//tempBoolean = properties.getBoolean("use-whitelist", false);
		//tempBoolean = properties.getBoolean("lock-all-chests", true);//rename
		//tempBoolean = properties.getBoolean("test-bool", true);
		//properties.set("test-bool", tempBoolean);
		
		
		
		defaultDoorTimer = properties.getInt("default-door-timer", -1);
		if(defaultDoorTimer == -1){
			defaultDoorTimer = 0;
			properties.set("default-door-timer", defaultDoorTimer);
			propChanged = true;
		}
		
		
		
		// Customizable protected block list.
		
		customBlockList = (List<Object>) properties.getList("custom-lockable-block-list");
		if(customBlockList == null){
			customBlockList = new ArrayList<Object>(3);
			customBlockList.add(Material.ENCHANTMENT_TABLE.getId());
			customBlockList.add(Material.JUKEBOX.getId());
			customBlockList.add(Material.DIAMOND_BLOCK.getId());
			properties.set("custom-lockable-block-list", customBlockList);
			propChanged = true;
		}
		log.info("[" + getDescription().getName() + "] Custom lockable block list: " + customBlockList.toString());
		
		
		
		broadcastSnoopTarget = properties.getString("broadcast-snoop-target");
		if(broadcastSnoopTarget == null){
			broadcastSnoopTarget = "[Everyone]";
			properties.set("broadcast-snoop-target", broadcastSnoopTarget);
			propChanged = true;
		}
		broadcastBreakTarget = properties.getString("broadcast-break-target");
		if(broadcastBreakTarget == null){
			broadcastBreakTarget = "[Everyone]";
			properties.set("broadcast-break-target", broadcastBreakTarget);
			propChanged = true;
		}
		broadcastReloadTarget = properties.getString("broadcast-reload-target");
		if(broadcastReloadTarget == null){
			broadcastReloadTarget = "[Operators]";
			properties.set("broadcast-reload-target", broadcastReloadTarget);
			propChanged = true;
		}
		
		
		String stringsFileName = properties.getString("strings-file-name");
		if((stringsFileName == null) || stringsFileName.isEmpty()){
			stringsFileName = "strings-en.yml";
			properties.set("strings-file-name", stringsFileName);
			propChanged = true;
		}
		
		if(propChanged){
			this.saveConfig();
		}
		loadStrings(reload, stringsFileName);
	}
	
	
	protected void loadStrings(boolean reload, String fileName){
		boolean			stringChanged = false;
		String			tempString;
		File			stringsFile = new File(getDataFolder(), fileName);
		
		
		// Close the strings file if already loaded.
		if(strings != null){
			// Should automatically garbage collect.
			strings = null;
		}
		
		// Load the strings file if not loaded yet.
		if(strings == null){
			strings = new YamlConfiguration();
			try{
				strings.load(stringsFile);
			}
			catch(Exception ex){}
		}
		else if(reload){
			try{
				strings.load(stringsFile);
			}
			catch(Exception ex){}
		}
		
		// To remove french tags from the default strings file, and to not save to alt strings files.
		boolean		original = false;
		if(fileName.equals("strings-en.yml")){
			original = true;
			
			strings.set("language", "English");
			
			// Force to be first.
			if(original){
				try{
					strings.save(stringsFile);
					strings.load(stringsFile);
				}
				catch(Exception ex){}
			}
			
			strings.set("author", "Acru");
			strings.set("editors", "");
			strings.set("version", 0);
		}
		
		
		// Load in the alternate sign strings.
		
		altPrivate = strings.getString("alternate-private-tag");
		if((altPrivate == null) || altPrivate.isEmpty() || (original && altPrivate.equals("Privé"))){
			altPrivate = "Private";
			strings.set("alternate-private-tag", altPrivate);
		}
		altPrivate = "["+altPrivate+"]";
		
		altMoreUsers = strings.getString("alternate-moreusers-tag");
		if((altMoreUsers == null) || altMoreUsers.isEmpty() || (original && altMoreUsers.equals("Autre Noms"))){
			altMoreUsers = "More Users";
			strings.set("alternate-moreusers-tag", altMoreUsers);
			stringChanged = true;
		}
		altMoreUsers = "["+altMoreUsers+"]";
		
		altEveryone = strings.getString("alternate-everyone-tag");
		if((altEveryone == null) || altEveryone.isEmpty() || (original && altEveryone.equals("Tout le Monde"))){
			altEveryone = "Everyone";
			strings.set("alternate-everyone-tag", altEveryone);
			stringChanged = true;
		}
		altEveryone = "["+altEveryone+"]";
		
		altOperators = strings.getString("alternate-operators-tag");
		if((altOperators == null) || altOperators.isEmpty() || (original && altOperators.equals("Opérateurs"))){
			altOperators = "Operators";
			strings.set("alternate-operators-tag", altOperators);
			stringChanged = true;
		}
		altOperators = "["+altOperators+"]";
		
		altTimer = strings.getString("alternate-timer-tag");
		if((altTimer == null) || altTimer.isEmpty() || (original && altTimer.equals("Minuterie"))){
			altTimer = "Timer";
			strings.set("alternate-timer-tag", altTimer);
			stringChanged = true;
		}
		
		altFee = strings.getString("alternate-fee-tag");
		if((altFee == null) || altFee.isEmpty()){
			altFee = "Fee";
			strings.set("alternate-fee-tag", altFee);
			stringChanged = true;
		}
		
		
		
		// Check all the message strings.
		
		// Messages for onBlockPlace.
		tempString = strings.getString("msg-user-conflict-door");
		if(tempString == null){
			strings.set("msg-user-conflict-door", "Conflicting door removed!");
			stringChanged = true;
		}
		tempString = strings.getString("msg-user-illegal");
		if(tempString == null){
			strings.set("msg-user-illegal", "Illegal chest removed!");
			stringChanged = true;
		}
		tempString = strings.getString("msg-user-resize-owned");
		if(tempString == null){
			strings.set("msg-user-resize-owned", "You cannot resize a chest claimed by ***.");
			stringChanged = true;
		}
		tempString = strings.getString("msg-help-chest");
		if(tempString == null){
			strings.set("msg-help-chest", "Place a sign headed [Private] next to a chest to lock it.");
			stringChanged = true;
		}
		
		
		// Messages for onBlockBreak.
		tempString = strings.getString("msg-owner-release");
		if(tempString == null){
			strings.set("msg-owner-release", "You have released a container!");
			stringChanged = true;
		}
		tempString = strings.getString("msg-admin-release");
		if(tempString == null){
			strings.set("msg-admin-release", "(Admin) @@@ has broken open a container owned by ***!");
			stringChanged = true;
		}
		tempString = strings.getString("msg-user-release-owned");
		if(tempString == null){
			strings.set("msg-user-release-owned", "You cannot release a container claimed by ***.");
			stringChanged = true;
		}
		tempString = strings.getString("msg-owner-remove");
		if(tempString == null){
			strings.set("msg-owner-remove", "You have removed users from a container!");
			stringChanged = true;
		}
		tempString = strings.getString("msg-user-remove-owned");
		if(tempString == null){
			strings.set("msg-user-remove-owned", "You cannot remove users from a container claimed by ***.");
			stringChanged = true;
		}
		tempString = strings.getString("msg-user-break-owned");
		if(tempString == null){
			strings.set("msg-user-break-owned", "You cannot break a container claimed by ***.");
			stringChanged = true;
		}
		
		
		// Messages for onBlockDamage.
		tempString = strings.getString("msg-user-denied-door");
		if(tempString == null){
			strings.set("msg-user-denied-door", "You don't have permission to use this door.");
			stringChanged = true;
		}
		
		
		// Messages for onBlockRightClick.
		tempString = strings.getString("msg-user-touch-fee");
		if(tempString == null){
			strings.set("msg-user-touch-fee", "A fee of ### will be paid to ***, to open.");
			stringChanged = true;
		}
		tempString = strings.getString("msg-user-touch-owned");
		if(tempString == null){
			strings.set("msg-user-touch-owned", "This container has been claimed by ***.");
			stringChanged = true;
		}
		tempString = strings.getString("msg-help-select");
		if(tempString == null){
			strings.set("msg-help-select", "Sign selected, use /lockette <line number> <text> to edit.");
			stringChanged = true;
		}
		
		
		// Messages for onBlockInteract.
		tempString = strings.getString("msg-admin-bypass");
		if(tempString == null){
			strings.set("msg-admin-bypass", "Bypassed a door owned by ***, be sure to close it behind you.");
			stringChanged = true;
		}
		tempString = strings.getString("msg-admin-snoop");
		if(tempString == null){
			strings.set("msg-admin-snoop", "(Admin) @@@ has snooped around in a container owned by ***!");
			stringChanged = true;
		}
		tempString = strings.getString("msg-user-denied");
		if(tempString == null){
			strings.set("msg-user-denied", "You don't have permission to open this container.");
			stringChanged = true;
		}
		
		
		// Messages for onSignChange.
		tempString = strings.getString("msg-error-zone");
		if(tempString == null){
			strings.set("msg-error-zone", "This zone is protected by ***.");
			stringChanged = true;
		}
		tempString = strings.getString("msg-error-permission");
		if(tempString == null){
			strings.set("msg-error-permission", "Permission to lock container denied.");
			stringChanged = true;
		}
		else if(tempString.equals("Permission to lock containers denied.")){
			strings.set("msg-error-permission", "Permission to lock container denied.");
			stringChanged = true;
		}
		tempString = strings.getString("msg-error-claim");
		if(tempString == null){
			strings.set("msg-error-claim", "No unclaimed container nearby to make Private!");
			stringChanged = true;
		}
		tempString = strings.getString("msg-error-claim-conflict");
		if(tempString == null){
			strings.set("msg-error-claim-conflict", "Conflict with an existing protected door.");
			stringChanged = true;
		}
		tempString = strings.getString("msg-admin-claim-error");
		if(tempString == null){
			strings.set("msg-admin-claim-error", "Player *** is not online, be sure you have the correct name.");
			stringChanged = true;
		}
		tempString = strings.getString("msg-admin-claim");
		if(tempString == null){
			strings.set("msg-admin-claim", "You have claimed a container for ***.");
			stringChanged = true;
		}
		tempString = strings.getString("msg-owner-claim");
		if(tempString == null){
			strings.set("msg-owner-claim", "You have claimed a container!");
			stringChanged = true;
		}
		tempString = strings.getString("msg-error-adduser-owned");
		if(tempString == null){
			strings.set("msg-error-adduser-owned", "You cannot add users to a container claimed by ***.");
			stringChanged = true;
		}
		tempString = strings.getString("msg-error-adduser");
		if(tempString == null){
			strings.set("msg-error-adduser", "No claimed container nearby to add users to!");
			stringChanged = true;
		}
		tempString = strings.getString("msg-owner-adduser");
		if(tempString == null){
			strings.set("msg-owner-adduser", "You have added users to a container!");
			stringChanged = true;
		}
		
		
		// Messages for onPlayerCommand.
		if(original){
			strings.set("msg-help-command1", "&C/lockette <line number> <text> - Edits signs on locked containers. Right click on the sign to edit.");
			strings.set("msg-help-command2", "&C/lockette fix - Fixes an automatic door that is in the wrong position. Look at the door to edit.");
			strings.set("msg-help-command3", "&C/lockette reload - Reloads the configuration files.  Operators only.");
			strings.set("msg-help-command4", "&C/lockette version - Reports Lockette version string.");
			stringChanged = true;
		}
		
		/*
		tempString = strings.getString("msg-help-command1");
		if(tempString == null){
			strings.set("msg-help-command1", "/lockette reload - Reloads the configuration files.");
			stringChanged = true;
		}
		tempString = strings.getString("msg-help-command2");
		if(tempString == null){
			strings.set("msg-help-command2", "/lockette <line number> <text> - Edits signs on locked containers. Right click on the sign to edit.");
			stringChanged = true;
		}
		*/
		tempString = strings.getString("msg-admin-reload");
		if(tempString == null){
			strings.set("msg-admin-reload", "Reloading plugin configuration files.");
			stringChanged = true;
		}
		tempString = strings.getString("msg-error-fix");
		if(tempString == null){
			strings.set("msg-error-fix", "No owned door found.");
			stringChanged = true;
		}
		tempString = strings.getString("msg-error-edit");
		if(tempString == null){
			strings.set("msg-error-edit", "First select a sign by right clicking it.");
			stringChanged = true;
		}
		tempString = strings.getString("msg-owner-edit");
		if(tempString == null){
			strings.set("msg-owner-edit", "Sign edited successfully.");
			stringChanged = true;
		}
		
		
		/*
		
		tempString = strings.getString("");
		if(tempString == null){
			strings.set("", "");
			stringChanged = true;
		}
		
		*/
		
		if(original) if(stringChanged){
			try{
				strings.save(stringsFile);
			}
			catch(Exception ex){}
		}
	}
	
	
	//********************************************************************************************************************
	// Start of public section
	
	
	public static boolean isProtected(Block block){
		if(!enabled) return(false);
		
		int			type = block.getTypeId();
		
		if(type == Material.WALL_SIGN.getId()){
			Sign		sign = (Sign) block.getState();
			String		text = sign.getLine(0).replaceAll("(?i)\u00A7[0-F]", "").toLowerCase();
			
			if(text.equals("[private]") || text.equalsIgnoreCase(altPrivate)){
				return(true);
			}
			else if(text.equals("[more users]") || text.equalsIgnoreCase(altMoreUsers)){
				Block		checkBlock = getSignAttachedBlock(block);
				
				if(checkBlock != null) if(findBlockOwner(checkBlock) != null){
					return(true);
				}
			}
		}
		else if(Lockette.findBlockOwner(block) != null) return(true);
		
		return(false);
	}
	
	
	public static String getProtectedOwner(Block block){
		if(!enabled) return(null);
		
		int			type = block.getTypeId();
		
		if(type == Material.WALL_SIGN.getId()){
			Sign		sign = (Sign) block.getState();
			String		text = sign.getLine(0).replaceAll("(?i)\u00A7[0-F]", "").toLowerCase();
			
			if(text.equals("[private]") || text.equalsIgnoreCase(altPrivate)){
				return(sign.getLine(1).replaceAll("(?i)\u00A7[0-F]", ""));
			}
			else if(text.equals("[more users]") || text.equalsIgnoreCase(altMoreUsers)){
				Block		checkBlock = getSignAttachedBlock(block);
				
				if(checkBlock != null){
					Block		signBlock = findBlockOwner(checkBlock);
					
					if(signBlock != null){
						sign = (Sign) signBlock.getState();
						
						return(sign.getLine(1).replaceAll("(?i)\u00A7[0-F]", ""));
					}
				}
			}
		}
		else{
			Block		signBlock = Lockette.findBlockOwner(block);
			
			if(signBlock != null){
				Sign		sign = (Sign) signBlock.getState();
				
				return(sign.getLine(1).replaceAll("(?i)\u00A7[0-F]", ""));
			}
		}
		
		return(null);
	}
	
	
	public static boolean isOwner(Block block, String name){
		if(!enabled) return(true);
		
		Block		checkBlock = Lockette.findBlockOwner(block);
		
		if(checkBlock == null) return(true);
		
		Sign		sign = (Sign) checkBlock.getState();
		int			length = name.length();

		if(length > 15) length = 15;
		
		// Check owner only.
		if(sign.getLine(1).replaceAll("(?i)\u00A7[0-F]", "").equals(name.substring(0, length))){
			return(true);
		}
		
		return(false);
	}
	
	
	public static boolean isUser(Block block, String name, boolean withGroups){
		if(!enabled) return(true);
		
		Block		signBlock = Lockette.findBlockOwner(block);
		
		if(signBlock == null) return(true);
		

		// Check main three users.

		Sign		sign = (Sign) signBlock.getState();
		int			length = name.length();
		String		line;
		int			y;
		
		if(length > 15) length = 15;
		
		for(y = 1; y <= 3; ++y) if(!sign.getLine(y).isEmpty()){
			line = sign.getLine(y).replaceAll("(?i)\u00A7[0-F]", "");
			
			// Check if the name is there verbatum.
			if(line.equalsIgnoreCase(name.substring(0, length))) return(true);

			// Check if name is in a group listed on the sign.
			if(withGroups) if(plugin.inGroup(block.getWorld(), name, line)) return(true);
		}
		
		
		// Check for more users.
		
		List<Block>	list = Lockette.findBlockUsers(block, signBlock);
		int			x, count = list.size();
		
		for(x = 0; x < count; ++x){
			sign = (Sign) list.get(x).getState();
			
			for(y = 1; y <= 3; ++y) if(!sign.getLine(y).isEmpty()){
				line = sign.getLine(y).replaceAll("(?i)\u00A7[0-F]", "");

				// Check if the name is there verbatum.
				if(line.equalsIgnoreCase(name.substring(0, length))) return(true);

				// Check if name is in a group listed on the sign.
				if(withGroups) if(plugin.inGroup(block.getWorld(), name, line)) return(true);
			}
		}
		
		
		// User doesn't have permission.
		return(false);
	}
	
	
	public static boolean isEveryone(Block block){
		if(!enabled) return(true);
		
		Block		signBlock = Lockette.findBlockOwner(block);
		
		if(signBlock == null) return(true);
		

		// Check main three users.

		Sign		sign = (Sign) signBlock.getState();
		String		line;
		int			y;
		
		for(y = 1; y <= 3; ++y) if(!sign.getLine(y).isEmpty()){
			line = sign.getLine(y).replaceAll("(?i)\u00A7[0-F]", "");
			
			if(line.equalsIgnoreCase("[Everyone]") || line.equalsIgnoreCase(Lockette.altEveryone)) return(true);
		}
		
		
		// Check for more users.
		
		List<Block>	list = Lockette.findBlockUsers(block, signBlock);
		int			x, count = list.size();
		
		for(x = 0; x < count; ++x){
			sign = (Sign) list.get(x).getState();
			
			for(y = 1; y <= 3; ++y) if(!sign.getLine(y).isEmpty()){
				line = sign.getLine(y).replaceAll("(?i)\u00A7[0-F]", "");
				
				if(line.equalsIgnoreCase("[Everyone]") || line.equalsIgnoreCase(Lockette.altEveryone)) return(true);
			}
		}
		
		
		// Everyone doesn't have permission.
		return(false);
	}
	
	
	
	//********************************************************************************************************************
	// Start of external permissions section
	
	
	protected boolean usingExternalPermissions(){
		if(!usePermissions) return(false);
		return(super.usingExternalPermissions());
		//return(usePermissions);
	}
	
	
	protected boolean usingExternalZones(){
		return(super.usingExternalZones());
	}
	
	
	protected String getLocalizedEveryone(){
		return(altEveryone);
	}
	
	
	protected String getLocalizedOperators(){
		return(altOperators);
	}
	
	
	//********************************************************************************************************************
	// Start of utility section
	

	protected void localizedMessage(Player player, String broadcast, String key){
		localizedMessage(player, broadcast, key, null, null);
	}
	protected void localizedMessage(Player player, String broadcast, String key, String sub){
		localizedMessage(player, broadcast, key, sub, null);
	}
	protected void localizedMessage(Player player, String broadcast, String key, String sub, String num){
		String		color = "";
		
		// Filter and color based on message type.
		if(key.startsWith("msg-user-")){
			if(broadcast == null) if(!Lockette.msgUser) return;
			color = ChatColor.YELLOW.toString();
		}
		else if(key.startsWith("msg-owner-")){
			if(broadcast == null) if(!Lockette.msgOwner) return;
			color = ChatColor.GOLD.toString();
		}
		else if(key.startsWith("msg-admin-")){
			if(broadcast == null) if(!Lockette.msgAdmin) return;
			color = ChatColor.RED.toString();
		}
		else if(key.startsWith("msg-error-")){
			if(broadcast == null) if(!Lockette.msgError) return;
			color = ChatColor.RED.toString();
		}
		else if(key.startsWith("msg-help-")){
			if(broadcast == null) if(!Lockette.msgHelp) return;
			color = ChatColor.GOLD.toString();
		}
		
		// Fetch the requested message string.
		String		message = strings.getString(key);
		if((message == null) || message.isEmpty()) return;
		
		// Do place holder substitution.
		message = message.replaceAll("&([0-9A-Fa-f])", "\u00A7$1");
		if(sub != null) message = message.replaceAll("\\*\\*\\*", sub + color);
		if(num != null) message = message.replaceAll("###", num);
		if(player != null) message = message.replaceAll("@@@", player.getName());
		
		// Send out the formatted message.
		if(broadcast != null) selectiveBroadcast(broadcast, color + "[Lockette] " + message);
		else if(player != null) player.sendMessage(color + "[Lockette] " + message);
	}
	
	
	// Find the owner for any block.
	protected static Block findBlockOwner(Block block){
		// Pass to a special version with specific values.
		return(findBlockOwner(block, null, false));
	}
	
	
	// Version for determining if a container is released.
	// Should return non-null if destroying the block will surely cause the the sign to fall off.
	// Okay for trap doors, though could be optimized.
	protected static Block findBlockOwnerBreak(Block block){
		int			type = block.getTypeId();
		
		
		// Check known block types.
		
		if(type == Material.CHEST.getId()){
			return(findBlockOwnerBase(block, null, false, false, false, false, false));
		}
		if((type == Material.DISPENSER.getId()) || (type == Material.FURNACE.getId()) || (type == Material.BURNING_FURNACE.getId()) ||
				(type == Material.BREWING_STAND.getId()) || Lockette.isInList(type, Lockette.customBlockList)){
			return(findBlockOwnerBase(block, null, false, false, false, false, false));
		}
		if(Lockette.protectTrapDoors) if(type == Material.TRAP_DOOR.getId()){
			return(findBlockOwnerBase(block, null, false, false, false, false, false));
		}
		if(Lockette.protectDoors) if((type == Material.WOODEN_DOOR.getId()) || (type == Material.IRON_DOOR_BLOCK.getId()) || (type == materialFenceGate)){
			return(findBlockOwnerBase(block, null, false, true, true, false, false));
		}
		
		
		
		Block		checkBlock;

		// This should be edited if invalid signs can be destroyed..........
		checkBlock = findBlockOwnerBase(block, null, false, false, false, false, false);
		if(checkBlock != null) return(checkBlock);
		
		if(Lockette.protectTrapDoors){
			// Need to check if there is a trap door attached to the block, and check for a sign attached there.
			// This is the bit that could be optimized.
			
			checkBlock = block.getRelative(BlockFace.NORTH);
			if(checkBlock.getTypeId() == Material.TRAP_DOOR.getId()){
				if((checkBlock.getData() & 0x3) == 2){
					checkBlock = findBlockOwnerBase(checkBlock, null, false, false, false, false, false);
					if(checkBlock != null) return(checkBlock);
				}
			}
			
			checkBlock = block.getRelative(BlockFace.EAST);
			if(checkBlock.getTypeId() == Material.TRAP_DOOR.getId()){
				if((checkBlock.getData() & 0x3) == 0){
					checkBlock = findBlockOwnerBase(checkBlock, null, false, false, false, false, false);
					if(checkBlock != null) return(checkBlock);
				}
			}
			
			checkBlock = block.getRelative(BlockFace.SOUTH);
			if(checkBlock.getTypeId() == Material.TRAP_DOOR.getId()){
				if((checkBlock.getData() & 0x3) == 3){
					checkBlock = findBlockOwnerBase(checkBlock, null, false, false, false, false, false);
					if(checkBlock != null) return(checkBlock);
				}
			}
			
			checkBlock = block.getRelative(BlockFace.WEST);
			if(checkBlock.getTypeId() == Material.TRAP_DOOR.getId()){
				if((checkBlock.getData() & 0x3) == 1){
					checkBlock = findBlockOwnerBase(checkBlock, null, false, false, false, false, false);
					if(checkBlock != null) return(checkBlock);
				}
			}
		}
		
		if(Lockette.protectDoors){
			// Need to check if there is a door above block, and check for a sign attached there.
			
			checkBlock = block.getRelative(BlockFace.UP);
			type = checkBlock.getTypeId();
			
			if((type != Material.WOODEN_DOOR.getId()) && (type != Material.IRON_DOOR_BLOCK.getId()) && (type != materialFenceGate)){
				// Handle door above type.
				
				return(findBlockOwnerBase(checkBlock, null, false, true, true, false, false));
			}
		}
		
		return(null);
	}
	
	
	// Version for finding conflicts, when creating a new sign.
	// Ignore the sign being made, in case another plugin has set the text of the sign prematurely.
	protected static Block findBlockOwner(Block block, Block ignoreBlock, boolean iterateFurther){
		int			type = block.getTypeId();
		Location	ignore;
		
		if(ignoreBlock != null) ignore = ignoreBlock.getLocation();
		else ignore = null;
		
		
		// Check known block types.
		
		if(type == Material.CHEST.getId()){
			return(findBlockOwnerBase(block, ignore, true, false, false, false, false));
		}
		if((type == Material.DISPENSER.getId()) || (type == Material.FURNACE.getId()) || (type == Material.BURNING_FURNACE.getId()) ||
				(type == Material.BREWING_STAND.getId()) || Lockette.isInList(type, Lockette.customBlockList)){
			return(findBlockOwnerBase(block, ignore, false, false, false, false, false));
		}
		if(Lockette.protectTrapDoors) if(type == Material.TRAP_DOOR.getId()){
			// Need to check block it is attached to as well as other attached trap doors.
			//return(findBlockOwnerBase(block, ignore, false, false, false, false, false));
			return(findBlockOwner(getTrapDoorAttachedBlock(block), ignoreBlock, false));
		}
		if(Lockette.protectDoors) if((type == Material.WOODEN_DOOR.getId()) || (type == Material.IRON_DOOR_BLOCK.getId()) || (type == materialFenceGate)){
			return(findBlockOwnerBase(block, ignore, true, true, true, true, iterateFurther));
		}
		
		
		Block		checkBlock, result;
		
		if(Lockette.protectTrapDoors){
			// Check base block, as it might have the sign and it isn't checked below.
			
			checkBlock = findBlockOwnerBase(block, ignore, false, false, false, false, false);
			if(checkBlock != null) return(checkBlock);
			
			
			// Need to check if there is a trap door attached to the block, and check for a sign attached there.
			
			checkBlock = block.getRelative(BlockFace.NORTH);
			if(checkBlock.getTypeId() == Material.TRAP_DOOR.getId()){
				if((checkBlock.getData() & 0x3) == 2){
					checkBlock = findBlockOwnerBase(checkBlock, ignore, false, false, false, false, false);
					if(checkBlock != null) return(checkBlock);
				}
			}
			
			checkBlock = block.getRelative(BlockFace.EAST);
			if(checkBlock.getTypeId() == Material.TRAP_DOOR.getId()){
				if((checkBlock.getData() & 0x3) == 0){
					checkBlock = findBlockOwnerBase(checkBlock, ignore, false, false, false, false, false);
					if(checkBlock != null) return(checkBlock);
				}
			}
			
			checkBlock = block.getRelative(BlockFace.SOUTH);
			if(checkBlock.getTypeId() == Material.TRAP_DOOR.getId()){
				if((checkBlock.getData() & 0x3) == 3){
					checkBlock = findBlockOwnerBase(checkBlock, ignore, false, false, false, false, false);
					if(checkBlock != null) return(checkBlock);
				}
			}
			
			checkBlock = block.getRelative(BlockFace.WEST);
			if(checkBlock.getTypeId() == Material.TRAP_DOOR.getId()){
				if((checkBlock.getData() & 0x3) == 1){
					checkBlock = findBlockOwnerBase(checkBlock, ignore, false, false, false, false, false);
					if(checkBlock != null) return(checkBlock);
				}
			}
		}
		
		if(Lockette.protectDoors){
			// Don't check the block but check for doors above then below the block, which includes the block.
			
			checkBlock = block.getRelative(BlockFace.UP);
			type = checkBlock.getTypeId();
			if((type == Material.WOODEN_DOOR.getId()) || (type == Material.IRON_DOOR_BLOCK.getId()) || (type == materialFenceGate)){
				// Handle door above type.
				
				result = findBlockOwnerBase(checkBlock, ignore, true, true, true, true, iterateFurther);
				if(result != null) return(result);
			}
			
			// This is needed to protect the other block above double doors.
			
			checkBlock = block.getRelative(BlockFace.DOWN);
			type = checkBlock.getTypeId();
			if((type == Material.WOODEN_DOOR.getId()) || (type == Material.IRON_DOOR_BLOCK.getId()) || (type == materialFenceGate)){
				// For door below only.
				// Don't include the block below door, as a sign there would not protect the target block.
				
				Block		checkBlock2 = checkBlock.getRelative(BlockFace.DOWN);
				type = checkBlock2.getTypeId();
				if((type == Material.WOODEN_DOOR.getId()) || (type == Material.IRON_DOOR_BLOCK.getId()) || (type == materialFenceGate)){
					return(findBlockOwnerBase(checkBlock2, ignore, true, true, false, true, iterateFurther));
				}
				else{
					return(findBlockOwnerBase(checkBlock, ignore, true, true, false, true, iterateFurther));
				}
			}
		}
		
		return(null);
	}
	
	
	// Should only be called by the above related functions.
	// Should generally not be passed a hinge block, only a known container or door.
	private static Block findBlockOwnerBase(Block block, Location ignore, boolean iterate, boolean iterateUp, boolean iterateDown, boolean includeEnds, boolean iterateFurther){
		Block		checkBlock;
		int			type;
		byte		face;
		boolean		doCheck;
		
		
		// Check up and down along door surfaces, with a recursive call and iterate false.

		if(iterateUp){
			checkBlock = block.getRelative(BlockFace.UP);
			type = checkBlock.getTypeId();
			
			if((type == Material.WOODEN_DOOR.getId()) || (type == Material.IRON_DOOR_BLOCK.getId()) || (type == materialFenceGate)){
				checkBlock = findBlockOwnerBase(checkBlock, ignore, false, iterateUp, false, includeEnds, false);
			}
			else if(includeEnds) checkBlock = findBlockOwnerBase(checkBlock, ignore, false, false, false, includeEnds, false);
			else checkBlock = null;
			
			if(checkBlock != null) return(checkBlock);
		}
		
		if(iterateDown){
			checkBlock = block.getRelative(BlockFace.DOWN);
			type = checkBlock.getTypeId();
			
			if((type == Material.WOODEN_DOOR.getId()) || (type == Material.IRON_DOOR_BLOCK.getId()) || (type == materialFenceGate)){
				checkBlock = findBlockOwnerBase(checkBlock, ignore, false, false, iterateDown, includeEnds, false);
			}
			else if(includeEnds) checkBlock = findBlockOwnerBase(checkBlock, ignore, false, false, false, includeEnds, false);
			else checkBlock = null;
			
			if(checkBlock != null) return(checkBlock);
		}
		
		
		// Check around the originating block, in the order NESW.
		// If a sign is found and it is not the ignored block, check the text.
		// If it is not a sign and iterate is true, do a recursive call with iterate false.
		// (Or further, though this currently backtracks slightly.)
		
		checkBlock = block.getRelative(BlockFace.NORTH);
		if(checkBlock.getTypeId() == Material.WALL_SIGN.getId()){
			face = checkBlock.getData();
			if(face == 4){
				// Ignore a sign being created.
				
				if(ignore == null) doCheck = true;
				else if(checkBlock.getLocation().equals(ignore)) doCheck = false;
				else doCheck = true;
				
				if(doCheck){
					Sign		sign = (Sign) checkBlock.getState();
					String		text = sign.getLine(0).replaceAll("(?i)\u00A7[0-F]", "").toLowerCase();
					
					if(text.equals("[private]") || text.equalsIgnoreCase(altPrivate)) return(checkBlock);
				}
			}
		}
		else if(iterate) if(checkBlock.getTypeId() == block.getTypeId()){
			checkBlock = findBlockOwnerBase(checkBlock, ignore, iterateFurther, iterateUp, iterateDown, includeEnds, false);
			if(checkBlock != null) return(checkBlock);
		}
		
		checkBlock = block.getRelative(BlockFace.EAST);
		if(checkBlock.getTypeId() == Material.WALL_SIGN.getId()){
			face = checkBlock.getData();
			if(face == 2){
				// Ignore a sign being created.
				
				if(ignore == null) doCheck = true;
				else if(checkBlock.getLocation().equals(ignore)) doCheck = false;
				else doCheck = true;
				
				if(doCheck){
					Sign		sign = (Sign) checkBlock.getState();
					String		text = sign.getLine(0).replaceAll("(?i)\u00A7[0-F]", "").toLowerCase();
					
					if(text.equals("[private]") || text.equalsIgnoreCase(altPrivate)) return(checkBlock);
				}
			}
		}
		else if(iterate) if(checkBlock.getTypeId() == block.getTypeId()){
			checkBlock = findBlockOwnerBase(checkBlock, ignore, iterateFurther, iterateUp, iterateDown, includeEnds, false);
			if(checkBlock != null) return(checkBlock);
		}
		
		checkBlock = block.getRelative(BlockFace.SOUTH);
		if(checkBlock.getTypeId() == Material.WALL_SIGN.getId()){
			face = checkBlock.getData();
			if(face == 5){
				// Ignore a sign being created.
				
				if(ignore == null) doCheck = true;
				else if(checkBlock.getLocation().equals(ignore)) doCheck = false;
				else doCheck = true;
				
				if(doCheck){
					Sign		sign = (Sign) checkBlock.getState();
					String		text = sign.getLine(0).replaceAll("(?i)\u00A7[0-F]", "").toLowerCase();
					
					if(text.equals("[private]") || text.equalsIgnoreCase(altPrivate)) return(checkBlock);
				}
			}
		}
		else if(iterate) if(checkBlock.getTypeId() == block.getTypeId()){
			checkBlock = findBlockOwnerBase(checkBlock, ignore, iterateFurther, iterateUp, iterateDown, includeEnds, false);
			if(checkBlock != null) return(checkBlock);
		}
		
		checkBlock = block.getRelative(BlockFace.WEST);
		if(checkBlock.getTypeId() == Material.WALL_SIGN.getId()){
			face = checkBlock.getData();
			if(face == 3){
				// Ignore a sign being created.
				
				if(ignore == null) doCheck = true;
				else if(checkBlock.getLocation().equals(ignore)) doCheck = false;
				else doCheck = true;
				
				if(doCheck){
					Sign		sign = (Sign) checkBlock.getState();
					String		text = sign.getLine(0).replaceAll("(?i)\u00A7[0-F]", "").toLowerCase();
					
					if(text.equals("[private]") || text.equalsIgnoreCase(altPrivate)) return(checkBlock);
				}
			}
		}
		else if(iterate) if(checkBlock.getTypeId() == block.getTypeId()){
			checkBlock = findBlockOwnerBase(checkBlock, ignore, iterateFurther, iterateUp, iterateDown, includeEnds, false);
			if(checkBlock != null) return(checkBlock);
		}
		
		return(null);
	}
	
	
	protected static List<Block> findBlockUsers(Block block, Block signBlock){
		int			type = block.getTypeId();
		
		if(type == Material.CHEST.getId()) return(findBlockUsersBase(block, true, false, false, false, 0));
		if(Lockette.protectTrapDoors) if(type == Material.TRAP_DOOR.getId()){
			return(findBlockUsersBase(getTrapDoorAttachedBlock(block), false, false, false, true, 0));
		}
		if(Lockette.protectDoors) if((type == Material.WOODEN_DOOR.getId()) || (type == Material.IRON_DOOR_BLOCK.getId()) || (type == materialFenceGate)){
			return(findBlockUsersBase(block, true, true, true, false, signBlock.getY()));
		}
		return(findBlockUsersBase(block, false, false, false, false, 0));
	}
	
	
	private static List<Block> findBlockUsersBase(Block block, boolean iterate, boolean iterateUp, boolean iterateDown, boolean traps, int includeYPos){
		Block		checkBlock;
		int			type;
		byte		face;
		List<Block> list = new ArrayList<Block>();
		
		
		// Experimental door code, check up and down.

		if(iterateUp){
			checkBlock = block.getRelative(BlockFace.UP);
			type = checkBlock.getTypeId();
			
			if((type == Material.WOODEN_DOOR.getId()) || (type == Material.IRON_DOOR_BLOCK.getId()) || (type == materialFenceGate)){
				list.addAll(findBlockUsersBase(checkBlock, false, iterateUp, false, false, includeYPos));
			}
			// Limitation for more users sign.
			else if(checkBlock.getY() == includeYPos) list.addAll(findBlockUsersBase(checkBlock, false, false, false, false, includeYPos));
		}
		
		if(iterateDown){
			checkBlock = block.getRelative(BlockFace.DOWN);
			type = checkBlock.getTypeId();
			
			if((type == Material.WOODEN_DOOR.getId()) || (type == Material.IRON_DOOR_BLOCK.getId()) || (type == materialFenceGate)){
				list.addAll(findBlockUsersBase(checkBlock, false, false, iterateDown, false, includeYPos));
			}
			// No limitation here.
			else list.addAll(findBlockUsersBase(checkBlock, false, false, false, false, includeYPos));
		}
		
		
		// Check around the originating block, in the order NESW.
		
		checkBlock = block.getRelative(BlockFace.NORTH);
		type = checkBlock.getTypeId();
		if(type == Material.WALL_SIGN.getId()){
			face = checkBlock.getData();
			if(face == 4){
				Sign		sign = (Sign) checkBlock.getState();
				String		text = sign.getLine(0).replaceAll("(?i)\u00A7[0-F]", "").toLowerCase();
				
				if(text.equals("[more users]") || text.equalsIgnoreCase(altMoreUsers)) list.add(checkBlock);
			}
		}
		else if(iterate){
			if(type == block.getTypeId()){
				list.addAll(findBlockUsersBase(checkBlock, false, iterateUp, iterateDown, false, includeYPos));
			}
		}
		else if(traps) if(type == Material.TRAP_DOOR.getId()){
			face = checkBlock.getData();
			if((face & 3) == 2){
				list.addAll(findBlockUsersBase(checkBlock, false, false, false, false, includeYPos));
			}
		}
		
		checkBlock = block.getRelative(BlockFace.EAST);
		type = checkBlock.getTypeId();
		if(type == Material.WALL_SIGN.getId()){
			face = checkBlock.getData();
			if(face == 2){
				Sign		sign = (Sign) checkBlock.getState();
				String		text = sign.getLine(0).replaceAll("(?i)\u00A7[0-F]", "").toLowerCase();

				if(text.equals("[more users]") || text.equalsIgnoreCase(altMoreUsers)) list.add(checkBlock);
			}
		}
		else if(iterate){
			if(type == block.getTypeId()){
				list.addAll(findBlockUsersBase(checkBlock, false, iterateUp, iterateDown, false, includeYPos));
			}
		}
		else if(traps) if(type == Material.TRAP_DOOR.getId()){
			face = checkBlock.getData();
			if((face & 3) == 0){
				list.addAll(findBlockUsersBase(checkBlock, false, false, false, false, includeYPos));
			}
		}
		
		checkBlock = block.getRelative(BlockFace.SOUTH);
		type = checkBlock.getTypeId();
		if(type == Material.WALL_SIGN.getId()){
			face = checkBlock.getData();
			if(face == 5){
				Sign		sign = (Sign) checkBlock.getState();
				String		text = sign.getLine(0).replaceAll("(?i)\u00A7[0-F]", "").toLowerCase();

				if(text.equals("[more users]") || text.equalsIgnoreCase(altMoreUsers)) list.add(checkBlock);
			}
		}
		else if(iterate){
			if(type == block.getTypeId()){
				list.addAll(findBlockUsersBase(checkBlock, false, iterateUp, iterateDown, false, includeYPos));
			}
		}
		else if(traps) if(type == Material.TRAP_DOOR.getId()){
			face = checkBlock.getData();
			if((face & 3) == 3){
				list.addAll(findBlockUsersBase(checkBlock, false, false, false, false, includeYPos));
			}
		}
		
		checkBlock = block.getRelative(BlockFace.WEST);
		type = checkBlock.getTypeId();
		if(type == Material.WALL_SIGN.getId()){
			face = checkBlock.getData();
			if(face == 3){
				Sign		sign = (Sign) checkBlock.getState();
				String		text = sign.getLine(0).replaceAll("(?i)\u00A7[0-F]", "").toLowerCase();

				if(text.equals("[more users]") || text.equalsIgnoreCase(altMoreUsers)) list.add(checkBlock);
			}
		}
		else if(iterate){
			if(type == block.getTypeId()){
				list.addAll(findBlockUsersBase(checkBlock, false, iterateUp, iterateDown, false, includeYPos));
			}
		}
		else if(traps) if(type == Material.TRAP_DOOR.getId()){
			face = checkBlock.getData();
			if((face & 3) == 1){
				list.addAll(findBlockUsersBase(checkBlock, false, false, false, false, includeYPos));
			}
		}
		
		return(list);
	}
	

	protected static int findChestCountNear(Block block){
		return(findChestCountNearBase(block, (byte) 0));
	}
	
	
	private static int findChestCountNearBase(Block block, byte face){
		int			count = 0;
		Block		checkBlock;
		
		
		if(face != 4){
			checkBlock = block.getRelative(BlockFace.NORTH);
			if(checkBlock.getTypeId() == Material.CHEST.getId()){
				++count;
				if(face == 0) count += findChestCountNearBase(checkBlock, (byte) 5);
			}
		}
		
		if(face != 2){
			checkBlock = block.getRelative(BlockFace.EAST);
			if(checkBlock.getTypeId() == Material.CHEST.getId()){
				++count;
				if(face == 0) count += findChestCountNearBase(checkBlock, (byte) 3);
			}
		}
		
		if(face != 5){
			checkBlock = block.getRelative(BlockFace.SOUTH);
			if(checkBlock.getTypeId() == Material.CHEST.getId()){
				++count;
				if(face == 0) count += findChestCountNearBase(checkBlock, (byte) 4);
			}
		}
		
		if(face != 3){
			checkBlock = block.getRelative(BlockFace.WEST);
			if(checkBlock.getTypeId() == Material.CHEST.getId()){
				++count;
				if(face == 0) count += findChestCountNearBase(checkBlock, (byte) 2);
			}
		}
		
		return(count);
	}
	
	
	protected static void rotateChestOrientation(Block block, BlockFace blockFace){
		if(block.getTypeId() != Material.CHEST.getId()) return;
		if(!rotateChests) if(block.getData() != 0) return;
		
		byte		face;

		if(blockFace == BlockFace.NORTH) face = 4;
		else if(blockFace == BlockFace.EAST) face = 2;
		else if(blockFace == BlockFace.SOUTH) face = 5;
		else if(blockFace == BlockFace.WEST) face = 3;
		else return;
		
		
		Block		checkBlock;
		
		
		checkBlock = block.getRelative(BlockFace.NORTH);
		if(checkBlock.getTypeId() == Material.CHEST.getId()){
			if((face == 2) || (face == 3)){
				block.setData(face);
				checkBlock.setData(face);
			}
			return;
		}
		
		checkBlock = block.getRelative(BlockFace.EAST);
		if(checkBlock.getTypeId() == Material.CHEST.getId()){
			if((face == 4) || (face == 5)){
				block.setData(face);
				checkBlock.setData(face);
			}
			return;
		}
		
		checkBlock = block.getRelative(BlockFace.SOUTH);
		if(checkBlock.getTypeId() == Material.CHEST.getId()){
			if((face == 2) || (face == 3)){
				block.setData(face);
				checkBlock.setData(face);
			}
			return;
		}
		
		checkBlock = block.getRelative(BlockFace.WEST);
		if(checkBlock.getTypeId() == Material.CHEST.getId()){
			if((face == 4) || (face == 5)){
				block.setData(face);
				checkBlock.setData(face);
			}
			return;
		}
		
		block.setData(face);
	}
	
	
	// Toggle all doors.  (Used by rightclick action to get door list.)
	protected static List<Block> toggleDoors(Block block, Block keyBlock, boolean wooden, boolean trap){
		List<Block> list = new ArrayList<Block>();
		
		toggleDoorBase(block, keyBlock, !trap, wooden, list);
		try{
			if(!wooden) block.getWorld().playEffect(block.getLocation(), Effect.DOOR_TOGGLE, 0);
		}
		catch(NoSuchFieldError ex){}
		catch(NoSuchMethodError ex){}
		catch(NoClassDefFoundError ex){}
		
		return(list);
	}
	
	
	// Toggle one door.  (Used only by pre-561 builds to fix for bug.)
	// Now also used to fix doors.
	protected static void toggleSingleDoor(Block block){
		int			type = block.getTypeId();
		//List<Block> list = new ArrayList<Block>();

		if((type == Material.WOODEN_DOOR.getId()) || (type == Material.IRON_DOOR_BLOCK.getId())){
			toggleDoorBase(block, null, true, false, null);
		}
		else if((type == materialTrapDoor) || (type == materialFenceGate)){
			toggleDoorBase(block, null, false, false, null);
		}
		//return(list);
	}
	
	
	// Toggle half door, or trap door.  (Used by automatic door closer.)
	protected static void toggleHalfDoor(Block block, boolean effect){
		int			type = block.getTypeId();
		//List<Block> list = new ArrayList<Block>();
		
		//toggleDoor(block, null, false, false, null);
		//return(list);
		if((type == Material.WOODEN_DOOR.getId()) || (type == Material.IRON_DOOR_BLOCK.getId()) || (type == materialTrapDoor) || (type == materialFenceGate)){
			block.setData((byte) (block.getData() ^ 4));
			try{
				if(effect) block.getWorld().playEffect(block.getLocation(), Effect.DOOR_TOGGLE, 0);
			}
			catch(NoSuchFieldError ex){}
			catch(NoSuchMethodError ex){}
			catch(NoClassDefFoundError ex){}
		}
	}
	
	
	// Main recursive function for toggling a door pair.  (No good for trap doors.)
	private static void toggleDoorBase(Block block, Block keyBlock, boolean iterateUpDown, boolean skipDoor, List<Block> list){
		Block		checkBlock;
		
		// Toggle this door.
		
		if(list != null) list.add(block);
		if(!skipDoor) block.setData((byte) (block.getData() ^ 4));
		

		// Check up and down.
		
		if(iterateUpDown){
			checkBlock = block.getRelative(BlockFace.UP);
			if(checkBlock.getTypeId() == block.getTypeId()) toggleDoorBase(checkBlock, null, false, skipDoor, list);
			
			checkBlock = block.getRelative(BlockFace.DOWN);
			if(checkBlock.getTypeId() == block.getTypeId()) toggleDoorBase(checkBlock, null, false, skipDoor, list);
		}
		
		
		// Check around the originating block, in the order NESW.
		
		if(keyBlock != null){
			checkBlock = block.getRelative(BlockFace.NORTH);
			if(checkBlock.getTypeId() == block.getTypeId()){
				if(((checkBlock.getX() == keyBlock.getX()) && (checkBlock.getZ() == keyBlock.getZ())) ||
						((block.getX() == keyBlock.getX()) && (block.getZ() == keyBlock.getZ()))){
					toggleDoorBase(checkBlock, null, true, false, list);
				}
			}
			
			checkBlock = block.getRelative(BlockFace.EAST);
			if(checkBlock.getTypeId() == block.getTypeId()){
				if(((checkBlock.getX() == keyBlock.getX()) && (checkBlock.getZ() == keyBlock.getZ())) ||
						((block.getX() == keyBlock.getX()) && (block.getZ() == keyBlock.getZ()))){
					toggleDoorBase(checkBlock, null, true, false, list);
				}
			}
			
			checkBlock = block.getRelative(BlockFace.SOUTH);
			if(checkBlock.getTypeId() == block.getTypeId()){
				if(((checkBlock.getX() == keyBlock.getX()) && (checkBlock.getZ() == keyBlock.getZ())) ||
						((block.getX() == keyBlock.getX()) && (block.getZ() == keyBlock.getZ()))){
					toggleDoorBase(checkBlock, null, true, false, list);
				}
			}
			
			checkBlock = block.getRelative(BlockFace.WEST);
			if(checkBlock.getTypeId() == block.getTypeId()){
				if(((checkBlock.getX() == keyBlock.getX()) && (checkBlock.getZ() == keyBlock.getZ())) ||
						((block.getX() == keyBlock.getX()) && (block.getZ() == keyBlock.getZ()))){
					toggleDoorBase(checkBlock, null, true, false, list);
				}
			}
		}
	}
	
	
	//********************************************************************************************************************
	// Start of utility section
	
	
	protected static int getSignOption(Block signBlock, String tag, String altTag, int defaultValue){
		Sign		sign = (Sign) signBlock.getState();
		
		
		// Check main two users.
		
		String		line;
		int			x, y, end, index;
		
		for(y = 2; y <= 3; ++y) if(!sign.getLine(y).isEmpty()){
			line = sign.getLine(y).replaceAll("(?i)\u00A7[0-F]", "");
			//if(line.isEmpty()) continue;
			
			end = line.length() - 1;
			
			if(end >= 2) if((line.charAt(0) == '[') && (line.charAt(end) == ']')){
				index = line.indexOf(":");
				
				if(index == -1){
					// No number.
					if(line.substring(1, end).equalsIgnoreCase(tag) || line.substring(1, end).equalsIgnoreCase(altTag)){
						return(defaultValue);
					}
				}
				else{
					// Number.
					if(line.substring(1, index).equalsIgnoreCase(tag) || line.substring(1, index).equalsIgnoreCase(altTag)){
						// Trim junk around the number.
						
						for(x = index; x < end; ++x){
							if(Character.isDigit(line.charAt(x))){
								index = x;
								break;
							}
						}
						for(x = index+1; x < end; ++x){
							if(!Character.isDigit(line.charAt(x))){
								end = x;
								break;
							}
						}
						
						
						// Try to parse the number, and return the result.
						try{
							int value = Integer.parseInt(line.substring(index, end));
							return(value);
						}
						catch(NumberFormatException ex){
							return(defaultValue);
						}
					}
				}
			}
		}
		
		return(defaultValue);
	}
	
	
	protected static boolean isInList(int target, List<Object> list){
		if(list == null) return(false);
		for(int x = 0; x < list.size(); ++x) if(list.get(x).equals(target)) return(true);
		return(false);
	}
}

