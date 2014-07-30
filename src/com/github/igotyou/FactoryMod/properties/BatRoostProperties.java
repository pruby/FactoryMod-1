package com.github.igotyou.FactoryMod.properties;

import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.spigotmc.TicksPerSecondCommand;

import com.github.igotyou.FactoryMod.FactoryModPlugin;
import com.github.igotyou.FactoryMod.recipes.ProductionRecipe;
import com.github.igotyou.FactoryMod.utility.ItemList;
import com.github.igotyou.FactoryMod.utility.NamedItemStack;

public class BatRoostProperties {
	private String name;
	private ItemList<NamedItemStack> fuel;
	private ItemList<NamedItemStack> constructionMaterials;
	private ItemList<NamedItemStack> replenishColonyMaterials;
	private ItemList<NamedItemStack> feedingMaterials;
	
	private int maxFoodLevel;
	private int maxPopulationHealth;
	private int ticksPerFood; // Interval in ticks between feeding times
	private int starvationRate; // Rate at which an unfed colony starves per feeding time.
	private int healingRate; // Rate at which a fed, full strength colony heals, per feeding time.
	private int foodPerFeeding; // How much the food count increases from a single feeding.
	private double energyTime;
	private Random random;
	
	public BatRoostProperties(
		String name,
		ItemList<NamedItemStack> fuel,
		ItemList<NamedItemStack> constructionMaterials,
		ItemList<NamedItemStack> replenishColonyMaterials,
		ItemList<NamedItemStack> feedingMaterials,
		double energyTime,
		int maxFoodLevel,
		int maxPopulationHealth,
		int ticksPerFood,
		int foodPerFeeding,
		int starvationRate,
		int healingRate
		)
	{
		this.name = name;
		this.fuel = fuel;
		this.constructionMaterials = constructionMaterials;
		this.replenishColonyMaterials = replenishColonyMaterials;
		this.feedingMaterials = feedingMaterials;
		this.energyTime = energyTime;
		this.maxFoodLevel = maxFoodLevel;
		this.maxPopulationHealth = maxPopulationHealth;
		this.ticksPerFood = ticksPerFood;
		this.starvationRate = starvationRate;
		this.healingRate = healingRate;
		this.foodPerFeeding = foodPerFeeding;
		this.random = new Random();
	}
	
	public static BatRoostProperties fromConfig(FactoryModPlugin plugin, ConfigurationSection configBatRoost) {
		ItemList<NamedItemStack> ppFuel=plugin.getItems(configBatRoost.getConfigurationSection("fuel"));
		ConfigurationSection costs = configBatRoost.getConfigurationSection("costs");
		ItemList<NamedItemStack> ppConstructionCost=plugin.getItems(costs.getConfigurationSection("construction"));
		ItemList<NamedItemStack> replenishColonyMaterials=plugin.getItems(costs.getConfigurationSection("replenish_colony"));
		ItemList<NamedItemStack> feedingMaterials=plugin.getItems(costs.getConfigurationSection("feed_colony"));
		String ppName = configBatRoost.getString("name", "Bat Roost");
		int maxFoodLevel = configBatRoost.getInt("max_food_level",72);
		int maxPopulationHealth = configBatRoost.getInt("max_population_health",1000);
		int ticksPerFood = configBatRoost.getInt("ticks_per_food",72000);
		int foodPerFeeding = configBatRoost.getInt("food_per_feeding",24);
		int starvationRate = configBatRoost.getInt("starvation_rate",40);
		int healingRate = configBatRoost.getInt("healing_rate",10);
		double energyTime = configBatRoost.getDouble("energy_time",5);
		return new BatRoostProperties(ppName, ppFuel, ppConstructionCost, replenishColonyMaterials,
				feedingMaterials, energyTime, maxFoodLevel, maxPopulationHealth,
				ticksPerFood, foodPerFeeding, starvationRate, healingRate);
	}
	
	public boolean testHeal(int health) {
		return(random.nextInt(maxPopulationHealth) < health);
	}

	public String getName() {
		return name;
	}

	public ItemList<NamedItemStack> getFuel() {
		return fuel;
	}

	public ItemList<NamedItemStack> getConstructionMaterials() {
		return constructionMaterials;
	}

	public int getMaxFoodLevel() {
		return maxFoodLevel;
	}

	public int getMaxPopulationHealth() {
		return maxPopulationHealth;
	}

	public int getTicksPerFood() {
		return ticksPerFood;
	}

	public double getEnergyTime() {
		return energyTime;
	}

	public ItemList<NamedItemStack> getReplenishColonyMaterials() {
		return replenishColonyMaterials;
	}

	public ItemList<NamedItemStack> getFeedingMaterials() {
		return feedingMaterials;
	}

	public int getFoodPerFeeding() {
		return foodPerFeeding;
	}

	public int getHealingRate() {
		return healingRate;
	}

	public int getStarvationRate() {
		return starvationRate;
	}}
