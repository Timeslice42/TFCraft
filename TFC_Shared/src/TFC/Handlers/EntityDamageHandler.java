package TFC.Handlers;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentThorns;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IEntityMultiPart;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import TFC.API.ICausesDamage;
import TFC.API.Enums.EnumDamageType;
import TFC.API.Events.EntityArmorCalcEvent;
import TFC.Items.ItemTFCArmor;

public class EntityDamageHandler
{
	@ForgeSubscribe
	public void onEntityHurt(LivingHurtEvent event) 
	{
		EntityLiving entity = event.entityLiving;


		if(event.source == DamageSource.onFire)
		{
			event.ammount = 50;
		}
		else if(event.source == DamageSource.fall)
		{
			event.ammount *= 80;
		}
		else if(event.source == DamageSource.drown)
		{
			event.ammount = 50;
		}
		else if(event.source == DamageSource.lava)
		{
			event.ammount = 100;
		}
		else if(event.source == DamageSource.starve)
		{
			event.ammount *= 10;
		}
		else if(event.source == DamageSource.inWall)
		{
			event.ammount = 100;
		}
		else if(event.source.isExplosion())
		{
			event.ammount *= 30;
		}
		else if(event.source.damageType == "player" || event.source.damageType == "mob" || event.source.damageType == "arrow")
		{
			applyArmorCalculations(entity, event.source, event.ammount);
			event.ammount = 0;
		}
	}

	protected int applyArmorCalculations(EntityLiving entity, DamageSource source, int originalDamage)
	{
		ItemStack[] armor = entity.getLastActiveItems();
		int pierceRating = 0;
		int slashRating = 0;
		int crushRating = 0;
		
		EntityArmorCalcEvent eventPre = new EntityArmorCalcEvent(entity, originalDamage, EntityArmorCalcEvent.EventType.PRE);
		MinecraftForge.EVENT_BUS.post(eventPre);
		int damage = eventPre.incomingDamage;
		
		if (!source.isUnblockable() && armor != null)
		{
			//1. Get Random Hit Location
			int location = getRandomSlot(entity.getRNG());
			
			//2. Get Armor Rating for armor in hit Location
			if(armor[location] != null && armor[location].getItem() instanceof ItemTFCArmor)
			{
				pierceRating = ((ItemTFCArmor)armor[location].getItem()).ArmorType.getPiercingAR();
				slashRating = ((ItemTFCArmor)armor[location].getItem()).ArmorType.getSlashingAR();
				crushRating = ((ItemTFCArmor)armor[location].getItem()).ArmorType.getCrushingAR();
				
				//3. Convert the armor rating to % damage reduction
				float pierceMult = getDamageReduction(pierceRating);
				float slashMult = getDamageReduction(slashRating);
				float crushMult = getDamageReduction(crushRating);
				
				//4. Reduce incoming damage
				EnumDamageType damageType = EnumDamageType.GENERIC;
				//4.1 Determine the source of the damage and get the appropriate Damage Type
				if(source.getSourceOfDamage() instanceof EntityPlayer)
				{
					EntityPlayer player = (EntityPlayer)source.getSourceOfDamage();
					if(player.getCurrentEquippedItem() != null && player.getCurrentEquippedItem().getItem() instanceof ICausesDamage)
					{
						damageType = ((ICausesDamage)player.getCurrentEquippedItem().getItem()).GetDamageType();
					}
				}
				else if(source.getSourceOfDamage() instanceof ICausesDamage)
				{
					damageType = ((ICausesDamage)source.getSourceOfDamage()).GetDamageType();
				}
				//4.2 Reduce the damage based upon the incoming Damage Type
				if(damageType == EnumDamageType.PIERCING)
				{
					damage *= pierceMult;
				}
				else if(damageType == EnumDamageType.SLASHING)
				{
					damage *= slashMult;
				}
				else if(damageType == EnumDamageType.CRUSHING)
				{
					damage *= crushMult;
				}
				
				//5. Damage the armor that was hit
				armor[location].damageItem(processArmorDamage(armor[location], originalDamage), entity);
			}
			else if(armor[location] == null || (armor[location] != null && !(armor[location].getItem() instanceof ItemTFCArmor)))
			{
				//a. If the attack hits an unprotected head, it does 75% more damage
				//b. If the attack hits unprotected feet, it applies a slow to the player
				if(location == 0)
				{
					damage *= 1.75f;
				}
				else if(location == 3)
				{
					entity.addPotionEffect(new PotionEffect(Potion.moveSlowdown.getId(), 600, 1));
				}
			}
			//6. Apply the damage to the player
			EntityArmorCalcEvent eventPost = new EntityArmorCalcEvent(entity, originalDamage, EntityArmorCalcEvent.EventType.POST);
			MinecraftForge.EVENT_BUS.post(eventPost);
			entity.setEntityHealth(entity.getHealth()-eventPost.incomingDamage);

		}

		return 0;
	}
	
	private int getRandomSlot(Random rand)
	{
		int chance = rand.nextInt(100);
		
		if(chance < 10)
			return 0;//Helm
		else if(chance < 20)
			return 3;//Feet
		else if(chance < 80)
			return 1;//Chest
		else
			return 2;//Legs
	}
	
	private int processArmorDamage(ItemStack armor, int baseDamage)
	{
		if(armor.hasTagCompound())
		{
			NBTTagCompound nbt = armor.getTagCompound();
			if(nbt.hasKey("armorReductionBuff"))
			{
				float reductBuff = nbt.getByte("armorReductionBuff")/100f;
				return baseDamage - (int)(baseDamage * reductBuff);
			}
		}
		
		return baseDamage;
	}

	/**
	 * @param AR Armor Rating supplied by the armor
	 * @return Multiplier for damage reduction e.g. damage * multiplier = final damage
	 */
	protected float getDamageReduction(int AR)
	{
		return (1000f / (1000f + AR));
	}
	
	@ForgeSubscribe
	public void onAttackEntity(AttackEntityEvent event)
	{
		EntityLiving attacker = event.entityLiving;
		Entity target = event.target;
		ItemStack stack = attacker.getCurrentItemOrArmor(0);
        if (stack != null && stack.getItem().onLeftClickEntity(stack, event.entityPlayer, event.target))
        {
            return;
        }
        if (target.canAttackWithItem())
        {
            if (!target.func_85031_j(target))
            {
                int i = event.entityPlayer.inventory.getDamageVsEntity(target);

                if (event.entityPlayer.isPotionActive(Potion.damageBoost))
                {
                    i += 3 << event.entityPlayer.getActivePotionEffect(Potion.damageBoost).getAmplifier();
                }

                if (event.entityPlayer.isPotionActive(Potion.weakness))
                {
                    i -= 2 << event.entityPlayer.getActivePotionEffect(Potion.weakness).getAmplifier();
                }

                int j = 0;
                int k = 0;

                if (target instanceof EntityLiving)
                {
                    k = EnchantmentHelper.getEnchantmentModifierLiving(event.entityPlayer, (EntityLiving) target);
                    j += EnchantmentHelper.getKnockbackModifier(event.entityPlayer, (EntityLiving) target);
                }

                if (event.entityPlayer.isSprinting())
                {
                    ++j;
                }

                if (i > 0 || k > 0)
                {
                    boolean flag = event.entityPlayer.fallDistance > 0.0F && !event.entityPlayer.onGround && 
                    		!event.entityPlayer.isOnLadder() && !event.entityPlayer.isInWater() && 
                    		!event.entityPlayer.isPotionActive(Potion.blindness) && event.entityPlayer.ridingEntity == null && 
                    				target instanceof EntityLiving;

                    if (flag && i > 0)
                    {
                        i += event.entity.worldObj.rand.nextInt(i / 2 + 2);
                    }

                    i += k;
                    boolean flag1 = false;
                    int l = EnchantmentHelper.getFireAspectModifier(event.entityPlayer);

                    if (target instanceof EntityLiving && l > 0 && !target.isBurning())
                    {
                        flag1 = true;
                        target.setFire(1);
                    }

                    boolean flag2 = target.attackEntityFrom(DamageSource.causePlayerDamage(event.entityPlayer), i);

                    if (flag2)
                    {
                        if (j > 0)
                        {
                        	target.addVelocity(-MathHelper.sin(event.entityPlayer.rotationYaw * (float)Math.PI / 180.0F) * j * 0.5F, 0.1D, 
                            		MathHelper.cos(event.entityPlayer.rotationYaw * (float)Math.PI / 180.0F) * j * 0.5F);
                            event.entityPlayer.motionX *= 0.6D;
                            event.entityPlayer.motionZ *= 0.6D;
                            event.entityPlayer.setSprinting(false);
                        }

                        if (flag)
                        {
                        	event.entityPlayer.onCriticalHit(target);
                        }

                        if (k > 0)
                        {
                        	event.entityPlayer.onEnchantmentCritical(target);
                        }

                        if (i >= 18)
                        {
                        	event.entityPlayer.triggerAchievement(AchievementList.overkill);
                        }

                        event.entityPlayer.setLastAttackingEntity(target);

                        if (target instanceof EntityLiving)
                        {
                            EnchantmentThorns.func_92096_a(event.entityPlayer, (EntityLiving) target, event.entity.worldObj.rand);
                        }
                    }

                    ItemStack itemstack = event.entityPlayer.getCurrentEquippedItem();
                    Object object = target;

                    if (target instanceof EntityDragonPart)
                    {
                        IEntityMultiPart ientitymultipart = ((EntityDragonPart)target).entityDragonObj;

                        if (ientitymultipart != null && ientitymultipart instanceof EntityLiving)
                        {
                            object = ientitymultipart;
                        }
                    }

                    if (itemstack != null && object instanceof EntityLiving)
                    {
                        itemstack.hitEntity((EntityLiving)object, event.entityPlayer);

                        if (itemstack.stackSize <= 0)
                        {
                        	event.entityPlayer.destroyCurrentEquippedItem();
                        }
                    }

                    if (target instanceof EntityLiving)
                    {
                        if (target.isEntityAlive())
                        {
                        	alertWolves(event.entityPlayer,(EntityLiving) target, true);
                        }

                        event.entityPlayer.addStat(StatList.damageDealtStat, i);

                        if (l > 0 && flag2)
                        {
                        	target.setFire(l * 4);
                        }
                        else if (flag1)
                        {
                        	target.extinguish();
                        }
                    }

                    event.entityPlayer.addExhaustion(0.3F);
                }
            }
        }
        event.setCanceled(true);
	}
	
	protected void alertWolves(EntityPlayer player, EntityLiving par1EntityLiving, boolean par2)
    {
        if (!(par1EntityLiving instanceof EntityCreeper) && !(par1EntityLiving instanceof EntityGhast))
        {
            if (par1EntityLiving instanceof EntityWolf)
            {
                EntityWolf entitywolf = (EntityWolf)par1EntityLiving;

                if (entitywolf.isTamed() && player.username.equals(entitywolf.getOwnerName()))
                {
                    return;
                }
            }

            if (!(par1EntityLiving instanceof EntityPlayer) || player.func_96122_a((EntityPlayer)par1EntityLiving))
            {
                List list = player.worldObj.getEntitiesWithinAABB(EntityWolf.class, AxisAlignedBB.getAABBPool().getAABB(player.posX, player.posY, player.posZ, player.posX + 1.0D, player.posY + 1.0D, player.posZ + 1.0D).expand(16.0D, 4.0D, 16.0D));
                Iterator iterator = list.iterator();

                while (iterator.hasNext())
                {
                    EntityWolf entitywolf1 = (EntityWolf)iterator.next();

                    if (entitywolf1.isTamed() && entitywolf1.getEntityToAttack() == null && player.username.equals(entitywolf1.getOwnerName()) && (!par2 || !entitywolf1.isSitting()))
                    {
                        entitywolf1.setSitting(false);
                        entitywolf1.setTarget(par1EntityLiving);
                    }
                }
            }
        }
    }
}
