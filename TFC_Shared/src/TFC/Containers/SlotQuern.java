package TFC.Containers;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import TFC.TFCItems;

public class SlotQuern extends Slot

{
	public SlotQuern(IInventory iinventory, int i, int j, int k)
	{
		super(iinventory, i, j, k);

	}

	@Override
	public boolean isItemValid(ItemStack itemstack)
	{    	
		if(itemstack.getItem() == TFCItems.Quern)
		{
			return true;
		}
		
		return false;
	}
	
	@Override
	public int getSlotStackLimit()
    {
	    return 1;
    }
}
