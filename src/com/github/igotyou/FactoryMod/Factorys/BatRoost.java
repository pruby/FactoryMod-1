package com.github.igotyou.FactoryMod.Factorys;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import com.github.igotyou.FactoryMod.properties.BatRoostProperties;
import com.github.igotyou.FactoryMod.utility.InteractionResponse;
import com.github.igotyou.FactoryMod.utility.InteractionResponse.InteractionResult;
import com.github.igotyou.FactoryMod.utility.ItemList;
import com.github.igotyou.FactoryMod.utility.NamedItemStack;

public class BatRoost extends BaseFactory {
	
	private BatRoostProperties batRoostProperties;
	private OperationMode mode;
	public OperationMode getMode() {
		return mode;
	}

	private int lastFoodLevel;
	private long lastFoodUpdateTime;

	private final Pattern xCoord = Pattern.compile("X([-+][0-9]+)", Pattern.CASE_INSENSITIVE);
	private final Pattern zCoord = Pattern.compile("Z([-+][0-9]+)", Pattern.CASE_INSENSITIVE);

	public BatRoost(Location factoryLocation,
			Location factoryInventoryLocation, Location factoryPowerSource,
			boolean active, BatRoostProperties batRoostProperties) {
		super(factoryLocation, factoryInventoryLocation, factoryPowerSource, active,
				FactoryType.BAT_ROOST, "Bat Roost");
		this.mode = OperationMode.REPAIR;
		this.batRoostProperties = batRoostProperties;
		this.lastFoodLevel = batRoostProperties.getMaxFoodLevel();
		this.lastFoodUpdateTime = factoryLocation.getWorld().getFullTime();
	}

	public BatRoost(Location factoryLocation,
			Location factoryInventoryLocation, Location factoryPowerSource,
			boolean active, int currentProductionTimer,
			int currentEnergyTimer, int currentMaintenance, long timeDisrepair,
			OperationMode mode, BatRoostProperties batRoostProperties,
			int lastFoodLevel, long lastFoodUpdateTime) {
		super(factoryLocation, factoryInventoryLocation, factoryPowerSource,
				FactoryType.BAT_ROOST, active, "Bat Roost", currentProductionTimer,
				currentEnergyTimer, currentMaintenance, timeDisrepair);
		this.mode = mode;
		this.active = active;
		this.batRoostProperties = batRoostProperties;
		this.lastFoodLevel = lastFoodLevel;
		this.lastFoodUpdateTime = lastFoodUpdateTime;
	}
	
	private void updateFood() {
		long time = factoryLocation.getWorld().getFullTime();
		if (lastFoodUpdateTime > time) {
			lastFoodUpdateTime = time;
		}
		
		int foodUpdates = (int) (time - lastFoodUpdateTime) / batRoostProperties.getTicksPerFood();
		System.err.println("Food update check: (" + time + "," + lastFoodUpdateTime + "," + foodUpdates + ")");
		
		for (int i = 0; i < foodUpdates; ++i) {
			if (lastFoodLevel > 0) {
				// Eat food
				lastFoodLevel -= 1;
				if (batRoostProperties.testHeal((int) currentRepair)) {
					currentRepair -= batRoostProperties.getHealingRate();
					if (currentRepair < 0) currentRepair = 0;
				}
			} else {
				currentRepair += batRoostProperties.getStarvationRate();
				if (currentRepair > batRoostProperties.getMaxPopulationHealth()) {
					currentRepair = batRoostProperties.getMaxPopulationHealth();
				}
			}
			lastFoodUpdateTime += batRoostProperties.getTicksPerFood();
		}
	}
			
	@Override
	public void updateRepair(double percent) {
		// Skip normal loss of repair, use food instead
		updateFood();
	}
	
	@Override
	public double getEnergyTime() {
		return batRoostProperties.getEnergyTime();
	}

	@Override
	public double getProductionTime() {
		return getEnergyTime();
	}

	@Override
	public ItemList<NamedItemStack> getInputs() {
		ItemList<NamedItemStack> inputs = new ItemList<NamedItemStack>();
		switch(mode) {
		case FEED_COLONY:
			inputs.addAll(batRoostProperties.getFeedingMaterials());
			break;
		}
		return inputs;
	}

	@Override
	public ItemList<NamedItemStack> getOutputs() {
		ItemList<NamedItemStack> outputs = new ItemList<NamedItemStack>();
		return outputs;
	}

	@Override
	public ItemList<NamedItemStack> getRepairs() {
		ItemList<NamedItemStack> inputs = new ItemList<NamedItemStack>();
		switch(mode) {
		case REPAIR:
			inputs.addAll(batRoostProperties.getReplenishColonyMaterials());
		}
		return inputs;
	}

	@Override
	public int getMaxRepair() {
		return batRoostProperties.getMaxPopulationHealth();
	}
	
	@Override
	public void powerOn() {
		super.powerOn();
	}
	
	@Override
	public void fuelConsumed() {
		updateFood();
		
		switch(mode) {
		case REPAIR:
			break;
		}
	}
	
	public boolean isRepairing() {
		return mode == OperationMode.REPAIR;
	}
	
	/**
	 * Returns either a success or error message.
	 * Called by the blockListener when a player left clicks the center block, with the InteractionMaterial
	 */
	public List<InteractionResponse> getCentralBlockResponse()
	{
		updateFood();
		List<InteractionResponse> responses=new ArrayList<InteractionResponse>();
		//Is the factory off
		if (!active)
		{
			//is the recipe is initiated
			if (mode == null) {
				mode = OperationMode.REPAIR;
			} else {		
				mode = mode.getNext();
			}
			
			responses.add(new InteractionResponse(InteractionResult.SUCCESS, "-----------------------------------------------------"));
			responses.add(new InteractionResponse(InteractionResult.SUCCESS, "Switched mode to: " + mode.getDescription()+"."));
			responses.add(new InteractionResponse(InteractionResult.SUCCESS, "Next mode is: "+mode.getNext().getDescription()+"."));
		}
		//if the factory is on, return error message
		else
		{
			responses.add(new InteractionResponse(InteractionResult.FAILURE, "You can't change modes while the bats are flying!."));
		}
		return responses;
	}
	
	public List<InteractionResponse> getChestResponse()
	{
		updateFood();
		List<InteractionResponse> responses=new ArrayList<InteractionResponse>();
		String status=active ? "On" : "Off";
		//Name: Status with XX% health.
		int maxRepair = batRoostProperties.getMaxPopulationHealth();
		boolean maintenanceActive = maxRepair!=0;
		int health =(!maintenanceActive) ? 100 : (int) Math.round(100*(1-currentRepair/(maxRepair)));
		responses.add(new InteractionResponse(InteractionResult.SUCCESS, batRoostProperties.getName()+": "+status+" with "+String.valueOf(health)+"% colony health, "+String.valueOf(lastFoodLevel)+" food."));

		responses.add(new InteractionResponse(InteractionResult.SUCCESS, mode.getDescription()));
		if(!getInputs().isEmpty())
		{
			responses.add(new InteractionResponse(InteractionResult.SUCCESS,"Input: "+getInputs().toString()+"."));
		}
		if(!getOutputs().isEmpty())
		{
			responses.add(new InteractionResponse(InteractionResult.SUCCESS,"Output: "+getOutputs().toString()+"."));
		}
		if(!getRepairs().isEmpty()&&maintenanceActive)
		{
			responses.add(new InteractionResponse(InteractionResult.SUCCESS,"Will replenish the colony to full strength using "+getRepairs().toString()+"."));
		}
		if (mode == OperationMode.SEND_MESSAGE) {
			Location target = getTargetLocation();
			responses.add(new InteractionResponse(InteractionResult.SUCCESS,String.format("Target destination for bat-o-grams is X%+d Z%+d.", target.getBlockX(), target.getBlockZ())));
		}
		return responses;
	}
		public enum OperationMode {
		REPAIR(0, "Replenish bats"),
		FEED_COLONY(1, "Feed colony"),
		SEND_MESSAGE(2, "Send bat-o-gram");
		
		private static final int MAX_ID = 3;
		private int id;
		private String description;

		private OperationMode(int id, String description) {
			this.id = id;
			this.description = description;
		}
		
		public String getDescription() {
			return description;
		}

		public static OperationMode byId(int id) {
			for (OperationMode mode : OperationMode.values()) {
				if (mode.getId() == id)
					return mode;
			}
			return null;
		}
		
		public int getId() {
			return id;
		}

		public OperationMode getNext() {
			int nextId = (getId() + 1) % MAX_ID;
			return OperationMode.byId(nextId);
		}
	}

	@Override
	protected void recipeFinished() {
		switch(mode) {
		case REPAIR:
		case SEND_MESSAGE:
			Inventory inventory = getInventory();
			Location destination = getTargetLocation();
			ListIterator<ItemStack> inventoryIterator = inventory.iterator();
			while (inventoryIterator.hasNext()) {
				ItemStack stack = inventoryIterator.next();
				if (stack == null) {
					continue;
				}
				
				ItemStack moveStack = stack.clone();
				moveStack.setAmount(1);
				
				if (stack.getAmount() > 1) {
					stack.setAmount(stack.getAmount() - 1);
					inventoryIterator.set(stack);
				} else {
					inventoryIterator.set(new ItemStack(Material.AIR, 0));
				}
				
				destination.getWorld().dropItem(destination, moveStack);
				
				break;
			}
		case FEED_COLONY:
			lastFoodLevel = lastFoodLevel + batRoostProperties.getFoodPerFeeding();
			int maxFood = batRoostProperties.getMaxFoodLevel();
			if (lastFoodLevel > maxFood) {
				lastFoodLevel = maxFood;
			}
			lastFoodUpdateTime = factoryLocation.getWorld().getFullTime();
			break;
		}
	}
	
	public Location getTargetLocation() {
		Location target = factoryLocation.clone();
		target.setY(257);
		
		for (ItemStack stack : getInventory().getContents()) {
			if (stack == null) {
				continue;
			}
			
			if (stack.getType().equals(Material.BOOK_AND_QUILL) || stack.getType().equals(Material.WRITTEN_BOOK)) {
				ItemMeta meta = stack.getItemMeta();
				if (meta instanceof BookMeta) {
					BookMeta bookData = (BookMeta) meta;
					if (bookData.getPageCount() >= 1) {
						String firstPage = bookData.getPages().get(0);
						System.err.println(firstPage);
						Matcher xMatcher = xCoord.matcher(firstPage);
						Matcher zMatcher = zCoord.matcher(firstPage);
						if (xMatcher.find() && zMatcher.find()) {
							int x = Integer.parseInt(xMatcher.group(1));
							int z = Integer.parseInt(zMatcher.group(1));
							target.setX(x);
							target.setZ(z);
							return target;
						}
					}
				}
			}
		}
		return target;
	}

	@Override
	public ItemList<NamedItemStack> getFuel() {
		return batRoostProperties.getFuel();
	}

	public int getLastFoodLevel() {
		return lastFoodLevel;
	}

	public long getLastFoodUpdateTime() {
		return lastFoodUpdateTime;
	}

	@Override
	public List<InteractionResponse> togglePower() {
		updateFood();
		if (!active) {
			if (mode == OperationMode.SEND_MESSAGE) {
				long time = this.getCenterLocation().getWorld().getFullTime();
				long phase = time % 24000;
				if (phase < 12000) {
					// Day time
					List<InteractionResponse> response=new ArrayList<InteractionResponse>();
					response.add(new InteractionResponse(InteractionResult.FAILURE,"The bats are asleep! They can only fly at night."));
					return response;
				}
				
				Location target = getTargetLocation();
				if (target.distance(factoryLocation) > batRoostProperties.getDistanceLimit()) {
					// Too far away to send
					List<InteractionResponse> response=new ArrayList<InteractionResponse>();
					response.add(new InteractionResponse(InteractionResult.FAILURE,"The destination is too far away - limit is " + batRoostProperties.getDistanceLimit() + " blocks."));
					return response;
				}
			}
		}
		return super.togglePower();
	}

	@Override
	protected void repair(int amountRepaired) {
		// Ignore the amount repaired - just set to full health
		currentRepair = 0;
	}
}
