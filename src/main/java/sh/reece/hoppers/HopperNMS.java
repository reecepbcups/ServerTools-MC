package sh.reece.hoppers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.World;
import org.bukkit.block.Block;

import sh.reece.utiltools.Util;

/**
 * Reflection bridge to pin/reset a hopper's vanilla transfer cooldown so Paper never
 * runs the expensive part of {@code HopperBlockEntity.tick} (the neighbour scan + the
 * AABB item search). Paper 1.20.5+ runs Mojang-mapped at runtime, so we bind by real
 * names ({@code cooldownTime}, {@code getBlockEntity}) - no obfuscation remapping.
 *
 * Fail-safe by design: if any lookup fails, {@link #isBound()} stays false and the
 * optimizer leaves vanilla hoppers alone. {@link #pin} read-back verifies every write,
 * so a wrong field binding manages zero hoppers instead of duping items.
 */
final class HopperNMS {

	private Constructor<?> blockPosCtor;   // net.minecraft.core.BlockPos(int,int,int)
	private Method getHandle;              // CraftWorld#getHandle -> ServerLevel
	private Method getBlockEntity;         // Level#getBlockEntity(BlockPos) -> BlockEntity
	private Field cooldownField;           // HopperBlockEntity#cooldownTime (int), resolved lazily
	private boolean bound;

	boolean bind() {
		try {
			Class<?> blockPos = Class.forName("net.minecraft.core.BlockPos");
			blockPosCtor = blockPos.getConstructor(int.class, int.class, int.class);
			bound = true;
		} catch (Throwable t) {
			Util.log("&c[Hoppers] NMS bind failed: " + t + " - optimizer disabled.");
			bound = false;
		}
		return bound;
	}

	boolean isBound() {
		return bound;
	}

	/**
	 * Set the live hopper's transfer cooldown to {@code value} and confirm via read-back.
	 * Returns false (do not manage this hopper) if the block entity isn't loaded or the
	 * write can't be verified - vanilla stays in charge, so no double transfers.
	 */
	boolean pin(Block block, int value) {
		if (!bound) {
			return false;
		}
		try {
			Object be = liveBlockEntity(block);
			if (be == null) {
				return false;
			}
			Field f = cooldownField(be);
			if (f == null) {
				return false;
			}
			f.setInt(be, value);
			return f.getInt(be) == value;
		} catch (Throwable t) {
			return false;
		}
	}

	/** Reset cooldown to 0 so vanilla immediately resumes ticking this hopper. */
	void reset(Block block) {
		pin(block, 0);
	}

	private Object liveBlockEntity(Block block) throws ReflectiveOperationException {
		Object nmsWorld = handleOf(block.getWorld());
		if (nmsWorld == null) {
			return null;
		}
		if (getBlockEntity == null) {
			getBlockEntity = nmsWorld.getClass().getMethod("getBlockEntity", blockPosCtor.getDeclaringClass());
		}
		Object pos = blockPosCtor.newInstance(block.getX(), block.getY(), block.getZ());
		return getBlockEntity.invoke(nmsWorld, pos);
	}

	private Object handleOf(World world) throws ReflectiveOperationException {
		if (getHandle == null) {
			getHandle = world.getClass().getMethod("getHandle");
		}
		return getHandle.invoke(world);
	}

	/** Walk the block entity's class hierarchy for the int cooldown field, by name. */
	private Field cooldownField(Object be) {
		if (cooldownField != null) {
			return cooldownField;
		}
		for (String name : new String[] { "cooldownTime", "transferCooldown" }) {
			for (Class<?> c = be.getClass(); c != null; c = c.getSuperclass()) {
				try {
					Field f = c.getDeclaredField(name);
					if (f.getType() == int.class) {
						f.setAccessible(true);
						cooldownField = f;
						return f;
					}
				} catch (NoSuchFieldException ignored) {
				}
			}
		}
		return null;
	}
}
