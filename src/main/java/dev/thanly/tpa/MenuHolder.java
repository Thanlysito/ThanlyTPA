package dev.thanly.tpa;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Marca los inventarios que son menus del plugin, y recuerda que jugador hay en cada casilla.
 */
public final class MenuHolder implements InventoryHolder {

    public enum Kind {
        /** lista de jugadores para enviar una solicitud (/tpa o /tpahere sin nombre) */
        PLAYERS,
        /** solicitudes recibidas (/tpamenu, o /tpaccept con varias pendientes) */
        REQUESTS
    }

    public static final int CLOSE_SLOT = -1;

    private final Kind kind;
    private final TpaRequest.Type mode;
    private final Map<Integer, UUID> slots = new HashMap<>();
    private Inventory inventory;
    private int closeSlot = CLOSE_SLOT;

    public MenuHolder(Kind kind, TpaRequest.Type mode) {
        this.kind = kind;
        this.mode = mode;
    }

    public Kind kind() {
        return kind;
    }

    public TpaRequest.Type mode() {
        return mode;
    }

    public void put(int slot, UUID player) {
        slots.put(slot, player);
    }

    public UUID playerAt(int slot) {
        return slots.get(slot);
    }

    public int closeSlot() {
        return closeSlot;
    }

    public void setCloseSlot(int slot) {
        this.closeSlot = slot;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
