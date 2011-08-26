package com.gmail.zariust.bukkit.otherblocks.subject;

import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Vehicle;

import com.gmail.zariust.bukkit.common.CommonEntity;
import com.gmail.zariust.bukkit.common.MaterialGroup;
import com.gmail.zariust.bukkit.otherblocks.OtherBlocks;
import com.gmail.zariust.bukkit.otherblocks.data.ContainerData;
import com.gmail.zariust.bukkit.otherblocks.data.NoteData;
import com.gmail.zariust.bukkit.otherblocks.data.RecordData;
import com.gmail.zariust.bukkit.otherblocks.data.SimpleData;
import com.gmail.zariust.bukkit.otherblocks.data.Data;
import com.gmail.zariust.bukkit.otherblocks.data.SpawnerData;
import com.gmail.zariust.bukkit.otherblocks.data.VehicleData;

public class BlockTarget implements Target {
	private Material id;
	private Data data;
	private Block bl;

	public BlockTarget() {
		this(null, null);
	}

	public BlockTarget(Material block) {
		this(block, null);  // note: leave as null for "wildcard" to match block with any data
	}

	public BlockTarget(Material block, byte d) {
		this(block, new SimpleData(d));
	}

	public BlockTarget(Material block, int d) {
		this(block, (byte)d);
	}

	public BlockTarget(Block block) {
		this(block.getType(), getData(block));
		bl = block;
	}

	public BlockTarget(Painting painting) {
		// TODO: Also fetch what painting it is (no API for this yet)
		this(Material.PAINTING, 0);
	}

	public BlockTarget(Vehicle vehicle) {
		this(CommonEntity.getVehicleType(vehicle), new VehicleData(vehicle));
	}
	
	public BlockTarget(Material mat, Data d) { // The Rome constructor
		id = mat;
		data = d;
	}

	private static Data getData(Block block) {
		switch(block.getType()) {
		case FURNACE:
		case BURNING_FURNACE:
		case DISPENSER:
		case CHEST:
			return new ContainerData(block.getState());
		case MOB_SPAWNER:
			return new SpawnerData(block.getState());
		case NOTE_BLOCK:
			return new NoteData(block.getState());
		case JUKEBOX:
			return new RecordData(block.getState());
		default:
			return new SimpleData(block.getData());
		}
	}

	public Material getMaterial() {
		return id;
	}
	
	public int getId() {
		return id.getId();
	}
	
	public Data getData() {
		return data;
	}
	
	@Override
	public boolean equals(Object other) {
		if(!(other instanceof BlockTarget)) return false;
		BlockTarget targ = (BlockTarget) other;
		return id == targ.id && data.equals(targ.data);
	}
	
	@Override
	public int hashCode() {
		return (data.getData() << 16) | id.getId();
	}

	@Override
	public boolean overrideOn100Percent() {
		return true;
	}

	@Override
	public ItemCategory getType() {
		return ItemCategory.BLOCK;
	}

	@Override
	public boolean matches(Subject block) {
		if(!(block instanceof BlockTarget)) return false;
		BlockTarget targ = (BlockTarget) block;
		
		Boolean match = false;
		if (id == targ.id) match = true;
		if (data == null) {
			match = true;
		} else {
			match = data.matches(targ.data);
		}
		return match;
	}

	public static Target parse(String name, String state) {
		name = name.toUpperCase();
		state = state.toUpperCase();
		Material mat = null;
		try {
			int id = Integer.parseInt(name);
			// TODO: Need a way to accept non-standard items, but Material is an enum...
			// Waiting on change to Bukkit API.
			mat = Material.getMaterial(id);
		} catch(NumberFormatException x) {
			mat = Material.getMaterial(name);
			if(mat == null) return null;
			if(!mat.isBlock()) {
				// Only a very select few non-blocks are permitted as a target
				if(mat != Material.PAINTING && mat != Material.BOAT && mat != Material.MINECART &&
						mat != Material.POWERED_MINECART && mat != Material.STORAGE_MINECART)
					return null;
			}
		}
		try {
			int val = Integer.parseInt(state);
			return new BlockTarget(mat, val);
		} catch(NumberFormatException e) {}
		if(mat == null) return null;
		Data data = null;
		try {
			data = SimpleData.parse(mat, state);
		} catch(IllegalArgumentException e) {
			OtherBlocks.logWarning(e.getMessage());
			return null;
		}
		if(data != null) return new BlockTarget(mat, data);
		return new BlockTarget(mat);
	}
	
	@Override
	public String toString() {
		if(id == null) return "ANY_BLOCK";
		if(data == null) return id.toString();
		return id + "@" + data.get(id);
	}

	@Override
	public List<Target> canMatch() {
		if(id == null) return new BlocksTarget(MaterialGroup.ANY_BLOCK).canMatch();
		return Collections.singletonList((Target) this);
	}

	@Override
	public String getKey() {
		return id.toString();
	}

	public void setTo(BlockTarget replacement) {
		bl.setType(replacement.getMaterial());
		BlockState state = bl.getState();
		if (replacement.data != null) replacement.data.setOn(state);
		state.update(true);
	}

	@Override
	public Location getLocation() {
		if(bl != null) return bl.getLocation();
		return null;
	}
}
