package com.onepiecearsenal.fruit;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The animal a weapon becomes when it eats a Zoan fruit, picked from the fruit's name: every Inu Inu fruit makes a
 * dog, every Ryu Ryu fruit a dinosaur. What each animal's bites do is in FruitWeapons.bite.
 */
public enum Beast {
	//        animal        signature move            what its bites do
	DOG("Dog", "Fang Rush", "Bites slow the enemy down."),
	CAT("Big Cat", "Pounce", "Bites hit half again as hard."),
	DINOSAUR("Dinosaur", "Ancient Jaws", "Bites throw the enemy back."),
	BIRD("Bird", "Talon Dive", "Bites throw the enemy into the air."),
	ELEPHANT("Elephant", "Trunk Sweep", "Bites knock the enemy far back, and you're tougher while it's awake."),
	OX("Ox", "Horn Charge", "Bites knock the enemy back."),
	SNAKE("Snake", "Venom Fang", "Bites poison."),
	DRAGON("Dragon", "Dragon Breath", "Bites set the enemy on fire."),
	HUMAN("Human", "Living Blade", "It fights beside you and weakens what it hits."),
	MOLE("Mole", "Burrow Strike", "Bites blind the enemy."),
	HORSE("Horse", "Stampede", "Bites knock back, and you run faster while it's awake."),
	AXOLOTL("Axolotl", "Regrowth", "Every bite heals you a little."),
	SPIDER("Spider", "Web Snare", "Bites wrap the enemy in webs and slow them a lot."),
	SQUIRREL("Squirrel", "Thunder Hammer", "Bites call down lightning."),
	BEAST("Beast", "Feral Strike", "It bites on its own.");

	private static final Map<String, Beast> BY_FRUIT = new LinkedHashMap<>();

	static {
		BY_FRUIT.put("inu_inu", DOG);
		BY_FRUIT.put("neko_neko", CAT);
		BY_FRUIT.put("ryu_ryu", DINOSAUR);
		BY_FRUIT.put("tori_tori", BIRD);
		BY_FRUIT.put("zou_zou", ELEPHANT);
		BY_FRUIT.put("ushi_ushi", OX);
		BY_FRUIT.put("hebi_hebi", SNAKE);
		BY_FRUIT.put("uo_uo", DRAGON);
		BY_FRUIT.put("hito_hito", HUMAN);
		BY_FRUIT.put("mogu_mogu", MOLE);
		BY_FRUIT.put("uma_uma", HORSE);
		BY_FRUIT.put("sara_sara", AXOLOTL);
		BY_FRUIT.put("kumo_kumo", SPIDER);
		BY_FRUIT.put("risu_risu", SQUIRREL);
	}

	public final String animal;
	public final String signature;
	public final String description;

	Beast(String animal, String signature, String description) {
		this.animal = animal;
		this.signature = signature;
		this.description = description;
	}

	/** The animal of a Zoan fruit, like DOG for "inu_inu_no_mi_wolf". BEAST for a Zoan this list doesn't know yet. */
	public static Beast of(String fruitId) {
		for (Map.Entry<String, Beast> entry : BY_FRUIT.entrySet()) {
			if (fruitId.startsWith(entry.getKey() + "_no_mi")) {
				return entry.getValue();
			}
		}
		return BEAST;
	}
}
