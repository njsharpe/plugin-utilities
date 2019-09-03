package memecat.fatcat.utilities.menu.menus;

import com.google.common.base.Preconditions;
import memecat.fatcat.utilities.menu.MenuManager;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;


/**
 * Represents a collection of functional actions and features of an inventory that multiple viewers can see and interact
 * with, containing many different features that can be customised and extended by a developer.
 * <p>
 * A developer can subclass this class, override the methods or add them to customise the ways of processing inputs for
 * inventory menu events or modifying the inventories.
 *
 * @author Alan B.
 * @see InventoryMenu
 * @see PropertyMenu
 */
public abstract class AbstractMenu implements InventoryHolder {

    /**
     * Main, constant part of an {@link AbstractMenu} that identifies it
     */
    private final Inventory inventory;

    /**
     * A {@link MenuManager} instance that is available for the control of this {@link AbstractMenu}.
     */
    private MenuManager menuManager;

    /**
     * An {@link AbstractMenu} that is set to be opened after this one's closed, handled by a {@link MenuManager}.
     *
     * @see #setNext(AbstractMenu)
     * @see #getNext()
     */
    private AbstractMenu next;

    /**
     * The default constructor for all subclasses.
     *
     * @param inventory   Not null {@link Inventory} that will be wrapped and controlled by an {@link AbstractMenu}
     * @param menuManager Not null {@link MenuManager} that will be used for (un)registering this {@link AbstractMenu}
     *                    and passing events to it
     */
    public AbstractMenu(@NotNull Inventory inventory, @NotNull MenuManager menuManager) {
        Preconditions.checkArgument(inventory != null, "Inventory argument can't be null");
        Preconditions.checkArgument(menuManager != null, "MenuManager argument can't be null");
        this.inventory = inventory;
        this.menuManager = menuManager;
    }

    /**
     * Handles the {@link PluginDisableEvent} of {@link MenuManager}'s {@link MenuManager#getPlugin}.
     *
     * @param event PluginDisableEvent event
     */
    public void onDisable(@NotNull PluginDisableEvent event) {
    }

    /**
     * Acts as an event handler for an ItemStack movement from source to destination inventory.
     * <p>
     * This event handler is most likely called in rare cases; when this {@link AbstractMenu} inventory belongs to a
     * container object and another object or inventory tries to move items to it. An example of this happening can be a
     * chest block container inventory with a hopper connecting to it that is trying to move items into it.
     *
     * @param event         InventoryMoveItemEvent event
     * @param isDestination Whether this {@link org.bukkit.inventory.Inventory} is equal to the {@link
     *                      InventoryMoveItemEvent#getDestination()}
     */
    public void onItemMove(@NotNull InventoryMoveItemEvent event, boolean isDestination) {
        event.setCancelled(true);
    }

    /**
     * Handles any {@link InventoryClickEvent} related to this inventory menu.
     * <p>
     * By default, the {@link InventoryAction} {@code COLLECT_TO_CURSOR} and {@code MOVE_TO_OTHER_INVENTORY} are
     * cancelled and any menu action is cancelled from interaction.
     *
     * @param event    InventoryClickEvent event
     * @param external Whether the clicked inventory is not this menu, possibly not any
     * @see memecat.fatcat.utilities.menu.slot.AbstractSlotProperty
     * @see memecat.fatcat.utilities.menu.slot.SlotProperty
     */
    public void onClick(@NotNull InventoryClickEvent event, boolean external) {
        switch (event.getAction()) {
            case COLLECT_TO_CURSOR:
            case MOVE_TO_OTHER_INVENTORY:
                event.setCancelled(true);
                break;
        }

        if (external) {
            return;
        }

        event.setCancelled(true);
    }

    /**
     * Handles the inventory close events of this menu and opens a next menu after this one.
     * <p>
     * Inventory menus can be immediately reopened by {@link MenuManager} after a menu processes {@link
     * InventoryCloseEvent} with this method. This can be done by setting a menu that will be opened after the next
     * inventory close event with the {@link #setNext(AbstractMenu)} method, including inside this handler.
     *
     * @param event InventoryCloseEvent event
     * @see #getNext()
     */
    public void onClose(@NotNull InventoryCloseEvent event) {
    }

    /**
     * Acts as an event handler for the inventory opening event.
     *
     * @param event InventoryOpenEvent event
     */
    public void onOpen(@NotNull InventoryOpenEvent event) {
    }

    /**
     * Acts as an event handler for the inventory item dragging event.
     * <p>
     * By default, any item dragging on the menu will be cancelled.
     *
     * @param event InventoryDragEvent event
     */
    public void onDrag(@NotNull InventoryDragEvent event) {
        int topEndSlot = event.getView().getTopInventory().getSize() - 1;

        for (int slot : event.getRawSlots()) {
            if (slot <= topEndSlot) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Sets an {@link ItemStack} at this {@link AbstractMenu}'s given slot(s).
     *
     * @param item  {@link ItemStack} to set at given slots
     * @param slots Slots that the {@link ItemStack} will be placed at
     * @return This instance, useful for chaining
     * @throws IndexOutOfBoundsException If a slot in the slot array argument is out of this inventory's array bounds
     * @throws IllegalArgumentException  If the slot array argument is null
     */
    @NotNull
    public AbstractMenu set(@Nullable ItemStack item, @NotNull int... slots) {
        Preconditions.checkArgument(slots != null, "Array of slots can't be null");

        for (int slot : slots) {
            InventoryMenu.checkElement(slot, getSize());
            getInventory().setItem(slot, item);
        }

        return this;
    }

    /**
     * Opens this {@link AbstractMenu} for the given viewers, fail-fast.
     * <p>
     * If any {@link HumanEntity} in the viewers argument is null, a {@link NullPointerException} is thrown - this is to
     * prevent this {@link AbstractMenu} from being registered unnecessarily to it's {@link MenuManager}. If a {@link
     * HumanEntity} had a previous {@link AbstractMenu}, it is closed and {@link AbstractMenu#setNext(AbstractMenu)} is
     * used on it to open this {@link AbstractMenu}.
     *
     * @param viewers {@link Collection}&lt;{@link HumanEntity}&gt; of which each will see this {@link AbstractMenu}
     *                {@link Inventory}
     * @return This instance, useful for chaining
     * @throws IllegalArgumentException If the {@link Collection}&lt;{@link HumanEntity}&gt; argument is null
     * @throws IllegalStateException    If this {@link AbstractMenu}'s {@link MenuManager} isn't registered for handling
     *                                  events
     * @throws NullPointerException     If a {@link HumanEntity} is null
     */
    public AbstractMenu open(@NotNull Collection<HumanEntity> viewers) {
        Preconditions.checkArgument(!viewers.isEmpty(), "Collection<HumanEntity> viewers argument can't be null");
        Preconditions.checkState(menuManager.isRegistered(),
                "MenuManager has no plugin registered to handle inventory menu events");
        viewers.forEach(viewer -> Objects.requireNonNull(viewer, "HumanEntity viewer in the Collection of viewers can't be null"));

        menuManager.registerMenu(this);

        viewers.forEach(viewer -> {
            Optional<AbstractMenu> currentMenu = menuManager.getMenu(viewer.getOpenInventory().getTopInventory());

            if (currentMenu.isPresent()) {
                currentMenu.get().setNext(this);
                viewer.closeInventory();
            } else {
                viewer.openInventory(getInventory());
            }
        });

        return this;
    }

    /**
     * Unregisters this {@link AbstractMenu} from it's previous {@link MenuManager}, sets this one and registers itself
     * to the new, given {@link MenuManager}.
     *
     * @param menuManager Not null {@link MenuManager} that this {@link AbstractMenu} will switch over to
     * @return Previous {@link MenuManager} of this {@link AbstractMenu}
     * @throws IllegalArgumentException If the {@link MenuManager} argument is null
     */
    @NotNull
    public AbstractMenu menuManager(@NotNull MenuManager menuManager) {
        Preconditions.checkArgument(menuManager != null, "MenuManager argument can't be null");

        this.menuManager.unregisterMenu(this);
        this.menuManager = menuManager;
        menuManager.registerMenu(this);

        return this;
    }

    /**
     * Sets the given {@link AbstractMenu} to be opened immediately after this one's closed.
     *
     * @param nextMenu Nullable {@link AbstractMenu}
     * @return This instance, useful for chaining
     * @see #onClose(InventoryCloseEvent)
     */
    @NotNull
    public AbstractMenu setNext(@Nullable AbstractMenu nextMenu) {
        next = nextMenu;
        return this;
    }

    /**
     * Opens this {@link AbstractMenu} for the given viewer(s).
     *
     * @param viewers Array of {@link HumanEntity} of which each will see this {@link AbstractMenu} {@link Inventory}
     * @return This instance, useful for chaining
     * @throws IllegalArgumentException If the {@link HumanEntity} array argument is null
     * @throws IllegalStateException    If this {@link AbstractMenu}'s {@link MenuManager} isn't registered for handling
     *                                  events
     * @throws NullPointerException     If a {@link HumanEntity} is null
     */
    public AbstractMenu open(@NotNull HumanEntity... viewers) {
        return open(Arrays.asList(viewers));
    }

    /**
     * Closes all {@link AbstractMenu}s of this instance for all viewers who are viewing it.
     * <p>
     * Closing {@link AbstractMenu}s for a {@link HumanEntity} might not always work because their {@link
     * #onClose(InventoryCloseEvent)} can choose to open a new, possibly the same one.
     *
     * @return This instance, useful for chaining
     */
    @NotNull
    public final AbstractMenu close() {
        new ArrayList<>(getInventory().getViewers()).forEach(HumanEntity::closeInventory);
        return this;
    }

    /**
     * Clears all of this inventory menu's contents.
     *
     * @return This instance, useful for chaining
     */
    @NotNull
    public AbstractMenu clear() {
        getInventory().clear();
        return this;
    }

    /**
     * Returns an ItemStack at the given slot of this inventory menu or null if it doesn't exist.
     *
     * @param slot Slot index location of the item in the inventory
     * @return {@link Optional} of a nullable {@link ItemStack}
     * @throws IndexOutOfBoundsException If the given slot argument is out of the inventory's array bounds
     */
    @NotNull
    public Optional<ItemStack> getItem(int slot) {
        InventoryMenu.checkElement(slot, getSize());
        return Optional.ofNullable(getInventory().getItem(slot));
    }

    /**
     * Returns the {@link AbstractMenu} that will be opened next after this one's closed.
     *
     * @return {@link Optional} of a nullable {@link AbstractMenu}
     */
    @NotNull
    public Optional<AbstractMenu> getNext() {
        return Optional.ofNullable(next);
    }

    @NotNull
    @Override
    public final Inventory getInventory() {
        return inventory;
    }

    /**
     * Returns the {@link MenuManager} that is used by this {@link AbstractMenu} to (un)register itself, listen to
     * events and access other {@link AbstractMenu}s.
     *
     * @return Not null {@link MenuManager}
     */
    @NotNull
    public MenuManager getMenuManager() {
        return menuManager;
    }

    /**
     * Returns the slot amount that this {@link AbstractMenu} has.
     *
     * @return Amount of slots of this inventory {@link AbstractMenu}
     */
    public int getSize() {
        return getInventory().getSize();
    }
}