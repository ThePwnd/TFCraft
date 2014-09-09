package com.bioxx.tfc.TileEntities;

import java.util.Random;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.MinecraftForge;

import com.bioxx.tfc.TFCItems;
import com.bioxx.tfc.Core.TFC_Core;
import com.bioxx.tfc.Food.Food;
import com.bioxx.tfc.Items.ItemMeltedMetal;
import com.bioxx.tfc.api.HeatIndex;
import com.bioxx.tfc.api.HeatRegistry;
import com.bioxx.tfc.api.TFC_ItemHeat;
import com.bioxx.tfc.api.Enums.EnumFuelMaterial;
import com.bioxx.tfc.api.Events.ItemCookEvent;
import com.bioxx.tfc.api.Interfaces.IFood;
import com.bioxx.tfc.api.TileEntities.TEFireEntity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TEFirepit extends TEFireEntity implements IInventory
{
	public ItemStack fireItemStacks[];
	public boolean hasCookingPot;

	public TEFirepit()
	{
		fuelTimeLeft = 375;
		fuelBurnTemp =  613;
		fireTemp = 350;
		maxFireTempScale = 2000;
		fireItemStacks = new ItemStack[11];
		hasCookingPot = true;
	}

	@Override
	public void closeInventory()
	{
	}

	public void combineMetals(ItemStack InputItem, ItemStack DestItem)
	{
		int D1 = 100 - InputItem.getItemDamage();
		int D2 = 100 - DestItem.getItemDamage();
		//This was causing the infinite amounts possibly
		DestItem.setItemDamage(100 - (D1 + D2));
	}

	public void CookItem()
	{
		HeatRegistry manager = HeatRegistry.getInstance();
		Random R = new Random();
		if(fireItemStacks[1] != null)
		{
			HeatIndex index = manager.findMatchingIndex(fireItemStacks[1]);
			if(index != null && TFC_ItemHeat.GetTemp(fireItemStacks[1]) > index.meltTemp)
			{
				float temp = TFC_ItemHeat.GetTemp(fireItemStacks[1]);
				ItemStack output = index.getOutput(fireItemStacks[1], R);
				ItemCookEvent eventMelt = new ItemCookEvent(fireItemStacks[1], output, this);
				MinecraftForge.EVENT_BUS.post(eventMelt);
				output = eventMelt.result;
				int damage = 0;
				ItemStack mold = null;
				if(output != null)
				{
					damage = output.getItemDamage();
					if(output.getItem() == fireItemStacks[1].getItem())
						damage = fireItemStacks[1].getItemDamage();

					//If the input is unshaped metal
					if(fireItemStacks[1].getItem() instanceof ItemMeltedMetal)
					{
						//if both output slots are empty then just lower the input item into the first output slot
						if(fireItemStacks[7] == null && fireItemStacks[8] == null)
						{
							fireItemStacks[7] = fireItemStacks[1].copy();
							fireItemStacks[1] = null;
							return;
						}
						//Otherwise if the first output has an item that doesnt match the input item then put the item in the second output slot
						else if(fireItemStacks[7] != null && fireItemStacks[7].getItem() != TFCItems.CeramicMold && 
								(fireItemStacks[7].getItem() != fireItemStacks[1].getItem() || fireItemStacks[7].getItemDamage() == 0))
						{
							if(fireItemStacks[8] == null)
							{
								fireItemStacks[8] = fireItemStacks[1].copy();
								fireItemStacks[1] = null;
								return;
							}
						}
						mold = new ItemStack(TFCItems.CeramicMold, 1);
						mold.stackSize = 1;
						mold.setItemDamage(1);
					}
				}
				//Morph the input
				fireItemStacks[1] = index.getMorph();
				if(fireItemStacks[1] != null && manager.findMatchingIndex(fireItemStacks[1]) != null)
				{
					//if the input is a new item, then apply the old temperature to it
					TFC_ItemHeat.SetTemp(fireItemStacks[1], temp);
				}

				//Check if we should combine the output with a pre-existing output
				if(output != null && output.getItem() instanceof ItemMeltedMetal)
				{
					int leftover = 0;
					boolean addLeftover = false;
					int fromSide = 0;
					if(fireItemStacks[7] != null && output != null && output.getItem() == fireItemStacks[7].getItem() && fireItemStacks[7].getItemDamage() > 0)
					{
						int amt1 = 100 - damage;//the percentage of the output
						int amt2 = 100 - fireItemStacks[7].getItemDamage();//the percentage currently in the out slot
						int amt3 = amt1 + amt2;//combined amount
						leftover = amt3 - 100;//assign the leftover so that we can add to the other slot if applicable
						if(leftover > 0)
							addLeftover = true;
						int amt4 = 100 - amt3;//convert the percent back to mc damage
						if(amt4 < 0)
							amt4 = 0;//stop the infinite glitch
						fireItemStacks[7] = output.copy();
						fireItemStacks[7].setItemDamage(amt4);

						TFC_ItemHeat.SetTemp(fireItemStacks[7], temp);

						if(fireItemStacks[1] == null && mold != null)
							fireItemStacks[1] = mold;
					}
					else if(fireItemStacks[8] != null && output != null && output.getItem() == fireItemStacks[8].getItem() && fireItemStacks[8].getItemDamage() > 0)
					{
						int amt1 = 100 - damage;//the percentage of the output
						int amt2 = 100 - fireItemStacks[8].getItemDamage();//the percentage currently in the out slot
						int amt3 = amt1 + amt2;//combined amount
						leftover = amt3 - 100;//assign the leftover so that we can add to the other slot if applicable
						if(leftover > 0)
							addLeftover = true;
						fromSide = 1;
						int amt4 = 100 - amt3;//convert the percent back to mc damage
						if(amt4 < 0)
							amt4 = 0;//stop the infinite glitch
						fireItemStacks[8] = output.copy();
						fireItemStacks[8].setItemDamage(amt4);

						TFC_ItemHeat.SetTemp(fireItemStacks[8], temp);

						if(fireItemStacks[1] == null && mold != null)
							fireItemStacks[1] = mold;
					}
					else if(output != null && fireItemStacks[7] != null && fireItemStacks[7].getItem() == TFCItems.CeramicMold)
					{
						fireItemStacks[7] = output.copy();
						fireItemStacks[7].setItemDamage(damage);

						TFC_ItemHeat.SetTemp(fireItemStacks[7], temp);
					}
					else if(output != null && fireItemStacks[8] != null && fireItemStacks[8].getItem() == TFCItems.CeramicMold)
					{
						fireItemStacks[8] = output.copy();
						fireItemStacks[8].setItemDamage(damage);

						TFC_ItemHeat.SetTemp(fireItemStacks[8], temp);
					}

					if(addLeftover)
					{
						int dest = fromSide == 1 ? 7 : 8;
						if(fireItemStacks[dest] != null && output.getItem() == fireItemStacks[dest].getItem() && fireItemStacks[dest].getItemDamage() > 0)
						{
							int amt1 = 100 - leftover;//the percentage of the output
							int amt2 = 100 - fireItemStacks[dest].getItemDamage();//the percentage currently in the out slot
							int amt3 = amt1 + amt2;//combined amount
							int amt4 = 100 - amt3;//convert the percent back to mc damage
							if(amt4 < 0)
								amt4 = 0;//stop the infinite glitch
							fireItemStacks[dest] = output.copy();
							fireItemStacks[dest].setItemDamage(amt4);

							TFC_ItemHeat.SetTemp(fireItemStacks[dest], temp);
						}
						else if(fireItemStacks[dest] != null && fireItemStacks[dest].getItem() == TFCItems.CeramicMold)
						{
							fireItemStacks[dest] = output.copy();
							fireItemStacks[dest].setItemDamage(100 - leftover);
							TFC_ItemHeat.SetTemp(fireItemStacks[dest], temp);
						}
					}
				}
				else if(output != null)
				{
					if(fireItemStacks[7] != null && 
							fireItemStacks[7].getItem() == output.getItem() && 
							fireItemStacks[7].stackSize + output.stackSize <= fireItemStacks[7].getMaxStackSize())
					{
						fireItemStacks[7].stackSize += output.stackSize; 
					}
					else if(fireItemStacks[8] != null && 
							fireItemStacks[8].getItem() == output.getItem() && 
							fireItemStacks[8].stackSize + output.stackSize <= fireItemStacks[8].getMaxStackSize())
					{
						fireItemStacks[8].stackSize += output.stackSize; 
					}
					else if(fireItemStacks[7] == null)
					{
						fireItemStacks[7] = output.copy();
					}
					else if(fireItemStacks[8] == null)
					{
						fireItemStacks[8] = output.copy();
					}
					else if((fireItemStacks[7].stackSize == fireItemStacks[7].getMaxStackSize() && fireItemStacks[8].stackSize == fireItemStacks[8].getMaxStackSize())
							|| (fireItemStacks[7].getItem() != output.getItem() && fireItemStacks[8].getItem() != output.getItem())
							|| (fireItemStacks[7].stackSize == fireItemStacks[7].getMaxStackSize() && fireItemStacks[8].getItem() != output.getItem())
							|| (fireItemStacks[7].getItem() != output.getItem() && fireItemStacks[8].stackSize == fireItemStacks[8].getMaxStackSize()))
					{
						fireItemStacks[1] = output.copy();
					}
				}
			}
		}
	}

	@Override
	public ItemStack decrStackSize(int slot, int amount)
	{
		if(fireItemStacks[slot] != null)
		{
			if(fireItemStacks[slot].stackSize <= amount)
			{
				ItemStack itemstack = fireItemStacks[slot];
				fireItemStacks[slot] = null;
				return itemstack;
			}
			ItemStack itemstack1 = fireItemStacks[slot].splitStack(amount);
			if(fireItemStacks[slot].stackSize == 0)
				fireItemStacks[slot] = null;
			return itemstack1;
		}
		else
			return null;
	}

	public void ejectContents()
	{
		float f3 = 0.05F;
		EntityItem entityitem;
		Random rand = new Random();
		float f = rand.nextFloat() * 0.8F + 0.1F;
		float f1 = rand.nextFloat() * 0.8F + 0.3F;
		float f2 = rand.nextFloat() * 0.8F + 0.1F;

		for (int i = 0; i < getSizeInventory(); i++)
		{
			if(fireItemStacks[i]!= null)
			{
				entityitem = new EntityItem(worldObj, xCoord + f, yCoord + f1, zCoord + f2, fireItemStacks[i]);
				entityitem.motionX = (float)rand.nextGaussian() * f3;
				entityitem.motionY = (float)rand.nextGaussian() * f3 + 0.2F;
				entityitem.motionZ = (float)rand.nextGaussian() * f3;
				worldObj.spawnEntityInWorld(entityitem);
				fireItemStacks[i] = null;
			}
		}
	}

	@Override
	public int getInventoryStackLimit()
	{
		return 64;
	}

	@Override
	public String getInventoryName()
	{
		return "Firepit";
	}

	public float getOutput1Temp()
	{
		return TFC_ItemHeat.GetTemp(fireItemStacks[7]);
	}

	public float getOutput2Temp()
	{
		return TFC_ItemHeat.GetTemp(fireItemStacks[8]);
	}

	@Override
	public int getSizeInventory()
	{
		return fireItemStacks.length;
	}

	@Override
	public ItemStack getStackInSlot(int slot)
	{
		return fireItemStacks[slot];
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot)
	{
		return null;
	}

	public void HandleFuelStack()
	{
		if(fireItemStacks[3] == null && fireItemStacks[0] != null)
		{
			fireItemStacks[3] = fireItemStacks[0];
			fireItemStacks[0] = null;
		}
		if(fireItemStacks[4] == null && fireItemStacks[3] != null)
		{
			fireItemStacks[4] = fireItemStacks[3];
			fireItemStacks[3] = null;
		}
		if(fireItemStacks[5] == null && fireItemStacks[4] != null)
		{
			fireItemStacks[5] = fireItemStacks[4];
			fireItemStacks[4] = null;
		}
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer entityplayer)
	{
		return false;
	}

	@Override
	public void openInventory()
	{
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack is)
	{
		fireItemStacks[slot] = is;
		if(is != null && is.stackSize > getInventoryStackLimit())
			is.stackSize = getInventoryStackLimit();
	}

	@Override
	public void careForInventorySlot(ItemStack is)
	{
		if(is != null)
		{
			float temp = TFC_ItemHeat.GetTemp(is);
			if(fuelTimeLeft > 0 && is.getItem() instanceof IFood)
			{
				float inc = Food.getCooked(is)+(fireTemp/700);
				Food.setCooked(is, inc);;
				temp = inc;
				if(Food.isCooked(is))
				{
					Food.setFuelProfile(is, EnumFuelMaterial.getFuelProfile(fuelTasteProfile));
				}
			}
			else if(fireTemp > temp)
			{
				temp += TFC_ItemHeat.getTempIncrease(is);
			}
			else
				temp -= TFC_ItemHeat.getTempDecrease(is);
			TFC_ItemHeat.SetTemp(is, temp);
		}
	}

	@Override
	public void updateEntity()
	{
		if(!worldObj.isRemote)
		{
			//Here we take care of the item that we are cooking in the fire
			careForInventorySlot(fireItemStacks[1]);
			careForInventorySlot(fireItemStacks[7]);
			careForInventorySlot(fireItemStacks[8]);

			hasCookingPot = (fireItemStacks[1] != null && fireItemStacks[1].getItem() == TFCItems.PotteryPot && 
					fireItemStacks[1].getItemDamage() == 1);

			//Now we cook the input item
			CookItem();

			//push the input fuel down the stack
			HandleFuelStack();

			if((fireTemp < 1) && (worldObj.getBlockMetadata(xCoord, yCoord, zCoord) != 0))
			{
				worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 0, 3);
				worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
			}
			else if((fireTemp >= 1) && (worldObj.getBlockMetadata(xCoord, yCoord, zCoord) != 1))
			{
				worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 1, 3);
				worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
			}

			//If the fire is still burning and has fuel
			if(fuelTimeLeft > 0 && fireTemp >= 1)
			{
				if(worldObj.getBlockMetadata(xCoord, yCoord, zCoord) != 2)
				{
					worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 2, 3);
					worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
				}
			}
			else if(fuelTimeLeft <= 0 && fireTemp >= 1 && fireItemStacks[5] != null &&
					(!worldObj.canBlockSeeTheSky(xCoord, yCoord, zCoord) || !worldObj.isRaining()))
			{
				if(fireItemStacks[5] != null)
				{
					EnumFuelMaterial m = TFC_Core.getFuelMaterial(fireItemStacks[5]);
					fuelTasteProfile = m.ordinal();
					fireItemStacks[5] = null;
					fuelTimeLeft = m.burnTimeMax;
					fuelBurnTemp = m.burnTempMax;
				}
			}

			//Calculate the fire temp
			float desiredTemp = handleTemp();

			handleTempFlux(desiredTemp);

			//Here we handle the bellows
			handleAirReduction();

			//do a last minute check to verify stack size
			if(fireItemStacks[7] != null)
			{
				if(fireItemStacks[7].stackSize <= 0)
					fireItemStacks[7].stackSize = 1;
			}

			if(fireItemStacks[8] != null)
			{
				if(fireItemStacks[8].stackSize <= 0)
					fireItemStacks[8].stackSize = 1;
			}

			if(fuelTimeLeft <= 0)
				TFC_Core.handleItemTicking(this, worldObj, xCoord, yCoord, zCoord);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound)
	{
		super.readFromNBT(nbttagcompound);
		NBTTagList nbttaglist = nbttagcompound.getTagList("Items", 10);
		fireItemStacks = new ItemStack[getSizeInventory()];
		for(int i = 0; i < nbttaglist.tagCount(); i++)
		{
			NBTTagCompound nbttagcompound1 = nbttaglist.getCompoundTagAt(i);
			byte byte0 = nbttagcompound1.getByte("Slot");
			if(byte0 >= 0 && byte0 < fireItemStacks.length)
				fireItemStacks[byte0] = ItemStack.loadItemStackFromNBT(nbttagcompound1);
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound)
	{
		super.writeToNBT(nbttagcompound);
		NBTTagList nbttaglist = new NBTTagList();
		for(int i = 0; i < fireItemStacks.length; i++)
		{
			if(fireItemStacks[i] != null)
			{
				NBTTagCompound nbttagcompound1 = new NBTTagCompound();
				nbttagcompound1.setByte("Slot", (byte)i);
				fireItemStacks[i].writeToNBT(nbttagcompound1);
				nbttaglist.appendTag(nbttagcompound1);
			}
		}
		nbttagcompound.setTag("Items", nbttaglist);
	}

	@Override
	public boolean hasCustomInventoryName()
	{
		return false;
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack is)
	{
		return false;
	}

	@SideOnly(Side.CLIENT)
	public void GenerateSmoke()
	{
		/*if(scanSmokeLayer)
		{
			int y = 0;
			int topY = yCoord;
			boolean checkArray[][][] = new boolean[25][13][25];
			while(worldObj.getBlock(xCoord, topY + 1, zCoord) == TFCBlocks.LogPile)
			{
				//scanLogs(xCoord, topY, zCoord, checkArray, (byte)12, (byte)y, (byte)12, false, true);
				++topY;
				++y;
			}
			//scanLogs(xCoord, topY, zCoord, checkArray, (byte)12, (byte)y, (byte)12, false, true);
			smokeMap.put(smokeMap.size(), new int[] {xCoord, topY + 2, zCoord});
			scanSmokeLayer = false;
		}

		if(smokeMap.size() > 0)
		{
			int[] sb = smokeMap.get(worldObj.rand.nextInt(smokeMap.size()));
			float f = sb[0] + 0.5F;
			float f1 = sb[1] + 0.1F + worldObj.rand.nextFloat() * 6F / 16F;
			float f2 = sb[2] + 0.5F;
			float f4 = worldObj.rand.nextFloat() * 0.6F;
			float f5 = worldObj.rand.nextFloat() * -0.6F;
			worldObj.spawnParticle("smoke", f+f4 - 0.3F, f1, f2 + f5 + 0.3F, 0.0D, 0.0D, 0.0D);
			worldObj.spawnParticle("smoke", f+f4 - 0.2F, f1, f2 + f5 + 0.4F, 0.0D, 0.0D, 0.0D);
			worldObj.spawnParticle("smoke", f+f4 - 0.1F, f1, f2 + f5 + 0.1F, 0.0D, 0.0D, 0.0D);
			if(worldObj.rand.nextInt(10) == 0) worldObj.spawnParticle("largesmoke", f+f4 - 0.2F, f1, f2 + f5 + 0.2F, 0.0D, 0.0D, 0.0D);
		}*/
	}
}
