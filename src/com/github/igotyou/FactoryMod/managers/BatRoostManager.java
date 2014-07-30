package com.github.igotyou.FactoryMod.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;

import com.github.igotyou.FactoryMod.FactoryModPlugin;
import com.github.igotyou.FactoryMod.Factorys.BatRoost;
import com.github.igotyou.FactoryMod.Factorys.BatRoost.OperationMode;
import com.github.igotyou.FactoryMod.Factorys.ProductionFactory;
import com.github.igotyou.FactoryMod.interfaces.Factory;
import com.github.igotyou.FactoryMod.interfaces.Manager;
import com.github.igotyou.FactoryMod.properties.BatRoostProperties;
import com.github.igotyou.FactoryMod.properties.PrintingPressProperties;
import com.github.igotyou.FactoryMod.utility.InteractionResponse;
import com.github.igotyou.FactoryMod.utility.InteractionResponse.InteractionResult;
import com.github.igotyou.FactoryMod.utility.ItemList;
import com.github.igotyou.FactoryMod.utility.NamedItemStack;

//original file:
/**
* Manager.java
* Purpose: Interface for Manager objects for basic manager functionality
*
* @author MrTwiggy
* @version 0.1 1/08/13
*/
//edited version:
/**
* Manager.java	 
* Purpose: Interface for Manager objects for basic manager functionality
* @author igotyou
*
*/

public class BatRoostManager implements Manager
{
	private FactoryModPlugin plugin;
	private List<BatRoost> producers;
	private long repairTime;
	
	public BatRoostManager(FactoryModPlugin plugin)
	{
		this.plugin = plugin;
		producers = new ArrayList<BatRoost>();
		//Set maintenance clock to 0
		updateFactorys();
	}
	
	public void save(File file) throws IOException 
	{
		//Takes difference between last repair update and current one and scales repair accordingly
		updateRepair(System.currentTimeMillis()-repairTime);
		repairTime=System.currentTimeMillis();
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		ObjectOutputStream oos = new ObjectOutputStream(fileOutputStream);
		int version = 1;
		oos.writeInt(version);
		oos.writeInt(producers.size());
		for (BatRoost production : producers)
		{
			Location centerlocation = production.getCenterLocation();
			Location inventoryLocation = production.getInventoryLocation();
			Location powerLocation = production.getPowerSourceLocation();
			
			oos.writeUTF(centerlocation.getWorld().getName());
			
			oos.writeInt(centerlocation.getBlockX());
			oos.writeInt(centerlocation.getBlockY());
			oos.writeInt(centerlocation.getBlockZ());

			oos.writeInt(inventoryLocation.getBlockX());
			oos.writeInt(inventoryLocation.getBlockY());
			oos.writeInt(inventoryLocation.getBlockZ());

			oos.writeInt(powerLocation.getBlockX());
			oos.writeInt(powerLocation.getBlockY());
			oos.writeInt(powerLocation.getBlockZ());
			
			oos.writeBoolean(production.getActive());
			oos.writeInt(production.getMode().getId());
			oos.writeInt(production.getProductionTimer());
			oos.writeInt(production.getEnergyTimer());
			oos.writeInt((int) production.getCurrentRepair());
			oos.writeLong(production.getTimeDisrepair());

			oos.writeInt(production.getLastFoodLevel());
			oos.writeLong(production.getLastFoodUpdateTime());
		}
		oos.flush();
		fileOutputStream.close();
	}

	public void load(File file) throws IOException 
	{
		try {
			repairTime=System.currentTimeMillis();
			FileInputStream fileInputStream = new FileInputStream(file);
			ObjectInputStream ois = new ObjectInputStream(fileInputStream);
			int version = ois.readInt();
			assert(version == 1);
			int count = ois.readInt();
			int i = 0;
			for (i = 0; i < count; i++)
			{
				String worldName = ois.readUTF();
				World world = plugin.getServer().getWorld(worldName);

				Location centerLocation = new Location(world, ois.readInt(), ois.readInt(), ois.readInt());
				Location inventoryLocation = new Location(world, ois.readInt(), ois.readInt(), ois.readInt());
				Location powerLocation = new Location(world, ois.readInt(), ois.readInt(), ois.readInt());
				boolean active = ois.readBoolean();
				OperationMode mode = BatRoost.OperationMode.byId(ois.readInt());
				int productionTimer = ois.readInt();
				int energyTimer = ois.readInt();
				int currentRepair = ois.readInt();
				long timeDisrepair  = ois.readLong();
				
				int lastFoodLevel = ois.readInt();
				long lastFoodUpdateTime = ois.readLong();
				
				BatRoost production = new BatRoost(centerLocation, inventoryLocation, powerLocation,
						active, productionTimer,
						energyTimer, currentRepair, timeDisrepair,
						mode,
						plugin.getBatRoostProperties(),
						lastFoodLevel, lastFoodUpdateTime
						);
				addFactory(production);
			}
			fileInputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void updateFactorys() 
	{
		plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
		{
			@Override
			public void run()
			{
				for (BatRoost production: producers)
				{
					production.update();
				}
			}
		}, 0L, FactoryModPlugin.PRODUCER_UPDATE_CYCLE);
	}

	public InteractionResponse createFactory(Location factoryLocation, Location inventoryLocation, Location powerSourceLocation) 
	{
	BatRoostProperties batRoostProperties = plugin.getBatRoostProperties();
		
		if (!factoryExistsAt(factoryLocation))
		{
			Block inventoryBlock = inventoryLocation.getBlock();
			Chest chest = (Chest) inventoryBlock.getState();
			Inventory chestInventory = chest.getInventory();
			ItemList<NamedItemStack> inputs = batRoostProperties.getConstructionMaterials();
			boolean hasMaterials = inputs.allIn(chestInventory);
			if (hasMaterials)
			{
				BatRoost production = new BatRoost(factoryLocation, inventoryLocation, powerSourceLocation, false, plugin.getBatRoostProperties());
				if (batRoostProperties.getConstructionMaterials().removeFrom(production.getInventory()))
				{
					addFactory(production);
					return new InteractionResponse(InteractionResult.SUCCESS, "Successfully created " + batRoostProperties.getName());
				}
			}
			return new InteractionResponse(InteractionResult.FAILURE, "Not enough materials in chest!");
		}
		return new InteractionResponse(InteractionResult.FAILURE, "There is already a " + batRoostProperties.getName() + " there!");
	}

	public InteractionResponse addFactory(Factory factory) 
	{
		BatRoost production = (BatRoost) factory;
		if (production.getCenterLocation().getBlock().getType().equals(FactoryModPlugin.CENTRAL_BLOCK_MATERIAL) && (!factoryExistsAt(production.getCenterLocation()))
				|| !factoryExistsAt(production.getInventoryLocation()) || !factoryExistsAt(production.getPowerSourceLocation()))
		{
			producers.add(production);
			return new InteractionResponse(InteractionResult.SUCCESS, "");
		}
		else
		{
			return new InteractionResponse(InteractionResult.FAILURE, "");
		}
	}

	public BatRoost getFactory(Location factoryLocation) 
	{
		for (BatRoost production : producers)
		{
			if (production.getCenterLocation().equals(factoryLocation) || production.getInventoryLocation().equals(factoryLocation)
					|| production.getPowerSourceLocation().equals(factoryLocation))
				return production;
		}
		return null;
	}
	
	public boolean factoryExistsAt(Location factoryLocation) 
	{
		boolean returnValue = false;
		if (getFactory(factoryLocation) != null)
		{
			returnValue = true;
		}
		return returnValue;
	}
	
	public boolean factoryWholeAt(Location factoryLocation) 
	{
		boolean returnValue = false;
		if (getFactory(factoryLocation) != null)
		{
			returnValue = getFactory(factoryLocation).isWhole(false);
		}
		return returnValue;
	}

	public void removeFactory(Factory factory) 
	{
		producers.remove((ProductionFactory)factory);
	}
	
	public void updateRepair(long time)
	{
		for (BatRoost production: producers)
		{
			production.updateRepair(time/((double)FactoryModPlugin.REPAIR_PERIOD));
		}
		long currentTime=System.currentTimeMillis();
		Iterator<BatRoost> itr=producers.iterator();
		while(itr.hasNext())
		{
			BatRoost producer=itr.next();
			if(currentTime>(producer.getTimeDisrepair()+FactoryModPlugin.DISREPAIR_PERIOD))
			{
				itr.remove();
			}
		}
	}
	
	public String getSavesFileName() 
	{
		return FactoryModPlugin.BAT_ROOSTS_SAVES_FILE;
	}

}
