package com.greenbeanz.client.moderation;

import com.greenbeanz.client.modules.StaffRank;
import com.greenbeanz.client.session.CommandDispatch;
import com.greenbeanz.client.session.ManagedOperation;
import com.greenbeanz.client.session.OperationCoordinator;
import com.greenbeanz.client.session.OperationHandle;
import com.greenbeanz.client.session.OperationStartResult;
import com.greenbeanz.client.utils.ClientUtils;
import com.greenbeanz.client.utils.SMLSConfig;
import net.minecraft.client.Minecraft;

/** Guarded moderation-view command dispatch. */
public final class ModerationActions {
    private ModerationActions() {
    }

    public static CommandDispatch sendToChannel(Minecraft client, ChatChannel channel, String message) {
        String clean = message == null ? "" : message.trim();
        if (!SMLSConfig.hasRecognizedStaffRank() || channel == null || !channel.selectable()
                || clean.isEmpty() || clean.indexOf('§') >= 0
                || clean.chars().anyMatch(Character::isISOControl)) {
            return CommandDispatch.BLOCKED;
        }
        if (channel == ChatChannel.GLOBAL) {
            return ClientUtils.dispatchChatMessage(client, clean);
        }
        return ClientUtils.dispatchCommand(client, channel.sendCommand() + " " + clean);
    }

    public static Outcome punish(Minecraft client, PunishmentRequest request) {
        if (request == null) return Outcome.INVALID;
        StaffRank rank = SMLSConfig.staffRank();
        if (!rank.isStaff() || !rank.isAtLeast(request.type().minimumRank())) return Outcome.RANK_BLOCKED;

        OneShotOperation operation = new OneShotOperation(request.command());
        OperationStartResult start = OperationCoordinator.global().start(client,
                "moderation-" + request.type().name().toLowerCase(),
                request.type().displayName().getString(), operation);
        if (start == OperationStartResult.BUSY) return Outcome.BUSY;
        if (start != OperationStartResult.STARTED) return Outcome.BLOCKED;
        return switch (operation.dispatch) {
            case SENT -> {
                // Helpers do not receive the moderator punishment broadcasts that confirm the action.
                // Higher ranks are recorded only when that server confirmation is captured.
                if (rank == StaffRank.HELPER) PunishmentLogService.shared().recordLocal(client, request);
                yield Outcome.SENT;
            }
            case SIMULATED -> Outcome.SIMULATED;
            case BLOCKED -> Outcome.BLOCKED;
        };
    }

    public enum Outcome { SENT, SIMULATED, INVALID, RANK_BLOCKED, BLOCKED, BUSY }

    private static final class OneShotOperation implements ManagedOperation {
        private final String command;
        private CommandDispatch dispatch = CommandDispatch.BLOCKED;

        private OneShotOperation(String command) {
            this.command = command;
        }

        @Override
        public void onStarted(OperationHandle handle, Minecraft client) {
            dispatch = handle.dispatchCommand(client, command);
            handle.complete();
        }
    }
}
