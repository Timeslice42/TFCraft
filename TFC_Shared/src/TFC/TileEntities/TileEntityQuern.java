package TFC.TileEntities;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.packet.Packet;
import TFC.TFCItems;
import TFC.TerraFirmaCraft;
import TFC.Core.TFC_ItemHeat;
import TFC.Entities.Mobs.EntityCowTFC;
import TFC.Handlers.PacketHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityQuern extends NetworkTileEntity implements IInventory
{
	public ItemStack[] storage = new ItemStack[3];
	public int rotation = 0;
	public boolean shouldRotate = false;
	public int rotatetimer = 0;
	public boolean hasQuern = false;

	@Override
	public void updateEntity()
	{
		if(!worldObj.isRemote)
		{
			TFC_ItemHeat.HandleContainerHeat(this.worldObj,storage, xCoord,yCoord,zCoord);
		}
		if(shouldRotate)
		{
			rotatetimer++;
			if(rotatetimer == 20)
			{
				if(rotation == 3)
				{
					rotation = 0;
					shouldRotate = false;
					worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
					if(!worldObj.isRemote)
					{
						if(storage[0] != null)
						{
							if(storage[0].getItem() == TFCItems.WheatGrain && (storage[1] == null || storage[1].getItem() == TFCItems.WheatGround))
							{
								if(storage[0].stackSize == 1)
									storage[0] = null;
								else
									storage[0].stackSize--;
								if(storage[1] == null)
									storage[1] = new ItemStack(TFCItems.WheatGround, 1);
								else if(storage[1].stackSize < 64)
									storage[1].stackSize++;
							}
							else if(storage[0].getItem() == TFCItems.RyeGrain && (storage[1] == null || storage[1].getItem() == TFCItems.RyeGround))
							{
								if(storage[0].stackSize == 1)
									storage[0] = null;
								else
									storage[0].stackSize--;
								if(storage[1] == null)
									storage[1] = new ItemStack(TFCItems.RyeGround, 1);
								else if(storage[1].stackSize < 64)
									storage[1].stackSize++;
							}
							else if(storage[0].getItem() == TFCItems.OatGrain && (storage[1] == null || storage[1].getItem() == TFCItems.OatGround))
							{
								if(storage[0].stackSize == 1)
									storage[0] = null;
								else
									storage[0].stackSize--;
								if(storage[1] == null)
									storage[1] = new ItemStack(TFCItems.OatGround, 1);
								else if(storage[1].stackSize < 64)
									storage[1].stackSize++;
							}
							else if(storage[0].getItem() == TFCItems.BarleyGrain && (storage[1] == null || storage[1].getItem() == TFCItems.BarleyGround))
							{
								if(storage[0].stackSize == 1)
									storage[0] = null;
								else
									storage[0].stackSize--;
								if(storage[1] == null)
									storage[1] = new ItemStack(TFCItems.BarleyGround, 1);
								else if(storage[1].stackSize < 64)
									storage[1].stackSize++;
							}
							else if(storage[0].getItem() == TFCItems.RiceGrain && (storage[1] == null || storage[1].getItem() == TFCItems.RiceGround))
							{
								if(storage[0].stackSize == 1)
									storage[0] = null;
								else
									storage[0].stackSize--;

								if(storage[1] == null)
									storage[1] = new ItemStack(TFCItems.RiceGround, 1);
								else if(storage[1].stackSize < 64)
									storage[1].stackSize++;
							}
							else if(storage[0].getItem() == TFCItems.MaizeEar && (storage[1] == null || storage[1].getItem() == TFCItems.CornmealGround))
							{
								if(storage[0].stackSize == 1)
									storage[0] = null;
								else
									storage[0].stackSize--;
								if(storage[1] == null)
									storage[1] = new ItemStack(TFCItems.CornmealGround, 1);
								else if(storage[1].stackSize < 64)
									storage[1].stackSize++;
							}
						}
						
						if(storage[2] != null)
							storage[2].damageItem(1, new EntityCowTFC(worldObj));
					}
				}
				else 
				{
					rotation++;
					worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
				}

				rotatetimer = 0;
			}
		}

	}


	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound)
	{
		super.readFromNBT(nbttagcompound);

		NBTTagList nbttaglist = nbttagcompound.getTagList("Items");
		storage = new ItemStack[getSizeInventory()];
		for(int i = 0; i < nbttaglist.tagCount(); i++)
		{
			NBTTagCompound nbttagcompound1 = (NBTTagCompound)nbttaglist.tagAt(i);
			byte byte0 = nbttagcompound1.getByte("Slot");
			if(byte0 >= 0 && byte0 < storage.length)
			{
				storage[byte0] = ItemStack.loadItemStackFromNBT(nbttagcompound1);
			}
		}
		hasQuern = nbttagcompound.getBoolean("hasQuern");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound)
	{
		super.writeToNBT(nbttagcompound);
		NBTTagList nbttaglist = new NBTTagList();
		for(int i = 0; i < storage.length; i++)
		{
			if(storage[i] != null)
			{
				NBTTagCompound nbttagcompound1 = new NBTTagCompound();
				nbttagcompound1.setByte("Slot", (byte)i);
				storage[i].writeToNBT(nbttagcompound1);
				nbttaglist.appendTag(nbttagcompound1);
			}
		}
		nbttagcompound.setTag("Items", nbttaglist);

		nbttagcompound.setBoolean("hasQuern", hasQuern);
	}

	@Override
	public ItemStack decrStackSize(int i, int j) {
		if(storage[i] != null)
		{
			if(storage[i].stackSize <= j)
			{
				ItemStack itemstack = storage[i];
				storage[i] = null;
				return itemstack;
			}
			ItemStack itemstack1 = storage[i].splitStack(j);
			if(storage[i].stackSize == 0)
			{
				storage[i] = null;
			}
			return itemstack1;
		} else
		{
			return null;
		}

	}

	public void ejectContents()
	{
		float f3 = 0.05F;
		EntityItem entityitem;
		Random rand = new Random();
		float f = rand.nextFloat() * 0.8F + 0.1F;
		float f1 = rand.nextFloat() * 2.0F + 0.4F;
		float f2 = rand.nextFloat() * 0.8F + 0.1F;

		for (int i = 0; i < getSizeInventory(); i++)
		{
			if(storage[i]!= null)
			{
				entityitem = new EntityItem(worldObj, xCoord + f, yCoord + f1, zCoord + f2, 
						storage[i]);
				entityitem.motionX = (float)rand.nextGaussian() * f3;
				entityitem.motionY = (float)rand.nextGaussian() * f3 + 0.2F;
				entityitem.motionZ = (float)rand.nextGaussian() * f3;
				worldObj.spawnEntityInWorld(entityitem);
			}
		}
	}

	public void ejectItem(int index)
	{
		float f3 = 0.05F;
		EntityItem entityitem;
		Random rand = new Random();
		float f = rand.nextFloat() * 0.8F + 0.1F;
		float f1 = rand.nextFloat() * 2.0F + 0.4F;
		float f2 = rand.nextFloat() * 0.8F + 0.1F;

		if(storage[index]!= null)
		{
			entityitem = new EntityItem(worldObj, xCoord + f, yCoord + f1, zCoord + f2, 
					storage[index]);
			entityitem.motionX = (float)rand.nextGaussian() * f3;
			entityitem.motionY = (float)rand.nextGaussian() * f3 + 0.05F;
			entityitem.motionZ = (float)rand.nextGaussian() * f3;
			worldObj.spawnEntityInWorld(entityitem);
		}
	}

	@Override
	public int getSizeInventory()
	{
		return storage.length;
	}

	@Override
	public ItemStack getStackInSlot(int i)
	{
		return storage[i];
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemstack) 
	{
		storage[i] = itemstack;
		if(itemstack != null && itemstack.stackSize > getInventoryStackLimit())
		{
			itemstack.stackSize = getInventoryStackLimit();
		}
		TerraFirmaCraft.proxy.sendCustomPacket(createUpdatePacket());
	}

	@Override
	public String getInvName() {
		// TODO Auto-generated method stub
		return "Quern";
	}

	@Override
	public int getInventoryStackLimit() {
		// TODO Auto-generated method stub
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer var1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void openChest() {
		// TODO Auto-generated method stub

	}

	@Override
	public void closeChest() {
		//TerraFirmaCraft.proxy.sendCustomPacket(createUpdatePacket());
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int var1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createInitPacket(DataOutputStream outStream) throws IOException 
	{
		outStream.writeBoolean(storage[2] != null);
	}


	@Override
	public void handleDataPacket(DataInputStream inStream) throws IOException {
		this.hasQuern = inStream.readBoolean();
		worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
	}

	public Packet createUpdatePacket()
	{
		ByteArrayOutputStream bos=new ByteArrayOutputStream(140);
		DataOutputStream dos=new DataOutputStream(bos);

		try {
			dos.writeByte(PacketHandler.Packet_Data_Block_Client);
			dos.writeInt(xCoord);
			dos.writeInt(yCoord);
			dos.writeInt(zCoord);

			dos.writeBoolean(storage[2] != null);
		} catch (IOException e) {
		}

		return this.setupCustomPacketData(bos.toByteArray(), bos.size());
	}


	@Override
	public void handleDataPacketServer(DataInputStream inStream)
			throws IOException {
		// TODO Auto-generated method stub

	}


	@Override
	@SideOnly(Side.CLIENT)
	public void handleInitPacket(DataInputStream inStream) throws IOException {

		this.hasQuern = inStream.readBoolean();
		worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);

	}


	@Override
	public boolean isInvNameLocalized() 
	{
		return false;
	}

	@Override
	public boolean isStackValidForSlot(int i, ItemStack itemstack) 
	{
		return false;
	}
}
