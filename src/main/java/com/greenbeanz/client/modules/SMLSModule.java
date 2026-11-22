package com.greenbeanz.client.modules;

import com.greenbeanz.client.accountlink.DiscordLinkAvailability;
import com.greenbeanz.client.utils.ChatUtils;
import com.greenbeanz.client.utils.SMLSConfig;
import com.greenbeanz.client.utils.ServerGuard;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Represents a module in the SMLS client. Use this class to create new modules for the SMLS.
 * A module may require a hub-detected staff rank, a configured department rank, or both.
 */
public abstract class SMLSModule {
    private static final String PREFIX = "§8[§6SMLS§8] §7";

    private final StaffRank minimumStaffRank;
    private final StaffDepartment requiredDepartment;
    private final DepartmentRank minimumDepartmentRank;

    /**
     * Creates a new SMLSModule with the specified minimum staff rank.
     *
     * @param minimumStaffRank the minimum staff rank required to use this module
     */
    protected SMLSModule(StaffRank minimumStaffRank) {
        this(minimumStaffRank, null, null);
    }

    /** Requires only the supplied department rank; the department is inferred from it. */
    protected SMLSModule(DepartmentRank minimumDepartmentRank) {
        this(StaffRank.NONE, departmentOf(minimumDepartmentRank), minimumDepartmentRank);
    }

    /** Requires any assigned rank in the supplied department. */
    protected SMLSModule(StaffDepartment requiredDepartment) {
        this(StaffRank.NONE, Objects.requireNonNull(requiredDepartment, "requiredDepartment"), null);
    }

    /** Requires both the supplied main staff rank and department rank. */
    protected SMLSModule(StaffRank minimumStaffRank, DepartmentRank minimumDepartmentRank) {
        this(minimumStaffRank, departmentOf(minimumDepartmentRank), minimumDepartmentRank);
    }

    /** Requires both the supplied main staff rank and membership in a department. */
    protected SMLSModule(StaffRank minimumStaffRank, StaffDepartment requiredDepartment) {
        this(minimumStaffRank, Objects.requireNonNull(requiredDepartment, "requiredDepartment"), null);
    }

    private SMLSModule(
            StaffRank minimumStaffRank,
            StaffDepartment requiredDepartment,
            DepartmentRank minimumDepartmentRank
    ) {
        this.minimumStaffRank = Objects.requireNonNull(minimumStaffRank, "minimumStaffRank");
        this.requiredDepartment = requiredDepartment;
        this.minimumDepartmentRank = minimumDepartmentRank;
    }

    /**
     * Returns the minimum staff rank required to use this module.
     *
     * @return the minimum staff rank required to use this module
     */
    public StaffRank minimumStaffRank() {
        return minimumStaffRank;
    }

    public Optional<StaffDepartment> requiredDepartment() {
        return Optional.ofNullable(requiredDepartment);
    }

    public Optional<DepartmentRank> minimumDepartmentRank() {
        return Optional.ofNullable(minimumDepartmentRank);
    }

    /** The name displayed on the SMLS home menu. */
    public abstract Component displayName();

    /** The icon displayed on the SMLS home menu. */
    public abstract ItemStack icon();

    /** The description displayed at the top of this module's screen. */
    public abstract List<Component> description();

    /** The collapsible home-screen section containing this module. */
    public ModuleCategory category() {
        return ModuleCategory.GENERAL;
    }

    /** Override for modules whose UI and commands require a verified Discord link. */
    public boolean requiresDiscordLink() {
        return false;
    }

    /** Fast, non-blocking integration check suitable for rendering and Brigadier predicates. */
    public final boolean isEnabledForClient(Minecraft client) {
        return !requiresDiscordLink() || DiscordLinkAvailability.isLinked(client);
    }

    /** Reason shown by shared module pickers when a visible module cannot currently be opened. */
    public final Optional<Component> disabledReason(Minecraft client) {
        if (requiresDiscordLink() && !DiscordLinkAvailability.isLinked(client)) {
            return Optional.of(Component.translatable("SMLS.tooltip.link_discord_for_module"));
        }
        return Optional.empty();
    }

    /** Shared command predicate: rank/department visibility plus required integrations. */
    public final boolean isCommandAvailable(Minecraft client) {
        return isAvailableToDetectedRank() && isEnabledForClient(client);
    }

    /** Opens this module's screen, returning to the supplied SMLS home screen. */
    public abstract void openScreen(Minecraft client, Screen parent);

    /** Shared guarded entry point for module pickers outside the module itself. */
    public final boolean tryOpenScreen(Minecraft client, Screen parent) {
        if (!isEnabledForClient(client)) return false;
        openScreen(client, parent);
        return true;
    }

    /**
     * Checks if the detected staff rank meets the minimum rank of this module.
     * Sends a chat message if it doesn't.
     *
     * @param client the Minecraft client
     * @return true if the detected rank is high enough
     */
    protected boolean hasRequiredRank(Minecraft client) {
        StaffRank currentRank = SMLSConfig.staffRank();
        if (!currentRank.isStaff()) {
            ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.chat.staff.required");
            return false;
        }
        if (minimumStaffRank.isStaff() && !currentRank.isAtLeast(minimumStaffRank)) {
            ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.chat.rank.required",
                    minimumStaffRank.displayName(), currentRank.displayName());
            return false;
        }

        if (requiredDepartment == null) return true;
        DepartmentRank currentDepartmentRank = SMLSConfig.departmentRank(requiredDepartment);
        if (minimumDepartmentRank != null && !currentDepartmentRank.isAtLeast(minimumDepartmentRank)) {
            ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.chat.department_rank.required",
                    minimumDepartmentRank.displayName(), requiredDepartment.displayName(),
                    currentDepartmentRank.displayName());
            return false;
        }
        if (minimumDepartmentRank == null && !currentDepartmentRank.isAssignedTo(requiredDepartment)) {
            ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.chat.department.required",
                    requiredDepartment.displayName());
            return false;
        }
        return true;
    }

    /** The detected rank controls visibility only; the server remains authoritative. */
    protected boolean canRunPrivilegedOperation(Minecraft client) {
        if (!hasRequiredRank(client)) return false;
        if (!hasRequiredIntegrations(client)) return false;
        // A captured dry-run can safely validate and preview commands without a live connection.
        if (SMLSConfig.dryRun()) return true;
        ServerGuard.GuardResult guard = ServerGuard.check(client);
        if (guard.allowed()) return true;
        ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.chat.server_guard.blocked",
                guard.reason(), guard.address());
        return false;
    }

    /** Sends the relevant local guidance when a declared integration is unavailable. */
    protected final boolean hasRequiredIntegrations(Minecraft client) {
        if (!requiresDiscordLink() || DiscordLinkAvailability.isLinked(client)) return true;
        ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.chat.discord_link.required");
        return false;
    }

    /** Returns whether the detected staff rank may use this module without producing chat output. */
    public final boolean isAvailableToDetectedRank() {
        StaffRank currentStaffRank = SMLSConfig.staffRank();
        DepartmentRank currentDepartmentRank = requiredDepartment == null
                ? DepartmentRank.NOT_ASSIGNED
                : SMLSConfig.departmentRank(requiredDepartment);
        return isAvailableFor(currentStaffRank, currentDepartmentRank);
    }

    public final boolean isVisibleForDetectedRank() {
        return isAvailableToDetectedRank();
    }

    /** Pure requirement check useful for previews and tests. */
    public final boolean isAvailableFor(StaffRank currentStaffRank, DepartmentRank currentDepartmentRank) {
        if (currentStaffRank == null || !currentStaffRank.isStaff()) return false;
        if (minimumStaffRank.isStaff() && !currentStaffRank.isAtLeast(minimumStaffRank)) return false;
        if (requiredDepartment == null) return true;
        if (currentDepartmentRank == null) return false;
        return minimumDepartmentRank == null
                ? currentDepartmentRank.isAssignedTo(requiredDepartment)
                : currentDepartmentRank.isAtLeast(minimumDepartmentRank);
    }

    /** Checks a hypothetical main rank while using the currently configured department rank. */
    public final boolean isAvailableForStaffRank(StaffRank currentStaffRank) {
        DepartmentRank currentDepartmentRank = requiredDepartment == null
                ? DepartmentRank.NOT_ASSIGNED
                : SMLSConfig.departmentRank(requiredDepartment);
        return isAvailableFor(currentStaffRank, currentDepartmentRank);
    }

    private static StaffDepartment departmentOf(DepartmentRank departmentRank) {
        DepartmentRank rank = Objects.requireNonNull(departmentRank, "minimumDepartmentRank");
        if (rank == DepartmentRank.NOT_ASSIGNED) {
            throw new IllegalArgumentException("NOT_ASSIGNED cannot be a module requirement");
        }
        return rank.department();
    }

    /**
     * Registers the module with the game.
     */
    public abstract void register();
}
