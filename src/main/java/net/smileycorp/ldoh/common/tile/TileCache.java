package net.smileycorp.ldoh.common.tile;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.smileycorp.atlas.api.util.RecipeUtils;

public class TileCache extends TileEntity implements IInventory {

	protected ItemStack item = ItemStack.EMPTY;
	protected int count = 0;
	protected String customName;
	protected final IItemHandler inventory = new InvWrapper(this);

	@Override
	public String getName() {
		return hasCustomName() ? customName : "container.hundreddayz.cache";
	}

	@Override
	public boolean hasCustomName() {
		return customName != null && !customName.isEmpty();
	}

	@Override
	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
		return (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) ? (T) inventory
				: super.getCapability(capability, facing);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
				|| super.hasCapability(capability, facing);
	}

	@Override
	public int getSizeInventory() {
		return 64;
	}

	@Override
	public boolean isEmpty() {
		return item.isEmpty() || count == 0;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		if (count == 0 || item.isEmpty()) return ItemStack.EMPTY;
		ItemStack stack = item.copy();
		int size = item.getMaxStackSize();
		if (count < size) stack.setCount(count);
		else stack.setCount(size);
		return stack;
	}

	@Override
	public ItemStack decrStackSize(int slot, int count) {
		if (this.count == 0 || item.isEmpty()) return ItemStack.EMPTY;
		ItemStack stack = item.copy();
		int size = item.getMaxStackSize();
		int total = this.count - count;
		if (total < 0) {
			stack.setCount(count + total);
			item = ItemStack.EMPTY;
			this.count = 0;
			return stack;
		}
		stack.setCount(count);
		this.count = total;
		return stack;
	}

	@Override
	public ItemStack removeStackFromSlot(int slot) {
		ItemStack stack = item.copy();
		if (count > 64) {
			count -= 64;
			stack.setCount(64);
		} else {
			stack = ItemStack.EMPTY;
		}
		return stack;
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) {
		if (item.isEmpty()) {
			count = stack.getCount();
			item = stack.copy();
			item.setCount(1);
		} else if (RecipeUtils.compareItemStacks(stack, item, true)) {
			int max = getMaxCount();
			int total = count + stack.getCount();
			if (total <= getMaxCount()) count = total;
			else count = max;
		}
	}

	private int getMaxCount() {
		if (!item.isEmpty()) { return item.getMaxStackSize() * 64; }
		return 4096;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer player) {
		return true;
	}

	@Override
	public void openInventory(EntityPlayer player) {}

	@Override
	public void closeInventory(EntityPlayer player) {}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if (isEmpty()) return true;
		if (!RecipeUtils.compareItemStacks(stack, item, true)) return false;
		int total = count + stack.getCount();
		return total <= getMaxCount();
	}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {}

	@Override
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clear() {
		item = ItemStack.EMPTY;
		count = 0;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		if (compound.hasKey("CustomName", 8)) {
			this.customName = compound.getString("CustomName");
		}
		if (compound.hasKey("items")) {
			NBTTagCompound items = compound.getCompoundTag("items");
			count = items.getInteger("Count");
			items.setByte("Count", (byte)1);
			item = new ItemStack(items);
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		if (this.hasCustomName()){
			compound.setString("CustomName", this.customName);
		}
		if (!this.isEmpty()) {
			NBTTagCompound items = item.serializeNBT();
			items.setInteger("Count", count);
			compound.setTag("items", items);
		}
		return compound;
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Override
	public void handleUpdateTag(NBTTagCompound compound) {
		readFromNBT(compound);
	}

}
