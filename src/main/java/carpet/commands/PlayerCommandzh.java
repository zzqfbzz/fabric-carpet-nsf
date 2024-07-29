package carpet.commands;

import carpet.CarpetSettings;
import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.helpers.EntityPlayerActionPack.Action;
import carpet.helpers.EntityPlayerActionPack.ActionType;
import carpet.patches.EntityPlayerMPFake;
import carpet.utils.CommandHelper;
import carpet.utils.Messenger;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

public class PlayerCommandzh
{
    // TODO: allow any order like execute
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext)
    {
        LiteralArgumentBuilder<CommandSourceStack> command = literal("假人")
                .requires((player) -> CommandHelper.canUseCommand(player, CarpetSettings.commandPlayer))
                .then(argument("假人", StringArgumentType.word())
                        .suggests((c, b) -> suggest(getPlayerSuggestions(c.getSource()), b))
                        .then(literal("停").executes(manipulation(EntityPlayerActionPack::stopAll)))
                        .then(makeActionCommand("用", ActionType.USE))
                        .then(makeActionCommand("跳", ActionType.JUMP))
                        .then(makeActionCommand("打", ActionType.ATTACK))
                        .then(makeActionCommand("丢", ActionType.DROP_ITEM))
                        .then(makeDropCommand("丢", false))
                        .then(makeActionCommand("全丢", ActionType.DROP_STACK))
                        .then(makeDropCommand("全丢", true))
                        .then(makeActionCommand("换手", ActionType.SWAP_HANDS))
                        .then(literal("快捷栏")
                                .then(argument("槽", IntegerArgumentType.integer(1, 9))
                                        .executes(c -> manipulate(c, ap -> ap.setSlot(IntegerArgumentType.getInteger(c, "slot"))))))
                        .then(literal("死").executes(PlayerCommandzh::kill))
                        .then(literal("顶号"). executes(PlayerCommandzh::shadow))
                        .then(literal("骑").executes(manipulation(ap -> ap.mount(true)))
                                .then(literal("任何").executes(manipulation(ap -> ap.mount(false)))))
                        .then(literal("下马").executes(manipulation(EntityPlayerActionPack::dismount)))
                        .then(literal("潜行").executes(manipulation(ap -> ap.setSneaking(true))))
                        .then(literal("取消潜行").executes(manipulation(ap -> ap.setSneaking(false))))
                        .then(literal("疾跑").executes(manipulation(ap -> ap.setSprinting(true))))
                        .then(literal("取消疾跑").executes(manipulation(ap -> ap.setSprinting(false))))
                        .then(literal("看")
                                .then(literal("北").executes(manipulation(ap -> ap.look(Direction.NORTH))))
                                .then(literal("南").executes(manipulation(ap -> ap.look(Direction.SOUTH))))
                                .then(literal("东").executes(manipulation(ap -> ap.look(Direction.EAST))))
                                .then(literal("西").executes(manipulation(ap -> ap.look(Direction.WEST))))
                                .then(literal("上").executes(manipulation(ap -> ap.look(Direction.UP))))
                                .then(literal("下").executes(manipulation(ap -> ap.look(Direction.DOWN))))
                                .then(literal("朝向").then(argument("位置", Vec3Argument.vec3())
                                        .executes(c -> manipulate(c, ap -> ap.lookAt(Vec3Argument.getVec3(c, "位置"))))))
                                .then(argument("方向", RotationArgument.rotation())
                                        .executes(c -> manipulate(c, ap -> ap.look(RotationArgument.getRotation(c, "方向").getRotation(c.getSource())))))
                        ).then(literal("转动")
                                .then(literal("左转").executes(manipulation(ap -> ap.turn(-90, 0))))
                                .then(literal("右转").executes(manipulation(ap -> ap.turn(90, 0))))
                                .then(literal("转身").executes(manipulation(ap -> ap.turn(180, 0))))
                                .then(argument("旋转", RotationArgument.rotation())
                                        .executes(c -> manipulate(c, ap -> ap.turn(RotationArgument.getRotation(c, "旋转").getRotation(c.getSource())))))
                        ).then(literal("移动").executes(manipulation(EntityPlayerActionPack::stopMovement))
                                .then(literal("前进").executes(manipulation(ap -> ap.setForward(1))))
                                .then(literal("后退").executes(manipulation(ap -> ap.setForward(-1))))
                                .then(literal("左移").executes(manipulation(ap -> ap.setStrafing(1))))
                                .then(literal("右移").executes(manipulation(ap -> ap.setStrafing(-1))))
                        ).then(literal("召唤").executes(PlayerCommandzh::spawn)
                                .then(literal("以").requires((player) -> player.hasPermission(2))
                                        .then(argument("游戏模式", GameModeArgument.gameMode())
                                        .executes(PlayerCommandzh::spawn)))
                                .then(literal("在").then(argument("位置", Vec3Argument.vec3()).executes(PlayerCommandzh::spawn)
                                        .then(literal("面对").then(argument("方向", RotationArgument.rotation()).executes(PlayerCommandzh::spawn)
                                                .then(literal("以").then(argument("维度", DimensionArgument.dimension()).executes(PlayerCommandzh::spawn)
                                                        .then(literal("以").requires((player) -> player.hasPermission(2))
                                                                .then(argument("游戏模式", GameModeArgument.gameMode())
                                                                .executes(PlayerCommandzh::spawn)
                                                        )))
                                        )))
                                ))
                        )
                );
        dispatcher.register(command);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> makeActionCommand(String actionName, ActionType type)
    {
        return literal(actionName)
                .executes(manipulation(ap -> ap.start(type, Action.once())))
                .then(literal("一次").executes(manipulation(ap -> ap.start(type, Action.once()))))
                .then(literal("持续").executes(manipulation(ap -> ap.start(type, Action.continuous()))))
                .then(literal("间隔").then(argument("刻", IntegerArgumentType.integer(1))
                        .executes(c -> manipulate(c, ap -> ap.start(type, Action.interval(IntegerArgumentType.getInteger(c, "刻")))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> makeDropCommand(String actionName, boolean dropAll)
    {
        return literal(actionName)
                .then(literal("全部").executes(manipulation(ap -> ap.drop(-2, dropAll))))
                .then(literal("主手").executes(manipulation(ap -> ap.drop(-1, dropAll))))
                .then(literal("副手").executes(manipulation(ap -> ap.drop(40, dropAll))))
                .then(argument("槽", IntegerArgumentType.integer(0, 40)).
                        executes(c -> manipulate(c, ap -> ap.drop(IntegerArgumentType.getInteger(c, "槽"), dropAll))));
    }

    private static Collection<String> getPlayerSuggestions(CommandSourceStack source)
    {
        Set<String> players = new LinkedHashSet<>(List.of("Steve", "Alex"));
        players.addAll(source.getOnlinePlayerNames());
        return players;
    }

    private static ServerPlayer getPlayer(CommandContext<CommandSourceStack> context)
    {
        String playerName = StringArgumentType.getString(context, "假人");
        MinecraftServer server = context.getSource().getServer();
        return server.getPlayerList().getPlayerByName(playerName);
    }

    private static boolean cantManipulate(CommandContext<CommandSourceStack> context)
    {
        Player player = getPlayer(context);
        CommandSourceStack source = context.getSource();
        if (player == null)
        {
            Messenger.m(source, "r 只能操控现有玩家");
            return true;
        }
        Player sender = source.getPlayer();
        if (sender == null)
        {
            return false;
        }

        if (!source.getServer().getPlayerList().isOp(sender.getGameProfile()))
        {
            if (sender != player && !(player instanceof EntityPlayerMPFake))
            {
                Messenger.m(source, "r 非OP玩家不能控制其他真实玩家");
                return true;
            }
        }
        return false;
    }

    private static boolean cantReMove(CommandContext<CommandSourceStack> context)
    {
        if (cantManipulate(context)) return true;
        Player player = getPlayer(context);
        if (player instanceof EntityPlayerMPFake) return false;
        Messenger.m(context.getSource(), "r 只有假人可以被移动或杀死");
        return true;
    }

    private static boolean cantSpawn(CommandContext<CommandSourceStack> context)
    {
        String playerName = StringArgumentType.getString(context, "假人");
        MinecraftServer server = context.getSource().getServer();
        PlayerList manager = server.getPlayerList();

        if (manager.getPlayerByName(playerName) != null)
        {
            Messenger.m(context.getSource(), "r 假人 ", "rb " + playerName, "r  已经登录");
            return true;
        }
        GameProfile profile = server.getProfileCache().get(playerName).orElse(null);
        if (profile == null)
        {
            if (!CarpetSettings.allowSpawningOfflinePlayers)
            {
                Messenger.m(context.getSource(), "r 假人 "+playerName+" 被Mojang禁止，或者认证服务器关闭。被禁止的玩家只能在单人游戏和离线模式的服务器中被召唤。");
                return true;
            } else {
                profile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(playerName), playerName);
            }
        }
        if (manager.getBans().isBanned(profile))
        {
            Messenger.m(context.getSource(), "r 玩家 ", "rb " + playerName, "r  被服务器禁止");
            return true;
        }
        if (manager.isUsingWhitelist() && manager.isWhiteListed(profile) && !context.getSource().hasPermission(2))
        {
            Messenger.m(context.getSource(), "r 只有OP才能生成白名单玩家");
            return true;
        }
        return false;
    }

    private static int kill(CommandContext<CommandSourceStack> context)
    {
        if (cantReMove(context)) return 0;
        getPlayer(context).kill();
        return 1;
    }

    @FunctionalInterface
    interface SupplierWithCSE<T>
    {
        T get() throws CommandSyntaxException;
    }

    private static <T> T getArgOrDefault(SupplierWithCSE<T> getter, T defaultValue) throws CommandSyntaxException
    {
        try
        {
            return getter.get();
        }
        catch (IllegalArgumentException e)
        {
            return defaultValue;
        }
    }

    private static int spawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        if (cantSpawn(context)) return 0;

        CommandSourceStack source = context.getSource();
        Vec3 pos = getArgOrDefault(
                () -> Vec3Argument.getVec3(context, "位置"),
                source.getPosition()
        );
        Vec2 facing = getArgOrDefault(
                () -> RotationArgument.getRotation(context, "方向").getRotation(source),
                source.getRotation()
        );
        ResourceKey<Level> dimType = getArgOrDefault(
                () -> DimensionArgument.getDimension(context, "维度").dimension(),
                source.getLevel().dimension()
        );
        GameType mode = GameType.CREATIVE;
        boolean flying = false;
        if (source.getEntity() instanceof ServerPlayer sender)
        {
            mode = sender.gameMode.getGameModeForPlayer();
            flying = sender.getAbilities().flying;
        }
        try {
            mode = GameModeArgument.getGameMode(context, "游戏模式");
        } catch (IllegalArgumentException notPresent) {}

        if (mode == GameType.SPECTATOR)
        {
            // 强制将飞行状态设置为真，以避免观察者玩家掉出世界。
            flying = true;
        } else if (mode.isSurvival())
        {
            // 强制将飞行状态设置为假，以避免生存模式玩家飞行。
            flying = false;
        }
        String playerName = StringArgumentType.getString(context, "假人");
        if (playerName.length() > maxNameLength(source.getServer()))
        {
            Messenger.m(source, "rb 玩家名称: " + playerName + " 过长");
            return 0;
        }

        if (!Level.isInSpawnableBounds(BlockPos.containing(pos)))
        {
            Messenger.m(source, "rb 玩家 " + playerName + " 无法在世界之外被放置");
            return 0;
        }
        boolean success = EntityPlayerMPFake.createFake(playerName, source.getServer(), pos, facing.y, facing.x, dimType, mode, flying);
        if (!success) {
            Messenger.m(source, "rb 玩家 " + playerName + " 不存在且无法在在线模式中生成。请将服务器切换到离线模式或启用 allowSpawningOfflinePlayers 以生成不存在的玩家");
            return 0;
        };
        return 1;
    }

    private static int maxNameLength(MinecraftServer server)
    {
        return server.getPort() >= 0 ? SharedConstants.MAX_PLAYER_NAME_LENGTH : 40;
    }

    private static int manipulate(CommandContext<CommandSourceStack> context, Consumer<EntityPlayerActionPack> action)
    {
        if (cantManipulate(context)) return 0;
        ServerPlayer player = getPlayer(context);
        action.accept(((ServerPlayerInterface) player).getActionPack());
        return 1;
    }

    private static Command<CommandSourceStack> manipulation(Consumer<EntityPlayerActionPack> action)
    {
        return c -> manipulate(c, action);
    }

    private static int shadow(CommandContext<CommandSourceStack> context)
    {
        if (cantManipulate(context)) return 0;

        ServerPlayer player = getPlayer(context);
        if (player instanceof EntityPlayerMPFake)
        {
            Messenger.m(context.getSource(), "r 无法对假人进行影像");
            return 0;
        }
        if (player.getServer().isSingleplayerOwner(player.getGameProfile())) {
            Messenger.m(context.getSource(), "r 无法跟随单人服务器所有者");
            return 0;
        }

        EntityPlayerMPFake.createShadow(player.server, player);
        return 1;
    }
}
