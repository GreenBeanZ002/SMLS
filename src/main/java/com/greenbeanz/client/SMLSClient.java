package com.greenbeanz.client;

import com.greenbeanz.SMLS;
import com.greenbeanz.client.commands.RankScanCommand;
import com.greenbeanz.client.accountlink.DiscordLinkSessionValidator;
import com.greenbeanz.client.gui.SMLSHomeScreen;
import com.greenbeanz.client.gui.modules.RulebookScreen;
import com.greenbeanz.client.moderation.ModerationChatService;
import com.greenbeanz.client.moderation.ModerationScreen;
import com.greenbeanz.client.moderation.PunishmentLogService;
import com.greenbeanz.client.modules.*;
import com.greenbeanz.client.utils.CannedReplies;
import com.greenbeanz.client.utils.ChatUtils;
import com.greenbeanz.client.utils.ClientUtils;
import com.greenbeanz.client.utils.SMLSConfig;
import com.greenbeanz.client.utils.HubStaffRankDetector;
import com.greenbeanz.client.utils.UpdateChecker;
import com.greenbeanz.client.message.ServerMessageRouter;
import com.greenbeanz.client.rulebook.RulebookService;
import com.greenbeanz.client.session.CommandDispatch;
import com.greenbeanz.client.session.OperationCancelResult;
import com.greenbeanz.client.session.OperationCoordinator;
import com.greenbeanz.client.session.OperationStartResult;
import com.greenbeanz.client.session.OutboundSpamSafety;
import com.greenbeanz.client.utils.AlertWordlist;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import java.util.List;

public class SMLSClient implements ClientModInitializer {
    private static final String PREFIX = "§8[§6SMLS§8] §7";

    @Override
    public void onInitializeClient() {
        SMLS.LOGGER.info("Initializing SMLS client, you are a lazy staff member!");
        DiscordLinkSessionValidator.validateSavedLink(Minecraft.getInstance());
        ServerMessageRouter.register();
        OperationCoordinator.global().register();
        OutboundSpamSafety.register();
        registerSMLSCommand();
        registerScreenKeybinds();
        ModerationChatService.register();
        PunishmentLogService.register();
        RulebookService.shared().register();
        HubStaffRankDetector.register();
        UpdateChecker.register();
        modules().forEach(SMLSModule::register);
    }

    public static List<SMLSModule> modules() {
        return ModulesHolder.ALL;
    }

    private void registerSMLSCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(buildSMLSCommand()));
    }

    LiteralArgumentBuilder<FabricClientCommandSource> buildSMLSCommand() {
        return ClientCommands.literal("smls")
                .executes(context -> openHomeScreen(context.getSource().getClient()))
                .then(staffLiteral("rank")
                        .executes(context -> {
                            ChatUtils.sendTranslatedMessage(context.getSource().getClient(), PREFIX,
                                    "SMLS.chat.rank.current", SMLSConfig.staffRank().displayName());
                            return 1;
                        }))
                .then(ClientCommands.literal("scanrank")
                        .executes(context -> RankScanCommand.run(context.getSource().getClient())))
                .then(staffLiteral("help")
                        .executes(context -> sendHelp(context.getSource().getClient())))
                .then(staffLiteral("modview")
                        .executes(context -> openModerationScreen(context.getSource().getClient())))
                .then(staffLiteral("punish")
                        .executes(context -> {
                            Minecraft client = context.getSource().getClient();
                            client.schedule(() -> client.gui.setScreen(new RulebookScreen(null, null)));
                            return 1;
                        }))
                .then(staffLiteral("cancel")
                        .executes(context -> cancelActiveOperation(context.getSource().getClient())))
                .then(moduleLiteral("war", WarManagerModule.class)
                        .executes(context -> {
                            Minecraft client = context.getSource().getClient();
                            client.schedule(() -> module(WarManagerModule.class).openScreen(client, null));
                            return 1;
                        }))
                .then(moduleLiteral("lands", CheckLandsModule.class)
                        .then(ClientCommands.argument("igns", StringArgumentType.greedyString()).executes(context -> {
                            return module(CheckLandsModule.class).submit(context.getSource().getClient(),
                                    StringArgumentType.getString(context, "igns"))
                                    == OperationStartResult.STARTED ? 1 : 0;
                        })))
                .then(moduleLiteral("landsalts", CheckLandsAltModule.class)
                        .then(ClientCommands.argument("ign", StringArgumentType.word()).executes(context -> {
                            return module(CheckLandsAltModule.class).submit(context.getSource().getClient(),
                                    StringArgumentType.getString(context, "ign"))
                                    == OperationStartResult.STARTED ? 1 : 0;
                        })))
                .then(moduleLiteral("members", CheckMembersModule.class)
                        .then(ClientCommands.argument("land", StringArgumentType.greedyString()).executes(context -> {
                            return module(CheckMembersModule.class).submit(context.getSource().getClient(),
                                    StringArgumentType.getString(context, "land"))
                                    == OperationStartResult.STARTED ? 1 : 0;
                        })))
                .then(moduleLiteral("alts", CheckAltsModule.class)
                        .then(ClientCommands.argument("ign", StringArgumentType.word()).executes(context -> {
                            return module(CheckAltsModule.class).submit(context.getSource().getClient(),
                                    StringArgumentType.getString(context, "ign"))
                                    == OperationStartResult.STARTED ? 1 : 0;
                        })))
                .then(moduleLiteral("uuid", UuidLookupModule.class)
                        .then(ClientCommands.argument("usernames", StringArgumentType.greedyString()).executes(context -> {
                            return module(UuidLookupModule.class).submit(context.getSource().getClient(),
                                    StringArgumentType.getString(context, "usernames")) ? 1 : 0;
                        })))
                .then(moduleLiteral("xray", XrayRollbackModule.class)
                        .then(ClientCommands.literal("confirm")
                                .executes(context -> module(XrayRollbackModule.class).confirm(
                                        context.getSource().getClient()) ? 1 : 0))
                        .then(ClientCommands.literal("cancel").executes(context -> {
                            return module(XrayRollbackModule.class).cancel(context.getSource().getClient()) ? 1 : 0;
                        }))
                        .then(ClientCommands.argument("ign", StringArgumentType.word()).executes(context -> {
                            return module(XrayRollbackModule.class).stage(context.getSource().getClient(),
                                    StringArgumentType.getString(context, "ign")).staged() ? 1 : 0;
                        })))
                .then(moduleLiteral("prefix", PrefixCreateModule.class)
                        .then(ClientCommands.argument("ign", StringArgumentType.word())
                                .then(ClientCommands.argument("limit", StringArgumentType.word())
                                        .then(ClientCommands.argument("prefixid", StringArgumentType.word())
                                                .then(ClientCommands.argument("prefixtext", StringArgumentType.greedyString()).executes(context -> {
                                                    return module(PrefixCreateModule.class).submit(context.getSource().getClient(),
                                                            StringArgumentType.getString(context, "ign"), StringArgumentType.getString(context, "limit"),
                                                            StringArgumentType.getString(context, "prefixid"),
                                                            StringArgumentType.getString(context, "prefixtext")).valid() ? 1 : 0;
                                                }))))))
                .then(moduleLiteral("reimbursement", ReimbursementModule.class)
                        .executes(context -> {
                            Minecraft client = context.getSource().getClient();
                            client.schedule(() -> module(ReimbursementModule.class).openScreen(client, null));
                            return 1;
                        }))
                .then(moduleLiteral("donorpet", DonorPetModule.class)
                        .then(ClientCommands.argument("ign", StringArgumentType.word())
                                .then(ClientCommands.argument("pet", StringArgumentType.word()).executes(context -> {
                                    return module(DonorPetModule.class).submit(context.getSource().getClient(),
                                            StringArgumentType.getString(context, "ign"),
                                            StringArgumentType.getString(context, "pet"))
                                            == DonorPetModule.SubmitStatus.STARTED ? 1 : 0;
                                }))))
                .then(moduleLiteral("promowave", PromoWaveModule.class)
                        .then(ClientCommands.literal("confirm")
                                .executes(context -> module(PromoWaveModule.class).confirm(
                                        context.getSource().getClient()) ? 1 : 0))
                        .then(ClientCommands.literal("cancel").executes(context -> {
                            return module(PromoWaveModule.class).cancel(context.getSource().getClient()) ? 1 : 0;
                        }))
                        .then(ClientCommands.argument("rank", StringArgumentType.word())
                                .then(ClientCommands.argument("igns", StringArgumentType.greedyString()).executes(context -> {
                                    return module(PromoWaveModule.class).stage(context.getSource().getClient(),
                                            StringArgumentType.getString(context, "rank"),
                                            StringArgumentType.getString(context, "igns")).staged() ? 1 : 0;
                                }))))
                .then(moduleLiteral("demowave", DemoWaveModule.class)
                        .then(ClientCommands.literal("confirm")
                                .executes(context -> module(DemoWaveModule.class).confirm(
                                        context.getSource().getClient()) ? 1 : 0))
                        .then(ClientCommands.literal("cancel").executes(context -> {
                            return module(DemoWaveModule.class).cancel(context.getSource().getClient()) ? 1 : 0;
                        }))
                        .then(ClientCommands.argument("rank", StringArgumentType.word())
                                .then(ClientCommands.argument("igns", StringArgumentType.greedyString()).executes(context -> {
                                    return module(DemoWaveModule.class).stage(context.getSource().getClient(),
                                            StringArgumentType.getString(context, "rank"),
                                            StringArgumentType.getString(context, "igns")).staged() ? 1 : 0;
                                }))))
                .then(moduleLiteral("activity", ActivityWaveModule.class)
                        .then(ClientCommands.literal("cancel").executes(context -> {
                            return module(ActivityWaveModule.class).cancel(context.getSource().getClient()) ? 1 : 0;
                        }))
                        .then(ClientCommands.argument("igns", StringArgumentType.greedyString()).executes(context -> {
                            return module(ActivityWaveModule.class).submit(context.getSource().getClient(),
                                    StringArgumentType.getString(context, "igns"))
                                    == ActivityWaveModule.SubmitStatus.STARTED ? 1 : 0;
                        })))
                .then(moduleLiteral("containers", ContainerScanModule.class)
                        .then(ClientCommands.literal("cancel").executes(context -> {
                            return module(ContainerScanModule.class).cancel(context.getSource().getClient()) ? 1 : 0;
                        }))
                        .then(ClientCommands.argument("ign", StringArgumentType.word())
                                .then(ClientCommands.argument("time", StringArgumentType.word())
                                        .then(ClientCommands.argument("radius", StringArgumentType.word()).executes(context -> {
                                            return module(ContainerScanModule.class).submit(context.getSource().getClient(),
                                                    StringArgumentType.getString(context, "ign"),
                                                    StringArgumentType.getString(context, "time"),
                                                    StringArgumentType.getString(context, "radius")).accepted() ? 1 : 0;
                                        })))))
                .then(moduleLiteral("griefs", GriefScanModule.class)
                        .then(ClientCommands.literal("cancel").executes(context -> {
                            return module(GriefScanModule.class).cancel(context.getSource().getClient()) ? 1 : 0;
                        }))
                        .then(ClientCommands.argument("ign", StringArgumentType.word())
                                .then(ClientCommands.argument("time", StringArgumentType.word())
                                        .then(ClientCommands.argument("radius", StringArgumentType.word()).executes(context -> {
                                            return module(GriefScanModule.class).submit(context.getSource().getClient(),
                                                    StringArgumentType.getString(context, "ign"),
                                                    StringArgumentType.getString(context, "time"),
                                                    StringArgumentType.getString(context, "radius")).accepted() ? 1 : 0;
                                        })))))
                .then(moduleLiteral("co", CoreProtectBuilderModule.class)
                        .executes(context -> {
                            module(CoreProtectBuilderModule.class).openScreenDeferred(context.getSource().getClient());
                            return 1;
                        }))
                .then(moduleLiteral("greet", GreeterModule.class)
                        .then(ClientCommands.argument("ign", StringArgumentType.word()).executes(context -> {
                            CommandDispatch dispatch = module(GreeterModule.class).greet(
                                    context.getSource().getClient(), StringArgumentType.getString(context, "ign").trim());
                            return dispatch == CommandDispatch.BLOCKED ? 0 : 1;
                        })))
                .then(moduleLiteral("greeter", GreeterModule.class)
                        .executes(context -> {
                            GreeterModule greeter = module(GreeterModule.class);
                            ChatUtils.sendTranslatedMessage(context.getSource().getClient(), PREFIX,
                                    greeter.enabled() ? "SMLS.chat.greeter.enabled" : "SMLS.chat.greeter.disabled");
                            return 1;
                        })
                        .then(ClientCommands.literal("on").executes(context -> {
                            return module(GreeterModule.class).setEnabled(context.getSource().getClient(), true) ? 1 : 0;
                        }))
                        .then(ClientCommands.literal("off").executes(context -> {
                            return module(GreeterModule.class).setEnabled(context.getSource().getClient(), false) ? 1 : 0;
                        })))
                .then(moduleLiteral("loc", LocationsModule.class)
                        .executes(context -> {
                            module(LocationsModule.class).list(context.getSource().getClient()); return 1;
                        })
                        .then(ClientCommands.literal("list").executes(context -> {
                            module(LocationsModule.class).list(context.getSource().getClient()); return 1;
                        }))
                        .then(ClientCommands.literal("save")
                                .then(ClientCommands.argument("name", StringArgumentType.greedyString()).executes(context -> {
                                    return locationOutcomeSucceeded(module(LocationsModule.class).save(
                                            context.getSource().getClient(),
                                            StringArgumentType.getString(context, "name"))) ? 1 : 0;
                                })))
                        .then(ClientCommands.literal("tp")
                                .then(ClientCommands.argument("name", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(module(LocationsModule.class).names(), builder))
                                        .executes(context -> {
                                            return locationOutcomeSucceeded(module(LocationsModule.class).teleport(
                                                    context.getSource().getClient(),
                                                    StringArgumentType.getString(context, "name"))) ? 1 : 0;
                                        })))
                        .then(ClientCommands.literal("del")
                                .then(ClientCommands.argument("name", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(module(LocationsModule.class).names(), builder))
                                        .executes(context -> {
                                            return locationOutcomeSucceeded(module(LocationsModule.class).delete(
                                                    context.getSource().getClient(),
                                                    StringArgumentType.getString(context, "name"))) ? 1 : 0;
                                        }))))
                .then(rankLiteral("dryrun", StaffRank.ADMIN)
                        .executes(context -> {
                            ChatUtils.sendTranslatedMessage(context.getSource().getClient(), PREFIX,
                                    SMLSConfig.dryRun() ? "SMLS.chat.dry_run.status.on" : "SMLS.chat.dry_run.status.off");
                            return 1;
                        })
                        .then(ClientCommands.literal("on")
                                .executes(context -> setDryRun(context.getSource().getClient(), true)))
                        .then(ClientCommands.literal("off")
                                .executes(context -> setDryRun(context.getSource().getClient(), false))))
                .then(moduleLiteral("chatlog", ChatReplayModule.class)
                        .executes(context -> {
                            module(ChatReplayModule.class).openScreenWithFilter(context.getSource().getClient(), "");
                            return 1;
                        })
                        .then(ClientCommands.argument("filter", StringArgumentType.greedyString()).executes(context -> {
                            module(ChatReplayModule.class).openScreenWithFilter(context.getSource().getClient(),
                                    StringArgumentType.getString(context, "filter")); return 1;
                        })))
                .then(moduleLiteral("brb", AwayModule.class)
                        .executes(context -> {
                            module(AwayModule.class).status(context.getSource().getClient());
                            return 1;
                        })
                        .then(ClientCommands.literal("off").executes(context -> {
                            module(AwayModule.class).disable(context.getSource().getClient());
                            return 1;
                        }))
                        .then(ClientCommands.argument("duration", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("5m", "15m", "30m", "1h"), builder))
                                .executes(context -> {
                                    return module(AwayModule.class).startBrb(
                                            context.getSource().getClient(),
                                            StringArgumentType.getString(context, "duration")) ? 1 : 0;
                                })))
                .then(moduleLiteral("dnd", AwayModule.class)
                        .executes(context -> {
                            module(AwayModule.class).status(context.getSource().getClient());
                            return 1;
                        })
                        .then(ClientCommands.literal("on").executes(context -> {
                            module(AwayModule.class).setDnd(context.getSource().getClient(), true);
                            return 1;
                        }))
                        .then(ClientCommands.literal("off").executes(context -> {
                            module(AwayModule.class).setDnd(context.getSource().getClient(), false);
                            return 1;
                        })))
                .then(buildSayCommand())
                .then(moduleLiteral("alerts", ChatAlertsModule.class)
                        .executes(context -> {
                            Component state = Component.translatable(SMLSConfig.alertsEnabled() ? "SMLS.option.on" : "SMLS.option.off")
                                    .withStyle(SMLSConfig.alertsEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED);
                            ChatUtils.sendTranslatedMessage(context.getSource().getClient(), PREFIX,
                                    "SMLS.chat.alerts.status", state, ChatAlertsModule.wordCount());
                            return 1;
                        })
                        .then(ClientCommands.literal("on")
                                .executes(context -> setAlertsEnabled(context.getSource().getClient(), true)))
                        .then(ClientCommands.literal("off")
                                .executes(context -> setAlertsEnabled(context.getSource().getClient(), false)))
                        .then(ClientCommands.literal("reload")
                                .executes(context -> {
                                    AlertWordlist.LoadResult result = ChatAlertsModule.reloadWordlistResult();
                                    int count = result.wordCount();
                                    if (!result.successful()) {
                                        ChatUtils.sendTranslatedMessage(context.getSource().getClient(), PREFIX,
                                                "SMLS.chat.alerts.reload_failed", count);
                                        return 0;
                                    }
                                    ChatUtils.sendTranslatedMessage(context.getSource().getClient(), PREFIX,
                                            count == 1 ? "SMLS.chat.alerts.reloaded.one" : "SMLS.chat.alerts.reloaded.many", count);
                                    return 1;
                                })))
                .then(moduleLiteral("event", EventProtectModule.class)
                        .then(moduleLiteral("protect", EventProtectModule.class)
                                .executes(context -> {
                                    Minecraft client = context.getSource().getClient();
                                    client.schedule(() -> module(EventProtectModule.class).openScreen(client, null));
                                    return 1;
                                })
                                .then(ClientCommands.argument("eventName", StringArgumentType.string())
                                        .then(ClientCommands.argument("landName", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    Minecraft client = context.getSource().getClient();
                                                    EventProtectModule.BroadcastResult result = module(EventProtectModule.class)
                                                            .broadcastProtection(client,
                                                                    StringArgumentType.getString(context, "eventName"),
                                                                    StringArgumentType.getString(context, "landName"));
                                                    return reportProtectResult(client, result) ? 1 : 0;
                                                }))))
                        .then(moduleLiteral("randomtp", EventRandomTeleportModule.class)
                                .executes(context -> {
                                    Minecraft client = context.getSource().getClient();
                                    EventRandomTeleportModule.TeleportResult result =
                                            module(EventRandomTeleportModule.class).teleport(client);
                                    return reportRandomTeleportResult(client, result) ? 1 : 0;
                                }))
                        .then(moduleLiteral("powertool", EventPowerToolModule.class)
                                .executes(context -> {
                                    Minecraft client = context.getSource().getClient();
                                    client.schedule(() -> module(EventPowerToolModule.class).openScreen(client, null));
                                    return 1;
                                }))
                        .then(moduleLiteral("containerexport", EventsContainerExportModule.class)
                                .executes(context -> {
                                    Minecraft client = context.getSource().getClient();
                                    client.schedule(() -> module(EventsContainerExportModule.class).openScreen(client, null));
                                    return 1;
                                })
                                .then(containerExportCorner("pos1", true))
                                .then(containerExportCorner("pos2", false))
                                .then(ClientCommands.literal("add").executes(context -> {
                                    Minecraft client = context.getSource().getClient();
                                    EventsContainerExportModule exporter = module(EventsContainerExportModule.class);
                                    exporter.addAreaToSelection(client);
                                    ChatUtils.sendClientMessage(client, PREFIX + exporter.lastStatus().getString());
                                    return exporter.lastStatusIsError() ? 0 : 1;
                                }))
                                .then(ClientCommands.literal("clearcorners").executes(context -> {
                                    Minecraft client = context.getSource().getClient();
                                    module(EventsContainerExportModule.class).clearCorners();
                                    ChatUtils.sendClientMessage(client, PREFIX + "§7Corners cleared.");
                                    return 1;
                                })))

                        .then(moduleLiteral("simultaneous", EventSimultaneousCommandModule.class)
                                .executes(context -> {
                                    Minecraft client = context.getSource().getClient();
                                    client.schedule(() -> module(EventSimultaneousCommandModule.class).openScreen(client, null));
                                    return 1;
                                })
                                .then(ClientCommands.literal("command")
                                        .then(ClientCommands.argument("slot",
                                                        IntegerArgumentType.integer(1, EventSimultaneousCommandModule.MAX_COMMANDS))
                                                .then(ClientCommands.argument("command", StringArgumentType.greedyString())
                                                        .executes(context -> {
                                                            Minecraft client = context.getSource().getClient();
                                                            int slot = IntegerArgumentType.getInteger(context, "slot");
                                                            return module(EventSimultaneousCommandModule.class).setCommand(client, slot,
                                                                    StringArgumentType.getString(context, "command")) ? 1 : 0;
                                                        }))))
                                .then(ClientCommands.literal("run")
                                        .executes(context -> {
                                            Minecraft client = context.getSource().getClient();
                                            EventSimultaneousCommandModule.RunResult result =
                                                    module(EventSimultaneousCommandModule.class).runStored(client);
                                            return reportSimultaneousResult(client, result) ? 1 : 0;
                                        })
                                        .then(ClientCommands.argument("count",
                                                        IntegerArgumentType.integer(
                                                                EventSimultaneousCommandModule.MIN_REPEAT_COUNT,
                                                                EventSimultaneousCommandModule.MAX_REPEAT_COUNT))
                                                .executes(context -> {
                                                    Minecraft client = context.getSource().getClient();
                                                    int count = IntegerArgumentType.getInteger(context, "count");
                                                    EventSimultaneousCommandModule.RunResult result =
                                                            module(EventSimultaneousCommandModule.class).runStored(client, count);
                                                    return reportSimultaneousResult(client, result) ? 1 : 0;
                                                }))))

                        .then(moduleLiteral("lightning", EventLightningModule.class)
                                .executes(context -> {
                                    Minecraft client = context.getSource().getClient();
                                    client.schedule(() -> module(EventLightningModule.class).openScreen(client, null));
                                    return 1;
                                })
                                .then(lightningShape("ring", EventLightningModule.Shape.RING, "radius", "8"))
                                .then(lightningShape("line", EventLightningModule.Shape.LINE, "length", "8"))
                                .then(lightningShape("plus", EventLightningModule.Shape.PLUS, "length", "4"))
                                .then(lightningShape("cross", EventLightningModule.Shape.CROSS, "length", "4"))
                                .then(lightningShape("spiral", EventLightningModule.Shape.SPIRAL, "length", "16")))

                        .then(moduleLiteral("playnearby", EventPlayNearEffectModule.class)
                                .executes(context -> {
                                    Minecraft client = context.getSource().getClient();
                                    client.schedule(() -> module(EventPlayNearEffectModule.class).openScreen(client, null));
                                    return 1;
                                })
                                .then(ClientCommands.argument("effect", StringArgumentType.word())
                                        .then(ClientCommands.argument("duration", StringArgumentType.word())
                                                .then(ClientCommands.argument("amplifier", StringArgumentType.word())
                                                        .then(ClientCommands.argument("radius", StringArgumentType.word())
                                                                .executes(context -> {
                                                                    Minecraft client = context.getSource().getClient();
                                                                    EventPlayNearEffectModule.RunResult result = module(EventPlayNearEffectModule.class)
                                                                            .run(client,
                                                                                    StringArgumentType.getString(context, "effect"),
                                                                                    StringArgumentType.getString(context, "duration"),
                                                                                    StringArgumentType.getString(context, "amplifier"),
                                                                                    StringArgumentType.getString(context, "radius"));
                                                                    return reportPlayNearEffectResult(client, result) ? 1 : 0;
                                                                })))))));
    }

    private static <T extends SMLSModule> T module(Class<T> type) {
        return modules().stream().filter(type::isInstance).map(type::cast).findFirst().orElseThrow();
    }

    /** Hides and blocks every SMLS subcommand until the HUB has verified a staff rank. */
    private static LiteralArgumentBuilder<FabricClientCommandSource> staffLiteral(String name) {
        return ClientCommands.literal(name)
                .requires(source -> SMLSConfig.hasRecognizedStaffRank());
    }

    /** Dynamically hides and blocks a command until the detected staff rank meets its requirement. */
    private static LiteralArgumentBuilder<FabricClientCommandSource> rankLiteral(String name, StaffRank minimumRank) {
        return ClientCommands.literal(name)
                .requires(source -> SMLSConfig.staffRank().isAtLeast(minimumRank));
    }

    /** Uses the module's live staff and department requirements for autocomplete and parsing. */
    private static LiteralArgumentBuilder<FabricClientCommandSource> moduleLiteral(
            String name,
            Class<? extends SMLSModule> moduleType
    ) {
        return ClientCommands.literal(name)
                .requires(source -> module(moduleType).isCommandAvailable(source.getClient()));
    }

    private int cancelActiveOperation(Minecraft client) {
        OperationCancelResult result = OperationCoordinator.global().cancelActive(client);
        ChatUtils.sendTranslatedMessage(client, PREFIX,
                result == OperationCancelResult.CANCELLED
                        ? "SMLS.chat.operation.cancelled"
                        : "SMLS.chat.operation.none");
        return result == OperationCancelResult.CANCELLED ? 1 : 0;
    }

    private static boolean locationOutcomeSucceeded(LocationsModule.Outcome outcome) {
        return switch (outcome) {
            case SAVED, UPDATED, DELETED, SENT, SIMULATED -> true;
            default -> false;
        };
    }

    private LiteralArgumentBuilder<FabricClientCommandSource> buildSayCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> say = rankLiteral("say", StaffRank.HELPER)
                .executes(context -> {
                    ChatUtils.sendTranslatedMessage(context.getSource().getClient(), PREFIX,
                            "SMLS.chat.say.available", String.join(", ", CannedReplies.names()));
                    return 1;
                });

        for (String name : CannedReplies.names()) {
            say.then(ClientCommands.literal(name).executes(context -> {
                return CannedReplies.get(name)
                        .map(reply -> ClientUtils.dispatchChatMessage(
                                context.getSource().getClient(), reply).accepted() ? 1 : 0)
                        .orElse(0);
            }));
        }
        return say;
    }

    private void registerScreenKeybinds() {
        KeyMapping.Category SMLSCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("smls", "smls"));
        KeyMapping menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.SMLS.open_menu",
                InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(),
                SMLSCategory));
        KeyMapping moderationKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.SMLS.open_moderation",
                InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(),
                SMLSCategory));
        KeyMapping rulebookKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.SMLS.open_rulebook",
                InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(),
                SMLSCategory));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.consumeClick()) {
                openHomeScreen(client);
            }
            while (moderationKey.consumeClick()) {
                if (SMLSConfig.hasRecognizedStaffRank()) openModerationScreen(client);
            }
            while (rulebookKey.consumeClick()) {
                if (SMLSConfig.hasRecognizedStaffRank() && !(client.gui.screen() instanceof RulebookScreen)) {
                    client.gui.setScreen(new RulebookScreen(client.gui.screen(), null));
                }
            }
        });
    }

    private int sendHelp(Minecraft client) {
        String header = PREFIX;
        ChatUtils.sendClientMessage(client, header + ChatUtils.separatorForChatWidth(client, header));
        moduleHelpLine(client, CheckLandsModule.class,
                "/SMLS lands <ign...>", Component.translatable("SMLS.help.checklands"));
        moduleHelpLine(client, CheckLandsAltModule.class,
                "/SMLS landsalts <ign>", Component.translatable("SMLS.help.checklandsalts", StaffRank.MODERATOR.displayName()));
        moduleHelpLine(client, CheckMembersModule.class,
                "/SMLS members <land>", Component.translatable("SMLS.help.checkmembers"));
        moduleHelpLine(client, CheckAltsModule.class,
                "/SMLS alts <ign>", Component.translatable("SMLS.help.checkalts", StaffRank.MODERATOR.displayName()));
        moduleHelpLine(client, UuidLookupModule.class,
                "/SMLS uuid <username...>", Component.translatable("SMLS.help.uuid"));
        moduleHelpLine(client, XrayRollbackModule.class,
                "/SMLS xray <ign>|confirm|cancel", Component.translatable("SMLS.help.xray", StaffRank.SENIOR_MODERATOR.displayName()));
        moduleHelpLine(client, PrefixCreateModule.class,
                "/SMLS prefix <ign> <limit> <prefixid> <prefixtext>", Component.translatable("SMLS.help.prefix", StaffRank.SUPPORT.displayName()));
        moduleHelpLine(client, ReimbursementModule.class,
                "/SMLS reimbursement", Component.translatable("SMLS.help.reimbursement", StaffRank.SUPPORT.displayName()));
        moduleHelpLine(client, DonorPetModule.class,
                "/SMLS donorpet <ign> <pet>", Component.translatable("SMLS.help.donorpet", StaffRank.ADMIN.displayName()));
        moduleHelpLine(client, PromoWaveModule.class,
                "/SMLS promowave <rank> <igns>|confirm|cancel", Component.translatable("SMLS.help.promowave", StaffRank.ADMIN.displayName()));
        moduleHelpLine(client, DemoWaveModule.class,
                "/SMLS demowave <rank> <igns>|confirm|cancel", Component.translatable("SMLS.help.demowave", StaffRank.ADMIN.displayName()));
        moduleHelpLine(client, ActivityWaveModule.class,
                "/SMLS activity <igns>|cancel", Component.translatable("SMLS.help.activity", StaffRank.ADMIN.displayName()));
        helpLine(client, "/SMLS rank", Component.translatable("SMLS.help.rank"));
        moduleHelpLine(client, ChatAlertsModule.class,
                "/SMLS alerts [on|off|reload]", Component.translatable("SMLS.help.alerts"));
        moduleHelpLine(client, ChatReplayModule.class,
                "/SMLS chatlog [filter]", Component.translatable("SMLS.help.chatlog"));
        rankHelpLine(client, StaffRank.ADMIN,
                "/SMLS dryrun <on|off>", Component.translatable("SMLS.help.dryrun", StaffRank.ADMIN.displayName()));
        moduleHelpLine(client, GreeterModule.class,
                "/SMLS greet <ign>", Component.translatable("SMLS.help.greet"));
        moduleHelpLine(client, GreeterModule.class,
                "/SMLS greeter [on|off]", Component.translatable("SMLS.help.greeter"));
        moduleHelpLine(client, LocationsModule.class,
                "/SMLS loc <save|tp|del|list> [name]", Component.translatable("SMLS.help.loc"));
        moduleHelpLine(client, CoreProtectBuilderModule.class,
                "/SMLS co", Component.translatable("SMLS.help.co", StaffRank.SENIOR_MODERATOR.displayName()));
        moduleHelpLine(client, ContainerScanModule.class,
                "/SMLS containers <ign|*> <time> <radius>|cancel", Component.translatable("SMLS.help.containers", StaffRank.MODERATOR.displayName()));
        moduleHelpLine(client, GriefScanModule.class,
                "/SMLS griefs <ign|*> <time> <radius>|cancel", Component.translatable("SMLS.help.griefs", StaffRank.MODERATOR.displayName()));
        moduleHelpLine(client, AwayModule.class,
                "/SMLS brb <duration|off>", Component.translatable("SMLS.help.brb"));
        moduleHelpLine(client, AwayModule.class,
                "/SMLS dnd <on|off>", Component.translatable("SMLS.help.dnd"));
        rankHelpLine(client, StaffRank.HELPER,
                "/SMLS say [reply]", Component.translatable("SMLS.help.say"));
        helpLine(client, "/SMLS cancel", Component.translatable("SMLS.help.cancel"));
        helpLine(client, "/SMLS modview", Component.translatable("SMLS.help.modview"));
        helpLine(client, "/SMLS punish", Component.translatable("SMLS.help.punish"));
        moduleHelpLine(client, EventProtectModule.class,
                "/SMLS event protect [\"event name\" <land>]", Component.translatable("SMLS.help.eventprotect"));
        moduleHelpLine(client, EventRandomTeleportModule.class,
                "/SMLS event randomtp", Component.translatable("SMLS.help.eventrandomteleport"));
        moduleHelpLine(client, EventPowerToolModule.class,
                "/SMLS event powertool", Component.translatable("SMLS.help.eventpowertool"));
        moduleHelpLine(client, EventsContainerExportModule.class,
                "/SMLS event containerexport [pos1|pos2 [x y z]|add|clearcorners]",
                Component.translatable("SMLS.help.eventcontainerexport"));
        moduleHelpLine(client, EventSimultaneousCommandModule.class,
                "/SMLS event simultaneous [set <1-5> <command>|run]", Component.translatable("SMLS.help.eventsimultaneous"));
        moduleHelpLine(client, EventLightningModule.class,
                "/SMLS event lightning <ring|line|plus|cross|spiral> <size> [points]", Component.translatable("SMLS.help.eventlightningring"));
        moduleHelpLine(client, WarManagerModule.class,
                "/SMLS war", Component.translatable("SMLS.help.war_manager"));

        helpLine(client, "/SMLS", Component.translatable("SMLS.help.menu"));
        ChatUtils.sendClientMessage(client, "§7" + ChatUtils.separatorForChatWidth(client, ""));
        return 1;
    }

    private void helpLine(Minecraft client, String command, Component description) {
        String suggested = command.replaceAll(" [<\\[].*", "") + " ";
        ChatUtils.sendClientMessage(client, Component.literal("§8• §6" + command).withStyle(style -> style
                .withClickEvent(new ClickEvent.SuggestCommand(suggested))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("SMLS.help.hover", description)))));
    }

    private void rankHelpLine(Minecraft client, StaffRank minimumRank, String command, Component description) {
        if (SMLSConfig.staffRank().isAtLeast(minimumRank)) {
            helpLine(client, command, description);
        }
    }

    private void moduleHelpLine(Minecraft client, Class<? extends SMLSModule> moduleType,
                                String command, Component description) {
        if (module(moduleType).isAvailableToDetectedRank()) {
            helpLine(client, command, description);
        }
    }

    private int openHomeScreen(Minecraft client) {
        // next tick, otherwise the closing chat screen overrides it
        client.schedule(() -> client.gui.setScreen(new SMLSHomeScreen(modules())));
        return 1;
    }

    private int openModerationScreen(Minecraft client) {
        if (!SMLSConfig.hasRecognizedStaffRank()) return 0;
        client.schedule(() -> client.gui.setScreen(new ModerationScreen(null)));
        return 1;
    }

    private int setDryRun(Minecraft client, boolean enabled) {
        if (!SMLSConfig.staffRank().isAtLeast(StaffRank.ADMIN)) {
            ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.chat.rank.required",
                    StaffRank.ADMIN.displayName(), SMLSConfig.staffRank().displayName());
            return 0;
        }

        SMLSConfig.setDryRun(enabled);
        ChatUtils.sendTranslatedMessage(client, PREFIX,
                enabled ? "SMLS.chat.dry_run.enabled" : "SMLS.chat.dry_run.disabled");
        return 1;
    }

    private int setAlertsEnabled(Minecraft client, boolean enabled) {
        if (!SMLSConfig.setAlertsEnabled(enabled)) {
            ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.chat.config.save_failed");
            return 0;
        }
        ChatUtils.sendTranslatedMessage(client, PREFIX,
                enabled ? "SMLS.chat.alerts.enabled" : "SMLS.chat.alerts.disabled");
        return 1;
    }

    private boolean reportProtectResult(Minecraft client, EventProtectModule.BroadcastResult result) {
        return switch (result) {
            case SENT, SIMULATED -> true;
            case INVALID_EVENT -> {
                ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.validation.event_protect.name");
                yield false;
            }
            case INVALID_LAND -> {
                ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.validation.event_protect.land");
                yield false;
            }
            case RANK_BLOCKED -> false; // module already sent the rank/department message
            case SERVER_BLOCKED -> false; // module already sent the guard message
        };
    }

    private boolean reportRandomTeleportResult(
            Minecraft client,
            EventRandomTeleportModule.TeleportResult result
    ) {
        return switch (result.status()) {
            case SENT -> {
                ChatUtils.sendTranslatedMessage(client, PREFIX,
                        "SMLS.chat.event_random_teleport.teleported", result.target());
                yield true;
            }
            case QUEUED -> {
                ChatUtils.sendTranslatedMessage(client, PREFIX,
                        "SMLS.chat.event_random_teleport.queued", result.target());
                yield true;
            }
            case SIMULATED -> true; // module already sent one concise dry-run summary
            case NO_PLAYERS -> {
                ChatUtils.sendTranslatedMessage(client, PREFIX,
                        "SMLS.chat.event_random_teleport.no_players");
                yield false;
            }
            case BLOCKED -> false; // module already sent the rank, guard, or dispatch message
        };
    }
    private boolean reportSimultaneousResult(Minecraft client, EventSimultaneousCommandModule.RunResult result) {
        return switch (result) {
            case SENT -> {
                ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.chat.event_simultaneous.sent_stored");
                yield true;
            }
            case SIMULATED -> true;
            case INVALID_COMMAND_COUNT -> {
                ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.validation.event_simultaneous.stored_count");
                yield false;
            }
            case INVALID_COMMAND -> {
                ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.validation.event_simultaneous.stored_gap");
                yield false;
            }
            case INVALID_REPEAT_COUNT -> {
                ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.validation.event_simultaneous.repeat_count",
                        EventSimultaneousCommandModule.MIN_REPEAT_COUNT, EventSimultaneousCommandModule.MAX_REPEAT_COUNT);
                yield false;
            }
            case RANK_BLOCKED, SERVER_BLOCKED -> false; // module already sent that message
        };
    }

    /**
     * Builds {@code <name> <size> [points]} for one lightning shape. Points fall back to
     * {@code defaultPoints} when the staff member leaves them out.
     */
    private LiteralArgumentBuilder<FabricClientCommandSource> lightningShape(
            String name, EventLightningModule.Shape shape, String sizeArgument, String defaultPoints) {
        return ClientCommands.literal(name)
                .then(ClientCommands.argument(sizeArgument, StringArgumentType.word())
                        .executes(context -> runLightning(context.getSource().getClient(), shape,
                                StringArgumentType.getString(context, sizeArgument), defaultPoints))
                        .then(ClientCommands.argument("points", StringArgumentType.word())
                                .executes(context -> runLightning(context.getSource().getClient(), shape,
                                        StringArgumentType.getString(context, sizeArgument),
                                        StringArgumentType.getString(context, "points")))));
    }
    private LiteralArgumentBuilder<FabricClientCommandSource> containerExportCorner(String name, boolean first) {
        return ClientCommands.literal(name)
                .executes(context -> {
                    Minecraft client = context.getSource().getClient();
                    if (client.player == null) return 0;
                    return setContainerExportCorner(client, first, client.player.blockPosition());
                })
                .then(ClientCommands.argument("x", IntegerArgumentType.integer())
                        .then(ClientCommands.argument("y", IntegerArgumentType.integer())
                                .then(ClientCommands.argument("z", IntegerArgumentType.integer())
                                        .executes(context -> setContainerExportCorner(
                                                context.getSource().getClient(), first,
                                                new BlockPos(
                                                        IntegerArgumentType.getInteger(context, "x"),
                                                        IntegerArgumentType.getInteger(context, "y"),
                                                        IntegerArgumentType.getInteger(context, "z")))))));
    }

    private int setContainerExportCorner(Minecraft client, boolean first, BlockPos pos) {
        EventsContainerExportModule exporter = module(EventsContainerExportModule.class);
        exporter.setCorner(first, pos);
        ChatUtils.sendClientMessage(client, PREFIX + exporter.lastStatus().getString());
        return 1;
    }

    private int runLightning(Minecraft client, EventLightningModule.Shape shape, String size, String points) {
        EventLightningModule.RunResult result = module(EventLightningModule.class).run(client, shape, size, points);
        return reportLightningResult(client, shape, result) ? 1 : 0;
    }

    private boolean reportLightningResult(Minecraft client, EventLightningModule.Shape shape,
                                          EventLightningModule.RunResult result) {
        return switch (result) {
            case SENT -> {
                ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.chat.event_lightning_ring.sent");
                yield true;
            }
            case SIMULATED -> true;
            case INVALID_RADIUS -> {
                ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.validation.event_lightning_ring.radius",
                        EventLightningModule.MIN_RADIUS, EventLightningModule.MAX_RADIUS);
                yield false;
            }
            case INVALID_LENGTH -> {
                ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.validation.event_lightning_ring.length",
                        EventLightningModule.MIN_LENGTH, EventLightningModule.MAX_LENGTH);
                yield false;
            }
            case INVALID_POINTS -> {
                ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.validation.event_lightning_ring.points",
                        EventLightningModule.MIN_POINTS, shape.maxPoints());
                yield false;
            }
            case RANK_BLOCKED, SERVER_BLOCKED -> false; // module already sent that message
        };
    }

    private boolean reportPlayNearEffectResult(Minecraft client, EventPlayNearEffectModule.RunResult result) {
        return switch (result) {
            case SENT -> {
                ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.chat.event_play_near_effect.sent");
                yield true;
            }
            case SIMULATED -> true;
            case INVALID_EFFECT -> {
                ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.validation.event_play_near_effect.effect");
                yield false;
            }
            case INVALID_DURATION -> {
                ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.validation.event_play_near_effect.duration");
                yield false;
            }
            case INVALID_AMPLIFIER -> {
                ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.validation.event_play_near_effect.amplifier");
                yield false;
            }
            case INVALID_RADIUS -> {
                ChatUtils.sendTranslatedMessage(client, PREFIX, "SMLS.validation.event_play_near_effect.radius");
                yield false;
            }
            case RANK_BLOCKED, SERVER_BLOCKED -> false; // module already sent that message
        };
    }

    /** Defers config-backed module construction until normal client initialization. */
    private static final class ModulesHolder {
        private static final List<SMLSModule> ALL = List.of(
                new ActivityWaveModule(),
                new AwayModule(),
                new BackgroundsModule(),
                new ChatAlertsModule(),
                new ChatReplayModule(),
                new ChatSpamMuteModule(),
                new CheckAltsModule(),
                new CheckLandsModule(),
                new CheckLandsAltModule(),
                new CheckMembersModule(),
                new ContainerScanModule(),
                new CoreProtectBuilderModule(),
                new DemoWaveModule(),
                new DonorPetModule(),
                new EventPowerToolModule(),
                new EventProtectModule(),
                new EventRandomTeleportModule(),
                new EventSimultaneousCommandModule(),
                new EventPlayNearEffectModule(),
                new EventLightningModule(),
                new EventsContainerExportModule(),
                new GreeterModule(),
                new GriefScanModule(),
                new LocationsModule(),
                new MiniMeHudModule(),
                new PrefixCreateModule(),
                new ReimbursementModule(),
                new PromoWaveModule(),
                new UuidLookupModule(),
                new WarManagerModule(),
                new XrayRollbackModule()
        );
    }
}