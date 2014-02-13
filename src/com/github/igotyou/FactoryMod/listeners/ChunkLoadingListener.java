package com.github.igotyou.FactoryMod.listeners;

import static com.untamedears.citadel.Utility.getReinforcement;
import static com.untamedears.citadel.Utility.isReinforced;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.util.Vector;

import com.github.igotyou.FactoryMod.FactoryModPlugin;
import com.github.igotyou.FactoryMod.interfaces.Factory;
import com.github.igotyou.FactoryMod.interfaces.Manager;
import com.github.igotyou.FactoryMod.managers.FactoryModManager;
import com.github.igotyou.FactoryMod.managers.ProductionManager;
import com.untamedears.citadel.entity.PlayerReinforcement;

public class ChunkLoadingListener implements Listener {
	private FactoryModManager factoryMan;
	
	/**
	 * Constructor
	 */
	public ChunkLoadingListener(FactoryModManager factoryManager)
	{
		this.factoryMan = factoryManager;
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onChunkUnload(ChunkUnloadEvent e) {
		int chunkX = e.getChunk().getX();
		int chunkZ = e.getChunk().getZ();
		Location location = new Vector(chunkX * 32, 0, chunkZ * 32).toLocation(e.getWorld());
		
		//is there a factory there? and if it has all its blocks
		for (Manager manager : factoryMan.getManagers())
		{
			for (Factory factory : manager.getLoadedFactories())
			{
				if (factory.keepChunksLoaded()) {
					int factoryChunkX = factory.getCenterLocation().getBlockX() / 32;
					int factoryChunkZ = factory.getCenterLocation().getBlockZ() / 32;
					int distance = Math.abs(chunkX - factoryChunkX) + Math.abs(chunkZ - factoryChunkZ);
					if (distance < factory.chunkLoadDistance()) {
						e.setCancelled(true);
					}
				}
			}
		}
	}
}
