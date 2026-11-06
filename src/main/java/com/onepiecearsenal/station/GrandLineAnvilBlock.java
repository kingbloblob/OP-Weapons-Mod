package com.onepiecearsenal.station;

/**
 * The Grand Line Anvil: a Shipwright's Anvil reinforced with Netherite or Wapometal, heavy enough to work the best
 * materials (see HighTier). It does everything the Shipwright's Anvil does, and also puts together, mends and
 * upgrades gear with high-tier parts.
 */
public class GrandLineAnvilBlock extends ShipwrightsAnvilBlock {
	public GrandLineAnvilBlock(Properties properties) {
		super(properties);
	}

	@Override
	public boolean grand() {
		return true;
	}
}
