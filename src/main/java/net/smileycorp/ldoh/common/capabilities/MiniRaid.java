package net.smileycorp.ldoh.common.capabilities;

import chumbanotz.mutantbeasts.entity.mutant.MutantZombieEntity;
import com.Fishmod.mod_LavaCow.entities.EntityFoglet;
import com.Fishmod.mod_LavaCow.entities.EntityUndeadSwine;
import com.Fishmod.mod_LavaCow.entities.EntityZombieMushroom;
import com.dhanantry.scapeandrunparasites.entity.monster.adapted.*;
import com.dhanantry.scapeandrunparasites.entity.monster.feral.EntityFerHuman;
import com.dhanantry.scapeandrunparasites.entity.monster.infected.EntityInfHuman;
import com.dhanantry.scapeandrunparasites.entity.monster.primitive.*;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.smileycorp.atlas.api.util.DirectionUtils;
import net.smileycorp.hordes.common.Hordes;
import net.smileycorp.hordes.hordeevent.HordeEventPacketHandler;
import net.smileycorp.hordes.hordeevent.HordeSoundMessage;
import net.smileycorp.ldoh.common.ModDefinitions;
import net.smileycorp.ldoh.common.entity.ai.AIMiniRaid;
import net.smileycorp.ldoh.common.entity.zombie.*;
import rafradek.TF2weapons.entity.mercenary.EntityTF2Character;
import rafradek.TF2weapons.util.TF2Class;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MiniRaid implements IMiniRaid {

    protected static final int[] times = {315000, 434000, 606000, 660000, 828000, 1094000, 1172000, 1373000, 1646000, 1756000, 1832000, 2130000, 2226000, 2310000, 2350000, 2400000, 2406000, 2412000};
    protected static final RaidType[] types = {RaidType.ALLY, RaidType.ALLY, RaidType.ZOMBIE, RaidType.ALLY, RaidType.ENEMY, RaidType.ZOMBIE, RaidType.ALLY, RaidType.ALLY, RaidType.PARASITE, RaidType.ZOMBIE, RaidType.ENEMY, RaidType.ALLY, RaidType.PARASITE, RaidType.ENEMY, RaidType.ZOMBIE, RaidType.PARASITE, RaidType.PARASITE, RaidType.PARASITE};

    protected int phase = 0;
    protected int cooldown = 0;

    protected boolean postGame;

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("phase", phase);
        nbt.setInteger("cooldown", cooldown);
        nbt.setBoolean("postGame", postGame);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("phase")) {
            phase = nbt.getInteger("phase");
        }
        if (nbt.hasKey("cooldown")) {
            cooldown = nbt.getInteger("cooldown");
        }
        if (nbt.hasKey("postGame")) {
            cooldown = nbt.getInteger("postGame");
        }
    }

    @Override
    public void enablePostgame() {
        postGame = true;
        cooldown = new Random().nextInt(48000) + 6000;
    }

    @Override
    public boolean shouldSpawnRaid(EntityPlayer player) {
        if (cooldown > 0) cooldown--;
        if (player == null || (phase >= types.length &! postGame)) return false;
        if (postGame) return cooldown <= 0;
        if (player.world.getWorldTime() >= times[phase] && cooldown <= 0) return true;
        return false;
    }

    @Override
    public void spawnRaid(EntityPlayer player) {
        if (player != null) {
            World world = player.world;
            if (!world.isRemote) {
                RaidType type = getRaidType(player);
                if (type != RaidType.NONE) {
                    spawnRaid(player, type, phase);
                } else {
                    System.out.println("Raid type is none, " + player.getName() + " doesn't have a team, aborting raid.");
                }
                phase++;
                cooldown = postGame ? world.rand.nextInt(48000) + 6000 : 6000;
            }
        }
    }

    @Override
    public void spawnRaid(EntityPlayer player, RaidType type, int phase) {
        if (player != null) {
            World world = player.world;
            if (!world.isRemote) {
                Random rand = world.rand;
                Vec3d dir = DirectionUtils.getRandomDirectionVecXZ(rand);
                BlockPos pos = DirectionUtils.getClosestLoadedPos(world, player.getPosition(), dir, 50);
                for (EntityLiving entity : buildList(world, player, type, phase)) {
                    double x = pos.getX() + (rand.nextFloat() * 2) - 1;
                    double z = pos.getZ() + (rand.nextFloat() * 2) - 1;
                    int y = ((WorldServer) world).getHeight(new BlockPos(x, 0, z)).getY();
                    entity.setPosition(x + rand.nextFloat(), y, z + rand.nextFloat());
                    entity.onInitialSpawn(world.getDifficultyForLocation(entity.getPosition()), null);
                    entity.enablePersistence();
                    world.spawnEntity(entity);
                    if (type == RaidType.ALLY) entity.addPotionEffect(new PotionEffect(MobEffects.GLOWING, 140));
                    else if (type == RaidType.ENEMY) entity.addPotionEffect(new PotionEffect(MobEffects.GLOWING, 100));
                    entity.tasks.addTask(1, new AIMiniRaid(entity, player));
                }
                HordeEventPacketHandler.NETWORK_INSTANCE.sendTo(new HordeSoundMessage(dir, getSound(type)), (EntityPlayerMP) player);
            }
        }
    }


    private RaidType getRaidType(EntityPlayer player) {
        if (postGame) return RaidType.randomType(player, player.world.rand);
        RaidType type = types[phase];
        if (type == RaidType.ALLY || type == RaidType.ENEMY) {
            if (player.getTeam() == null) {
                return type == RaidType.ALLY ? RaidType.NONE : phase < 8 ? RaidType.ZOMBIE : RaidType.PARASITE;
            } else if (!(player.getTeam().getName().equals("RED") || player.getTeam().getName().equals("BLU"))) {
                return type == RaidType.ALLY ? RaidType.NONE : phase < 8 ? RaidType.ZOMBIE : RaidType.PARASITE;
            }
        }
        return type;
    }

    private List<EntityLiving> buildList(World world, EntityPlayer player, RaidType type, int phase) {
        Random rand = world.rand;
        boolean hardMode = phase > 8;
        List<EntityLiving> spawnlist = new ArrayList<EntityLiving>();
        switch (type) {
            case ALLY:
                for (int i = 0; i < (phase + 1) * 2.5; i++)
                    try {
                        EntityTF2Character entity = TF2Class.getRandomClass((c) -> c != TF2Class.SPY).createEntity(world);
                        world.getScoreboard().addPlayerToTeam(entity.getCachedUniqueIdString(), player.getTeam().getName());
                        entity.setEntTeam(player.getTeam().getName().equals("RED") ? 0 : 1);
                        spawnlist.add(entity);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                break;
            case ENEMY:
                for (int i = 0; i < (phase + 1) * 2.5; i++)
                    try {
                        EntityTF2Character entity = TF2Class.getRandomClass((c) -> c != TF2Class.SPY).createEntity(world);
                        world.getScoreboard().addPlayerToTeam(entity.getCachedUniqueIdString(), player.getTeam().getName().equals("RED") ? "BLU" : "RED");
                        entity.setEntTeam(player.getTeam().getName().equals("RED") ? 1 : 0);
                        spawnlist.add(entity);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                break;
            case PARASITE:
                for (int i = 0; i < (phase + 1) * 0.5; i++) {
                    int r = rand.nextInt(7);
                    if (r == 0) spawnlist.add(hardMode ? new EntityShycoAdapted(world) : new EntityShyco(world));
                    else if (r == 1) spawnlist.add(hardMode ? new EntityCanraAdapted(world) : new EntityCanra(world));
                    else if (r == 2) spawnlist.add(hardMode ? new EntityNoglaAdapted(world) : new EntityNogla(world));
                    else if (r == 3) spawnlist.add(hardMode ? new EntityHullAdapted(world) : new EntityHull(world));
                    else if (r == 4) spawnlist.add(hardMode ? new EntityEmanaAdapted(world) : new EntityEmana(world));
                    else if (r == 5) spawnlist.add(hardMode ? new EntityBanoAdapted(world) : new EntityBano(world));
                    else if (r == 6) spawnlist.add(hardMode ? new EntityRanracAdapted(world) : new EntityRanrac(world));
                }
                for (int i = 0; i < phase * 1.5; i++) spawnlist.add(hardMode ? new EntityFerHuman(world) : new EntityInfHuman(world));
                break;
            case ZOMBIE:
                if (postGame) {
                    for (int i = 0; i < (phase + 1); i++) {
                        spawnlist.add(new EntityZombieNurse(world));
                    }
                } else {
                    spawnlist.add(new EntityZombieNurse(world));
                    if (hardMode) spawnlist.add(new EntityZombieNurse(world));
                    for (int i = 0; i < (phase + 1); i++) {
                        int r = rand.nextInt(3);
                        if (r == 0) spawnlist.add(new EntitySwatZombie(world));
                        else if (r == 1) spawnlist.add(new EntityZombieMechanic(world));
                        else if (r == 2) spawnlist.add(new EntityZombieTechnician(world));
                    }
                }
                for (int i = 0; i < (phase + 1) * 2.5; i++) {
                    if (hardMode) {
                        int r = rand.nextInt(7);
                        if (r == 0) spawnlist.add(new MutantZombieEntity(world));
                        else if (r == 1) spawnlist.add(new EntityZombieMushroom(world));
                        else if (r == 2) spawnlist.add(new EntityFoglet(world));
                        else if (r == 3) spawnlist.add(new EntityUndeadSwine(world));
                        else if (r == 5) spawnlist.add(new EntityZombieFireman(world));
                        else spawnlist.add(new EntityZombie(world));
                    } else spawnlist.add(new EntityZombie(world));
                };
                break;
            case NONE:
                break;
        }
        return spawnlist;
    }

    private ResourceLocation getSound(RaidType type) {
        if (type == RaidType.ALLY) return ModDefinitions.TF_ALLY_SOUND;
        if (type == RaidType.ENEMY) return ModDefinitions.TF_ENEMY_SOUND;
        return Hordes.HORDE_SOUND;
    }

}