package com.greenbeanz.client.modules;

import com.greenbeanz.client.gui.modules.CoreProtectBuilderScreen;
import com.greenbeanz.client.message.MessageOrigin;
import com.greenbeanz.client.message.ServerMessage;
import com.greenbeanz.client.parser.CoreProtectResponseParser;
import com.greenbeanz.client.session.CommandDispatch;
import com.greenbeanz.client.session.ManagedOperation;
import com.greenbeanz.client.session.OperationCancelReason;
import com.greenbeanz.client.session.OperationCoordinator;
import com.greenbeanz.client.session.OperationHandle;
import com.greenbeanz.client.session.OperationStartResult;
import com.greenbeanz.client.session.PendingConfirmation;
import com.greenbeanz.client.utils.ChatUtils;
import com.greenbeanz.client.utils.SMLSConfig;
import com.greenbeanz.client.utils.ServerGuard;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Composes CoreProtect lookup, rollback and restore commands from a form instead of memorized syntax. */
public final class CoreProtectBuilderModule extends SMLSModule {
    public static final String OPERATION_ID = "coreprotect-builder";
    public static final List<String> MODES = List.of("lookup", "rollback", "restore");
    public static final List<String> ACTIONS = List.of("(any)", "block", "+block", "-block", "container", "kill", "item", "sign", "chat", "command", "session");
    public static final int MAX_COMMAND_LENGTH = 256;

    private static final String PREFIX = "§8[§6SMLS - CoreProtect§8] §7";
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final Pattern TIME = Pattern.compile("(\\d{1,4}[wdhms])+");
    private static final Pattern RADIUS = Pattern.compile("#global|\\d{1,5}");
    private static final Pattern LIST = Pattern.compile("[a-z0-9_,:#\\-]+");
    private static final int RESPONSE_TIMEOUT_TICKS = 20 * 60;

    private PendingConfirmation<CommandRequest> pendingConfirmation;

    /** The composed command, or the translation key of the first validation error. */
    public record BuildResult(String command, String errorKey, boolean destructive) {
        public boolean valid() {
            return errorKey.isEmpty();
        }

        private static BuildResult error(String errorKey) {
            return new BuildResult("", errorKey, false);
        }
    }

    /** Immutable request shared by validation, preview, and execution. */
    public record CommandRequest(String mode, String action, String command, String errorKey,
                                 boolean destructive) {
        public boolean valid() {
            return errorKey.isEmpty();
        }
    }

    public enum SubmissionOutcome {
        STAGED,
        STARTED,
        SIMULATED,
        INVALID,
        RANK_BLOCKED,
        SERVER_BLOCKED,
        BUSY,
        IO_ERROR
    }

    public record SubmissionResult(SubmissionOutcome outcome, CommandRequest request, String token,
                                   String errorKey) {
        public boolean staged() {
            return outcome == SubmissionOutcome.STAGED;
        }

        public boolean accepted() {
            return staged() || outcome == SubmissionOutcome.STARTED || outcome == SubmissionOutcome.SIMULATED;
        }
    }

    public CoreProtectBuilderModule() {
        super(StaffRank.SENIOR_MODERATOR);
    }

    @Override
    public Component displayName() {
        return Component.translatable("SMLS.module.co_builder.name");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(Items.SPYGLASS);
    }

    @Override
    public List<Component> description() {
        return List.of(
                Component.translatable("SMLS.module.co_builder.description.1"),
                Component.translatable("SMLS.module.co_builder.description.2")
        );
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.GENERAL;
    }

    @Override
    public void openScreen(Minecraft client, Screen parent) {
        client.gui.setScreen(new CoreProtectBuilderScreen(parent, this));
    }

    @Override
    public void register() {
        // GUI only, opened from the SMLS menu or /SMLS co.
    }

    /** Opens the builder screen. */
    public void openScreenDeferred(Minecraft client) {
        // next tick, otherwise the closing chat screen overrides it
        client.schedule(() -> client.gui.setScreen(new CoreProtectBuilderScreen(null, this)));
    }

    /** Validates the form values and composes the CoreProtect command. */
    public static BuildResult build(String mode, String user, String time, String radius, String action, String include, String exclude) {
        String cleanMode = normalize(mode);
        if (!MODES.contains(cleanMode)) {
            return BuildResult.error("SMLS.validation.co.mode");
        }

        String cleanAction = normalize(action);
        if (cleanAction.isEmpty()) cleanAction = ACTIONS.get(0);
        if (!ACTIONS.contains(cleanAction)) {
            return BuildResult.error("SMLS.validation.co.action");
        }

        String cleanUser = value(user).trim();
        if (!cleanUser.isEmpty() && !USERNAME.matcher(cleanUser).matches()) {
            return BuildResult.error("SMLS.validation.co.user");
        }

        String cleanTime = normalize(time);
        if (!cleanTime.isEmpty() && !TIME.matcher(cleanTime).matches()) {
            return BuildResult.error("SMLS.validation.co.time");
        }

        String cleanRadius = normalize(radius);
        if (!cleanRadius.isEmpty() && !RADIUS.matcher(cleanRadius).matches()) {
            return BuildResult.error("SMLS.validation.co.radius");
        }

        String cleanInclude = normalizeList(include);
        if (!cleanInclude.isEmpty() && !LIST.matcher(cleanInclude).matches()) {
            return BuildResult.error("SMLS.validation.co.include");
        }

        String cleanExclude = normalizeList(exclude);
        if (!cleanExclude.isEmpty() && !LIST.matcher(cleanExclude).matches()) {
            return BuildResult.error("SMLS.validation.co.exclude");
        }

        boolean destructive = !cleanMode.equals("lookup");
        if (destructive && cleanTime.isEmpty()) {
            return BuildResult.error("SMLS.validation.co.time_required");
        }
        if (destructive && cleanUser.isEmpty() && cleanRadius.isEmpty()) {
            return BuildResult.error("SMLS.validation.co.target_required");
        }
        if (!destructive && cleanUser.isEmpty() && cleanTime.isEmpty() && cleanRadius.isEmpty() && cleanInclude.isEmpty()) {
            return BuildResult.error("SMLS.validation.co.lookup_empty");
        }

        StringBuilder command = new StringBuilder("co ").append(cleanMode);
        if (!cleanUser.isEmpty()) {
            command.append(" u:").append(cleanUser);
        }
        if (!cleanTime.isEmpty()) {
            command.append(" t:").append(cleanTime);
        }
        if (!cleanRadius.isEmpty()) {
            command.append(" r:").append(cleanRadius);
        }
        if (!cleanAction.equals(ACTIONS.get(0))) {
            command.append(" a:").append(cleanAction);
        }
        if (!cleanInclude.isEmpty()) {
            command.append(" i:").append(cleanInclude);
        }
        if (!cleanExclude.isEmpty()) {
            command.append(" e:").append(cleanExclude);
        }
        if (command.length() > MAX_COMMAND_LENGTH) {
            return BuildResult.error("SMLS.validation.co.command_too_long");
        }
        return new BuildResult(command.toString(), "", destructive);
    }

    public static CommandRequest prepare(String mode, String user, String time, String radius,
                                         String action, String include, String exclude) {
        BuildResult result = build(mode, user, time, radius, action, include, exclude);
        return new CommandRequest(normalize(mode), normalize(action), result.command(), result.errorKey(),
                result.destructive());
    }

    /** Validates a lookup immediately or stages rollback/restore for bounded confirmation. */
    public SubmissionResult submit(Minecraft client, String mode, String user, String time, String radius,
                                   String action, String include, String exclude) {
        invalidatePending();
        CommandRequest request = prepare(mode, user, time, radius, action, include, exclude);
        if (!request.valid()) {
            ChatUtils.sendTranslatedMessage(client, PREFIX, request.errorKey());
            return result(SubmissionOutcome.INVALID, request, "", request.errorKey());
        }

        SubmissionResult blocked = validateExecution(client, request);
        if (blocked != null) return blocked;

        if (OperationCoordinator.global().isBusy()) {
            String owner = OperationCoordinator.global().activeDescriptor()
                    .map(descriptor -> descriptor.displayName()).orElse("another operation");
            ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.chat.operation.busy", owner);
            return result(SubmissionOutcome.BUSY, request, "", "SMLS.validation.operation.busy");
        }

        if (request.destructive()) {
            pendingConfirmation = new PendingConfirmation<>(request);
            return result(SubmissionOutcome.STAGED, request, pendingConfirmation.token(), "");
        }
        return start(client, request);
    }

    public SubmissionResult confirm(Minecraft client, String token) {
        PendingConfirmation<CommandRequest> pending = pendingConfirmation;
        if (pending == null) {
            return result(SubmissionOutcome.INVALID, invalidRequest(), "", "SMLS.screen.confirmation.expired");
        }
        PendingConfirmation.ConsumeResult<CommandRequest> consumed = pending.consume(token);
        if (consumed.status() != PendingConfirmation.ConsumeStatus.CONFIRMED) {
            String error = consumed.status() == PendingConfirmation.ConsumeStatus.EXPIRED
                    ? "SMLS.screen.confirmation.expired" : "SMLS.screen.confirmation.failed";
            return result(SubmissionOutcome.INVALID, pending.request(), "", error);
        }
        pendingConfirmation = null;
        CommandRequest request = consumed.request().orElseThrow();
        SubmissionResult blocked = validateExecution(client, request);
        if (blocked != null) return blocked;
        return start(client, request);
    }

    public boolean isPending(String token) {
        return pendingConfirmation != null && pendingConfirmation.token().equals(token)
                && pendingConfirmation.isActive();
    }

    public void invalidatePending(String token) {
        if (pendingConfirmation != null && pendingConfirmation.token().equals(token)) invalidatePending();
    }

    public void cancel(Minecraft client) {
        if (pendingConfirmation != null && pendingConfirmation.isActive()) {
            invalidatePending();
            return;
        }
        OperationCoordinator.global().cancel(OPERATION_ID, client);
    }

    public List<Component> previewLines(CommandRequest request) {
        return List.of(
                Component.translatable("SMLS.screen.co.review_warning", request.mode()),
                Component.literal("/" + request.command())
        );
    }

    private SubmissionResult validateExecution(Minecraft client, CommandRequest request) {
        if (!hasRequiredRank(client)) {
            return result(SubmissionOutcome.RANK_BLOCKED, request, "", "SMLS.validation.operation.rank_blocked");
        }
        if (SMLSConfig.dryRun()) return null;
        ServerGuard.GuardResult guard = ServerGuard.check(client);
        if (guard.allowed()) return null;
        ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.chat.server_guard.blocked",
                guard.reason(), guard.address());
        return result(SubmissionOutcome.SERVER_BLOCKED, request, "", "SMLS.validation.server_blocked");
    }

    private SubmissionResult start(Minecraft client, CommandRequest request) {
        CoreProtectCommandOperation operation = new CoreProtectCommandOperation(request);
        OperationStartResult started = OperationCoordinator.global().start(
                client, OPERATION_ID, displayName().getString(), operation);
        if (started == OperationStartResult.BUSY) {
            return result(SubmissionOutcome.BUSY, request, "", "SMLS.validation.operation.busy");
        }
        if (started != OperationStartResult.STARTED || operation.dispatch == CommandDispatch.BLOCKED) {
            SubmissionOutcome outcome = started == OperationStartResult.SERVER_BLOCKED
                    ? SubmissionOutcome.SERVER_BLOCKED : SubmissionOutcome.INVALID;
            return result(outcome, request, "", "SMLS.validation.operation.start_failed");
        }
        return result(operation.dispatch == CommandDispatch.SIMULATED
                ? SubmissionOutcome.SIMULATED : SubmissionOutcome.STARTED, request, "", "");
    }

    private static SubmissionResult result(SubmissionOutcome outcome, CommandRequest request,
                                           String token, String errorKey) {
        return new SubmissionResult(outcome, request, token, errorKey);
    }

    private static CommandRequest invalidRequest() {
        return new CommandRequest("", "", "", "SMLS.screen.confirmation.expired", true);
    }

    private void invalidatePending() {
        if (pendingConfirmation != null) pendingConfirmation.invalidate();
        pendingConfirmation = null;
    }

    private final class CoreProtectCommandOperation implements ManagedOperation {
        private final CommandRequest request;
        private CommandDispatch dispatch = CommandDispatch.BLOCKED;
        private int waitTicks;

        private CoreProtectCommandOperation(CommandRequest request) {
            this.request = request;
        }

        @Override
        public void onStarted(OperationHandle handle, Minecraft client) {
            dispatch = handle.dispatchCommand(client, request.command());
            if (dispatch == CommandDispatch.BLOCKED) {
                handle.cancel(client, OperationCancelReason.DISPATCH_BLOCKED);
            } else if (dispatch == CommandDispatch.SIMULATED) {
                ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.chat.co.simulated", "/" + request.command());
                handle.complete();
            } else {
                ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.chat.co.sent", "/" + request.command());
                if (!request.destructive()) handle.complete();
            }
        }

        @Override
        public void onTick(OperationHandle handle, Minecraft client) {
            if (dispatch != CommandDispatch.SENT || !request.destructive()) return;
            if (++waitTicks >= RESPONSE_TIMEOUT_TICKS) {
                ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.chat.co.unverified", request.mode());
                handle.complete();
            }
        }

        @Override
        public void onServerMessage(OperationHandle handle, Minecraft client, ServerMessage message) {
            if (message.origin() != MessageOrigin.SERVER_SYSTEM) return;
            if (dispatch != CommandDispatch.SENT || !request.destructive()) return;
            switch (CoreProtectResponseParser.parse(request.mode(), message.cleanText())) {
                case CONFIRMED -> {
                    ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.chat.co.confirmed", request.mode());
                    handle.complete();
                }
                case REJECTED -> {
                    ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.chat.co.rejected", request.mode());
                    handle.complete();
                }
                case UNRELATED -> { }
            }
        }

        @Override
        public void onCancelled(OperationHandle handle, Minecraft client, OperationCancelReason reason) {
            if (dispatch == CommandDispatch.BLOCKED) {
                ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.chat.command.not_sent");
            } else {
                ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.chat.co.cancelled", request.mode(), reason.name());
            }
        }
    }

    private static String normalizeList(String input) {
        return normalize(input).replaceAll("\\s*,\\s*", ",").replaceAll(",+$", "");
    }

    private static String normalize(String input) {
        return value(input).trim().toLowerCase(Locale.ROOT);
    }

    private static String value(String input) {
        return input == null ? "" : input;
    }
}
