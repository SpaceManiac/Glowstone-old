package net.glowstone.entity;

import com.flowpowered.networking.Message;
import net.glowstone.EventFactory;
import net.glowstone.constants.GlowPotionEffect;
import net.glowstone.inventory.EquipmentMonitor;
import net.glowstone.net.message.play.entity.EntityEquipmentMessage;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * A GlowLivingEntity is a {@link org.bukkit.entity.Player} or {@link org.bukkit.entity.Monster}.
 *
 * @author Graham Edgecombe.
 */
public abstract class GlowLivingEntity extends GlowEntity implements LivingEntity {

    /**
     * Potion effects on the entity.
     */
    private final Map<PotionEffectType, PotionEffect> potionEffects = new HashMap<>();

    /**
     * The entity's health.
     */
    protected double health;

    /**
     * The entity's maximum health.
     */
    protected double maxHealth;

    /**
     * The magnitude of the last damage the entity took.
     */
    private double lastDamage;

    /**
     * How long the entity has until it runs out of air.
     */
    private int airTicks = 300;

    /**
     * The maximum amount of air the entity can hold.
     */
    private int maximumAir = 300;

    /**
     * The number of ticks remaining in the invincibility period.
     */
    private int noDamageTicks = 0;

    /**
     * The default length of the invincibility period.
     */
    private int maxNoDamageTicks = 20;

    /**
     * A custom overhead name to be shown for non-Players.
     */
    private String customName;

    /**
     * Whether the custom name is shown.
     */
    private boolean customNameVisible;

    /**
     * Whether the entity should be removed if it is too distant from players.
     */
    private boolean removeDistance;

    /**
     * Whether the (non-Player) entity can pick up armor and tools.
     */
    private boolean pickupItems;

    /**
     * Monitor for the equipment of this entity.
     */
    private EquipmentMonitor equipmentMonitor = new EquipmentMonitor(this);

    /**
     * Creates a mob within the specified world.
     *
     * @param location The location.
     */
    public GlowLivingEntity(Location location) {
        super(location);
        resetMaxHealth();
        health = maxHealth;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Internals

    @Override
    public void pulse() {
        super.pulse();

        // invulnerability
        if (noDamageTicks > 0) {
            --noDamageTicks;
        }

        Material mat = getEyeLocation().getBlock().getType();
        // breathing
        if (mat == Material.WATER || mat == Material.STATIONARY_WATER) {
            if (canDrown()) {
                --airTicks;
                if (airTicks <= -20) {
                    airTicks = 0;
                    damage(1, EntityDamageEvent.DamageCause.DROWNING);
                }
            }
        } else {
            airTicks = maximumAir;
        }

        takeCactusDamage();

        // potion effects
        List<PotionEffect> effects = new ArrayList<>(potionEffects.values());
        for (PotionEffect effect : effects) {
            // pulse effect
            GlowPotionEffect type = (GlowPotionEffect) effect.getType();
            type.pulse(this, effect);

            if (effect.getDuration() > 0) {
                // reduce duration and re-add
                addPotionEffect(new PotionEffect(type, effect.getDuration() - 1, effect.getAmplifier(), effect.isAmbient()), true);
            } else {
                // remove
                removePotionEffect(type);
            }
        }
    }

    @Override
    public void reset() {
        super.reset();
        equipmentMonitor.resetChanges();
    }

    @Override
    public List<Message> createUpdateMessage() {
        List<Message> messages = super.createUpdateMessage();

        for (EquipmentMonitor.Entry change : equipmentMonitor.getChanges()) {
            messages.add(new EntityEquipmentMessage(id, change.slot, change.item));
        }

        return messages;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties

    @Override
    public double getEyeHeight() {
        return 0;
    }

    @Override
    public double getEyeHeight(boolean ignoreSneaking) {
        return getEyeHeight();
    }

    @Override
    public Location getEyeLocation() {
        return getLocation().add(0, getEyeHeight(), 0);
    }

    @Override
    public Player getKiller() {
        return null;
    }

    @Override
    public boolean hasLineOfSight(Entity other) {
        return false;
    }

    @Override
    public EntityEquipment getEquipment() {
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties

    @Override
    public int getNoDamageTicks() {
        return noDamageTicks;
    }

    @Override
    public void setNoDamageTicks(int ticks) {
        noDamageTicks = ticks;
    }

    @Override
    public int getMaximumNoDamageTicks() {
        return maxNoDamageTicks;
    }

    @Override
    public void setMaximumNoDamageTicks(int ticks) {
        maxNoDamageTicks = ticks;
    }

    @Override
    public int getRemainingAir() {
        return airTicks;
    }

    @Override
    public void setRemainingAir(int ticks) {
        airTicks = Math.min(ticks, maximumAir);
    }

    @Override
    public int getMaximumAir() {
        return maximumAir;
    }

    @Override
    public void setMaximumAir(int ticks) {
        maximumAir = Math.max(0, ticks);
    }

    @Override
    public boolean getRemoveWhenFarAway() {
        return removeDistance;
    }

    @Override
    public void setRemoveWhenFarAway(boolean remove) {
        removeDistance = remove;
    }

    @Override
    public boolean getCanPickupItems() {
        return pickupItems;
    }

    @Override
    public void setCanPickupItems(boolean pickup) {
        pickupItems = pickup;
    }

    /**
     * Get the hurt sound of this entity, or null for silence.
     * @return the hurt sound if available
     */
    protected Sound getHurtSound() {
        return null;
    }

    /**
     * Get the death sound of this entity, or null for silence.
     * @return the death sound if available
     */
    protected Sound getDeathSound() {
        return null;
    }

    /**
     * Get whether this entity should take drowning damage.
     * @return whether this entity can drown
     */
    protected boolean canDrown() {
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Line of Sight

    private List<Block> getLineOfSight(HashSet<Byte> transparent, int maxDistance, int maxLength) {
        // same limit as CraftBukkit
        if (maxDistance > 120) {
            maxDistance = 120;
        }

        LinkedList<Block> blocks = new LinkedList<>();
        Iterator<Block> itr = new BlockIterator(this, maxDistance);
        while (itr.hasNext()) {
            Block block = itr.next();
            blocks.add(block);
            if (maxLength != 0 && blocks.size() > maxLength) {
                blocks.removeFirst();
            }
            int id = block.getTypeId();
            if (transparent == null) {
                if (id != 0) {
                    break;
                }
            } else {
                if (!transparent.contains((byte) id)) {
                    break;
                }
            }
        }
        return blocks;
    }

    @Override
    public List<Block> getLineOfSight(HashSet<Byte> transparent, int maxDistance) {
        return getLineOfSight(transparent, maxDistance, 0);
    }

    @Override
    public Block getTargetBlock(HashSet<Byte> transparent, int maxDistance) {
        return getLineOfSight(transparent, maxDistance, 1).get(0);
    }

    @Override
    public List<Block> getLastTwoTargetBlocks(HashSet<Byte> transparent, int maxDistance) {
        return getLineOfSight(transparent, maxDistance, 2);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Projectiles

    @Override
    public Egg throwEgg() {
        return launchProjectile(Egg.class);
    }

    @Override
    public Snowball throwSnowball() {
        return launchProjectile(Snowball.class);
    }

    @Override
    public Arrow shootArrow() {
        return launchProjectile(Arrow.class);
    }

    @Override
    public <T extends Projectile> T launchProjectile(Class<? extends T> projectile) {
        return launchProjectile(projectile, getLocation().getDirection());  // todo: multiply by some speed
    }

    @Override
    public <T extends Projectile> T launchProjectile(Class<? extends T> projectile, Vector velocity) {
        T entity = world.spawn(getEyeLocation(), projectile);
        entity.setVelocity(velocity);
        return entity;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Health

    @Override
    public double getHealth() {
        return health;
    }

    @Override
    public void setHealth(double health) {
        if (health < 0) health = 0;
        if (health > maxHealth) health = maxHealth;
        this.health = health;
    }

    @Override
    public void damage(double amount) {
        damage(amount, null, EntityDamageEvent.DamageCause.CUSTOM);
    }

    @Override
    public void damage(double amount, Entity source) {
        damage(amount, source, EntityDamageEvent.DamageCause.CUSTOM);
    }

    @Override
    public void damage(double amount, EntityDamageEvent.DamageCause cause) {
        damage(amount, null, cause);
    }

    @Override
    public void damage(double amount, Entity source, EntityDamageEvent.DamageCause cause) {
        // invincibility timer
        if (noDamageTicks > 0 || health <= 0) {
            return;
        } else {
            noDamageTicks = maxNoDamageTicks;
        }

        // fire resistance
        if (cause != null && hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
            switch (cause) {
                case PROJECTILE:
                    if (source == null || !(source instanceof Fireball)) {
                        break;
                    }
                case FIRE:
                case FIRE_TICK:
                case LAVA:
                    return;
            }
        }

        // fire event
        // todo: use damage modifier system
        EntityDamageEvent event;
        if (source == null) {
            event = new EntityDamageEvent(this, cause, amount);
        } else {
            event = new EntityDamageByEntityEvent(source, this, cause, amount);
        }
        EventFactory.callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        // apply damage
        amount = event.getFinalDamage();
        lastDamage = amount;
        setHealth(health - amount);
        playEffect(EntityEffect.HURT);

        // play sounds, handle death
        if (health <= 0.0) {
            Sound deathSound = getDeathSound();
            if (deathSound != null) {
                world.playSound(location, deathSound, 1.0f, 1.0f);
            }
            // todo: drop items
        } else {
            Sound hurtSound = getHurtSound();
            if (hurtSound != null) {
                world.playSound(location, hurtSound, 1.0f, 1.0f);
            }
        }
    }

    @Override
    public double getMaxHealth() {
        return maxHealth;
    }

    @Override
    public void setMaxHealth(double health) {
        maxHealth = health;
    }

    @Override
    public void resetMaxHealth() {
        maxHealth = 20;
    }

    @Override
    public double getLastDamage() {
        return lastDamage;
    }

    @Override
    public void setLastDamage(double damage) {
        lastDamage = damage;
    }


    public boolean canTakeCactusDamage() {
        return true;
    }

    public void takeCactusDamage() {

        // Gets the block the are standing on and the location of it
        Material blockBelow = getLocation().getBlock().getType();
        Location blockLocationX = getLocation().getBlock().getLocation();
        Location blockLocationNegX = getLocation().getBlock().getLocation();
        Location blockLocationZ = getLocation().getBlock().getLocation();
        Location blockLocationNegZ = getLocation().getBlock().getLocation();

        // sets the location to its respective cord
        blockLocationX.setX(blockLocationX.getX() + 1);
        blockLocationNegX.setX(blockLocationNegX.getX() - 1);
        blockLocationZ.setZ(blockLocationZ.getZ() + 1);
        blockLocationNegZ.setZ(blockLocationNegZ.getZ() - 1);

        //Checks if the living entity is near a cactus or is on top of a cactus and hurts them
        if ((blockBelow == Material.CACTUS || blockLocationX.getBlock().getType() == Material.CACTUS ||
                blockLocationNegX.getBlock().getType() == Material.CACTUS || blockLocationZ.getBlock().getType() == Material.CACTUS ||
                blockLocationNegZ.getBlock().getType() == Material.CACTUS) && canTakeCactusDamage()) {
            damage(1, EntityDamageEvent.DamageCause.CONTACT);
        }

    }

    ////////////////////////////////////////////////////////////////////////////
    // Invalid health methods

    @Override
    public void _INVALID_damage(int amount) {
        damage(amount);
    }

    @Override
    public int _INVALID_getLastDamage() {
        return (int) getLastDamage();
    }

    @Override
    public void _INVALID_setLastDamage(int damage) {
        setLastDamage(damage);
    }

    @Override
    public void _INVALID_setMaxHealth(int health) {
        setMaxHealth(health);
    }

    @Override
    public int _INVALID_getMaxHealth() {
        return (int) getMaxHealth();
    }

    @Override
    public void _INVALID_damage(int amount, Entity source) {
        damage(amount, source);
    }

    @Override
    public int _INVALID_getHealth() {
        return (int) getHealth();
    }

    @Override
    public void _INVALID_setHealth(int health) {
        setHealth(health);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Potion effects

    @Override
    public boolean addPotionEffect(PotionEffect effect) {
        return addPotionEffect(effect, false);
    }

    @Override
    public boolean addPotionEffect(PotionEffect effect, boolean force) {
        if (potionEffects.containsKey(effect.getType())) {
            if (force) {
                removePotionEffect(effect.getType());
            } else {
                return false;
            }
        }

        potionEffects.put(effect.getType(), effect);

        // todo: this, updated, only players in range
        /*EntityEffectMessage msg = new EntityEffectMessage(getEntityId(), effect.getType().getId(), effect.getAmplifier(), effect.getDuration());
        for (Player player : server.getOnlinePlayers()) {
            ((GlowPlayer) player).getSession().send(msg);
        }*/
        return true;
    }

    @Override
    public boolean addPotionEffects(Collection<PotionEffect> effects) {
        boolean result = true;
        for (PotionEffect effect : effects) {
            if (!addPotionEffect(effect)) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public boolean hasPotionEffect(PotionEffectType type) {
        return potionEffects.containsKey(type);
    }

    @Override
    public void removePotionEffect(PotionEffectType type) {
        if (!hasPotionEffect(type)) return;
        potionEffects.remove(type);

        // todo: this, improved, for players in range
        /*EntityRemoveEffectMessage msg = new EntityRemoveEffectMessage(getEntityId(), type.getId());
        for (Player player : server.getOnlinePlayers()) {
            ((GlowPlayer) player).getSession().send(msg);
        }*/
    }

    @Override
    public Collection<PotionEffect> getActivePotionEffects() {
        return Collections.unmodifiableCollection(potionEffects.values());
    }

    ////////////////////////////////////////////////////////////////////////////
    // Custom name

    @Override
    public void setCustomName(String name) {
        customName = name;
    }

    @Override
    public String getCustomName() {
        return customName;
    }

    @Override
    public void setCustomNameVisible(boolean flag) {
        customNameVisible = flag;
    }

    @Override
    public boolean isCustomNameVisible() {
        return customNameVisible;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Leashes

    @Override
    public boolean isLeashed() {
        return false;
    }

    @Override
    public Entity getLeashHolder() throws IllegalStateException {
        return null;
    }

    @Override
    public boolean setLeashHolder(Entity holder) {
        return false;
    }

}
