//
// This file is a component of Lockette for Bukkit, and was written by Acru Jovian.
// Distributed under the The Non-Profit Open Software License version 3.0 (NPOSL-3.0)
// http://www.opensource.org/licenses/NOSL3.0
//


package org.yi.acru.bukkit.Lockette;


// Imports.
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import org.bukkit.World;
import org.bukkit.block.Block;



public class LocketteDoorCloser implements Runnable{
	private static Lockette					plugin;
	private static int						doorTask = -1;
	private final PriorityQueue<closeTask>	closeTaskList = new PriorityQueue<closeTask>();
	
	
	
	public LocketteDoorCloser(Lockette instance){
		plugin = instance;
	}
	
	
	protected boolean start(){
		if(doorTask != -1) return(false);
		
		doorTask = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 100, 10);
		if(doorTask == -1) return(true);
		
		return(false);
	}
	
	
	protected boolean stop(){
		if(doorTask == -1) return(false);
		
		plugin.getServer().getScheduler().cancelTask(doorTask);
		doorTask = -1;
		cleanup();
		
		return(false);
	}
	
	
	protected void cleanup(){
		closeTask		door;
		
		while(!closeTaskList.isEmpty()){
			door = closeTaskList.poll();
			if(door == null) break;
			
			close(door);
		}
	}
	
	
	public void run(){
		if(closeTaskList.isEmpty()) return;
		
		Date		time = new Date();
		closeTask	door;
		
		while(time.after(closeTaskList.peek().time)){
			door = closeTaskList.poll();
			if(door == null) break;
			
			close(door);
			if(closeTaskList.isEmpty()) break;
		}
	}
	
	
	private void close(closeTask door){
		//Lockette.log.info("Closing at " + door.time);
		Lockette.toggleHalfDoor(door.world.getBlockAt(door.x, door.y, door.z), door.effect);
	}
	
	
	// Assumes all blocks in the list are for the same world.
	public void add(List<Block> list, boolean auto, int delta){
		if(list == null) return;
		if(list.isEmpty()) return;
		
		Iterator<closeTask>	it;
		Iterator<Block>		itb;
		
		closeTask	task;
		Block		block;
		World		world = list.get(0).getWorld();
		
		// Check each item in the task list for duplicates.
		it = closeTaskList.iterator();
		while(it.hasNext()){
			task = it.next();
			
			// Ignore block lists for another world.
			if(!task.world.equals(world)) continue;
			
			// Compare to each of the blocks in the new list.
			itb = list.iterator();
			while(itb.hasNext()){
				block = itb.next();
				
				// Ignore block lists for another world.
				//if(!block.getWorld().equals(task.world)) break;
				
				if((block.getX() == task.x) && (block.getY() == task.y) && (block.getZ() == task.z)){
					// Duplicate found, remove it from both lists.  A door to be closed was closed manually.
					//Lockette.log.info("Duplicate");
					it.remove();
					itb.remove();
					break;
				}
			}
		}
		
		
		// If the list is empty or the new door blocks aren't automatic, we are done.
		if(!auto) return;
		if(list.isEmpty()) return;
		
		
		// Add the new blocks to the task list.
		//(((block.getX()&0x7)*4)+2)
		//Lockette.log.info("Adding " + list.size());
		
		Date		time = new Date();
		time.setTime(time.getTime() + (delta * 1000L));
		
		for(int x = 0; x < list.size(); ++x){
			closeTaskList.add(new closeTask(time, list.get(x), x == 0));
		}
	}
	
	
	protected class closeTask implements Comparable<closeTask>{
		Date		time;
		World		world;
		int			x, y, z;
		boolean		effect;
		
		
		public closeTask(Date taskTime, Block block, boolean taskEffect){
			time = taskTime;
			world = block.getWorld();
			x = block.getX();
			y = block.getY();
			z = block.getZ();
			effect = taskEffect;
		}
		
		
		public int compareTo(closeTask arg){
			return(time.compareTo(arg.time));
		}
	}
}

