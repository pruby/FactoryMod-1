package com.github.igotyou.FactoryMod.Factorys;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;

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
		return super.togglePower();
	}

	@Override
	protected void repair(int amountRepaired) {
		// Ignore the amount repaired - just set to full health
		currentRepair = 0;
	}
}
