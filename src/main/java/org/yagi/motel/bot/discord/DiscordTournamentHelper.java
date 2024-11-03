package org.yagi.motel.bot.discord;

import static org.yagi.motel.bot.discord.commands.DiscordCommandsFactory.createLogCommand;
import static org.yagi.motel.bot.discord.commands.DiscordCommandsFactory.createMeCommand;
import static org.yagi.motel.bot.discord.commands.DiscordCommandsFactory.createStatusCommand;
import static org.yagi.motel.bot.discord.commands.DiscordCommandsFactory.createUpdateTeamsCommand;
import static org.yagi.motel.bot.discord.utils.DiscordChannelUtils.getRequiredLangFromChannel;
import static org.yagi.motel.bot.discord.utils.DiscordCommandPermissionsProvider.getAddCommandPermissions;
import static org.yagi.motel.bot.discord.utils.DiscordCommandPermissionsProvider.getCloseRegistrationCommandPermissions;
import static org.yagi.motel.bot.discord.utils.DiscordCommandPermissionsProvider.getLogCommandPermissions;
import static org.yagi.motel.bot.discord.utils.DiscordCommandPermissionsProvider.getMeCommandPermissions;
import static org.yagi.motel.bot.discord.utils.DiscordCommandPermissionsProvider.getStartRegistrationCommandPermissions;
import static org.yagi.motel.bot.discord.utils.DiscordCommandPermissionsProvider.getStartServeCommandPermissions;
import static org.yagi.motel.bot.discord.utils.DiscordCommandPermissionsProvider.getStatusCommandPermissions;
import static org.yagi.motel.bot.discord.utils.DiscordCommandPermissionsProvider.getStopServeCommandPermissions;
import static org.yagi.motel.bot.discord.utils.DiscordCommandPermissionsProvider.getUpdateTeamsCommandPermissions;

import akka.actor.ActorRef;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.yagi.motel.bot.discord.utils.DiscordInteractionUtils;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.handler.AddCommandHandler;
import org.yagi.motel.handler.CloseRegistrationCommandHandler;
import org.yagi.motel.handler.CommandHandler;
import org.yagi.motel.handler.LogCommandHandler;
import org.yagi.motel.handler.MeCommandHandler;
import org.yagi.motel.handler.StartRegistrationCommandHandler;
import org.yagi.motel.handler.StartServeCommandHandler;
import org.yagi.motel.handler.StatusCommandHandler;
import org.yagi.motel.handler.StopServeCommandHandler;
import org.yagi.motel.handler.UpdateTeamsCommandHandler;
import org.yagi.motel.handler.context.CommandContext;
import org.yagi.motel.handler.context.HandlerErrorContext;
import org.yagi.motel.handler.context.ReplyCallbackContext;
import org.yagi.motel.handler.holder.PlatformCallbacksHolder;
import org.yagi.motel.kernel.enums.PlatformType;
import org.yagi.motel.kernel.model.container.ResultCommandContainer;
import org.yagi.motel.kernel.model.enums.IsProcessedState;
import org.yagi.motel.kernel.repository.StateRepository;
import reactor.core.publisher.Mono;

@Slf4j
@SuppressWarnings({"checkstyle:LineLength", "checkstyle:MissingJavadocType"})
public class DiscordTournamentHelper implements Runnable {

  private static final String WHITESPACE_STR = " ";
  private static final String SYNC_COMMAND_PREFIX = "/sync";
  private static final Integer COMMAND_UNIQUE_ID_LENGTH = 10;

  private final ExecutorService discordExecutor;
  private final DiscordClient client;
  private final AppConfig config;
  private final StateRepository stateRepository;
  private final Map<String, CommandHandler> handlers;
  private final BlockingQueue<ResultCommandContainer> messagesQueue;

  // todo: clear by timeout for TTL event
  private final Map<Long, ApplicationCommandInteractionEvent> eventCache;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public DiscordTournamentHelper(
      ActorRef commandDispatcherActor,
      ActorRef errorCommandDispatcherActor,
      AppConfig config,
      StateRepository stateRepository,
      BlockingQueue<ResultCommandContainer> messagesQueue) {
    this.discordExecutor = Executors.newFixedThreadPool(2);
    this.client = DiscordClient.create(config.getDiscord().getDiscordBotToken());
    this.config = config;
    this.stateRepository = stateRepository;
    this.messagesQueue = messagesQueue;
    final List<Object> args = new ArrayList<>();
    args.add(config);
    args.add(commandDispatcherActor);
    args.add(errorCommandDispatcherActor);
    args.add(registerCallbacks());

    this.handlers = registerHandlers(args, config);
    this.eventCache = new ConcurrentHashMap<>();
  }

  @SuppressWarnings({"checkstyle:OperatorWrap", "checkstyle:MissingJavadocMethod"})
  public void start() {
    discordExecutor.execute(
        () -> {
          Mono<Void> login =
              client.withGateway(
                  (GatewayDiscordClient gateway) ->
                      gateway.on(
                          MessageCreateEvent.class,
                          event -> {
                            processMessageEvent(event);
                            return Mono.empty();
                          }));
          login.block();
        });

    discordExecutor.execute(
        () -> {
          Mono<Void> login =
              client.withGateway(
                  (GatewayDiscordClient gateway) ->
                      gateway.on(
                          ApplicationCommandInteractionEvent.class,
                          event -> {
                            event.deferReply().block();
                            processCommandEvent(event);
                            return Mono.empty();
                          }));
          login.block();
        });
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public void sendNotification(String message, Long chatId) {
    client.getChannelById(Snowflake.of(chatId)).createMessage(message).block();
  }

  private void processMessageEvent(final MessageCreateEvent event) {
    Message message = event.getMessage();
    if (message.getAuthor().isPresent()) {
      final Long chatId = message.getChannelId().asLong();
      if (chatId.equals(config.getDiscord().getDiscordAdminChatId())) {
        User user = message.getAuthor().get();
        if (user.getId().asLong() != config.getDiscord().getDiscordBotUserId()) {
          String inputText = StringUtils.normalizeSpace(message.getContent());
          String[] commandArgs = inputText.split(WHITESPACE_STR);
          if (commandArgs.length >= 1) {
            String commandPrefix = StringUtils.normalizeSpace(commandArgs[0]);
            if (SYNC_COMMAND_PREFIX.equals(commandPrefix)) {
              initCommands(client, config);
              sendNotification("All command synced!", chatId);
            }

            if (handlers.containsKey(commandPrefix)) {
              handlers
                  .get(commandPrefix)
                  .handleCommand(
                      CommandContext.builder()
                          .commandUniqueId(
                              Long.valueOf(
                                  RandomStringUtils.randomNumeric(COMMAND_UNIQUE_ID_LENGTH)))
                          .commandArgs(commandArgs)
                          .senderChatId(chatId)
                          .username(user.getMention())
                          .platformType(PlatformType.DISCORD)
                          .build());
            }
          }
        }
      }
    }
  }

  private void processCommandEvent(final ApplicationCommandInteractionEvent event) {
    Interaction interaction = event.getInteraction();
    if (interaction.getMember().isPresent()) {
      final Long chatId = interaction.getChannelId().asLong();
      Optional<IsProcessedState> isProcessedState = stateRepository.getIsProcessedState();
      if (chatId.equals(config.getDiscord().getDiscordAdminChatId())
          || (isProcessedState.isPresent() && IsProcessedState.ENABLE == isProcessedState.get())) {
        Member user = interaction.getMember().get();
        if (user.getId().asLong() != config.getDiscord().getDiscordBotUserId()) {
          Optional<String> commandName =
              DiscordInteractionUtils.extractInteractionName(interaction);
          if (commandName.isPresent()) {
            String commandPrefix = "/" + commandName.get();
            String[] commandArgs =
                DiscordInteractionUtils.prepareHandlerArgs(
                    commandPrefix, DiscordInteractionUtils.extractArgsFromInteraction(interaction));
            if (commandArgs.length >= 1) {
              if (handlers.containsKey(commandPrefix)) {
                final Long eventKey =
                    Long.valueOf(RandomStringUtils.randomNumeric(COMMAND_UNIQUE_ID_LENGTH));
                eventCache.putIfAbsent(eventKey, event);
                handlers
                    .get(commandPrefix)
                    .handleCommand(
                        CommandContext.builder()
                            .commandUniqueId(eventKey)
                            .commandArgs(commandArgs)
                            .senderChatId(chatId)
                            .username(user.getNicknameMention())
                            .platformType(PlatformType.DISCORD)
                            .requestedResponseLang(getRequiredLangFromChannel(chatId, config))
                            .build());
              }
            }
          }
        }
      }
    }
  }

  private PlatformCallbacksHolder registerCallbacks() {
    return PlatformCallbacksHolder.builder()
        .platformErrorMessageSupplier(prepareErrorMessageSupplier())
        .platformAdminChatIdSupplier(prepareGetAdminChatIdSupplier())
        .build();
  }

  private Map<String, CommandHandler> registerHandlers(final List<Object> args, AppConfig config) {
    final Map<String, CommandHandler> handlers = new HashMap<>();
    handlers.putIfAbsent(
        "/log",
        registerCommandHandler(
            LogCommandHandler.class, getHandlerArgs(args, getLogCommandPermissions(config))));
    handlers.putIfAbsent(
        "/start_serve",
        registerCommandHandler(
            StartServeCommandHandler.class,
            getHandlerArgs(args, getStartServeCommandPermissions(config))));
    handlers.putIfAbsent(
        "/stop_serve",
        registerCommandHandler(
            StopServeCommandHandler.class,
            getHandlerArgs(args, getStopServeCommandPermissions(config))));
    handlers.putIfAbsent(
        "/start_registration",
        registerCommandHandler(
            StartRegistrationCommandHandler.class,
            getHandlerArgs(args, getStartRegistrationCommandPermissions(config))));
    handlers.putIfAbsent(
        "/close_registration",
        registerCommandHandler(
            CloseRegistrationCommandHandler.class,
            getHandlerArgs(args, getCloseRegistrationCommandPermissions(config))));
    handlers.putIfAbsent(
        "/me",
        registerCommandHandler(
            MeCommandHandler.class, getHandlerArgs(args, getMeCommandPermissions(config))));
    handlers.putIfAbsent(
        "/add",
        registerCommandHandler(
            AddCommandHandler.class, getHandlerArgs(args, getAddCommandPermissions(config))));
    handlers.putIfAbsent(
        "/status",
        registerCommandHandler(
            StatusCommandHandler.class, getHandlerArgs(args, getStatusCommandPermissions(config))));
    handlers.putIfAbsent(
        "/update_teams",
        registerCommandHandler(
            UpdateTeamsCommandHandler.class, getHandlerArgs(args, getUpdateTeamsCommandPermissions(config))));
    return Collections.unmodifiableMap(handlers);
  }

  private Object[] getHandlerArgs(final List<Object> args, final Set<Long> allowedChatIds) {
    List<Object> currentArgs = new ArrayList<>(args);
    currentArgs.add(allowedChatIds);
    return currentArgs.toArray();
  }

  private CommandHandler registerCommandHandler(
      Class<? extends CommandHandler> handlerClazz, Object[] args) {
    try {
      return handlerClazz
          .getDeclaredConstructor(
              AppConfig.class,
              ActorRef.class,
              ActorRef.class,
              PlatformCallbacksHolder.class,
              Set.class)
          .newInstance(args);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private Supplier<Long> prepareGetAdminChatIdSupplier() {
    return () -> config.getDiscord().getDiscordAdminChatId();
  }

  private Consumer<Optional<ReplyCallbackContext>> prepareSendMessageCallback() {
    return replyCallbackContext -> {
      replyCallbackContext.ifPresent(
          callbackContext -> {
            if (callbackContext.getReplyCallback() != null) {
              callbackContext.getReplyCallback().accept(callbackContext.getText());
            } else {
              sendNotification(callbackContext.getText(), callbackContext.getTargetChatId());
            }
          });
    };
  }

  @SuppressWarnings("checkstyle:MissingSwitchDefault")
  private Function<HandlerErrorContext, Optional<String>> prepareErrorMessageSupplier() {
    return handlerErrorContext -> {
      switch (handlerErrorContext.getErrorType()) {
        case COMMAND_NOT_ALLOWED:
          return Optional.of(
              String.format(
                  "%s, command not allowed on this channel or for you!",
                  handlerErrorContext.getContext().getUsername()));
        case MISSED_USERNAME:
          return Optional.of("Username is missed!");
        case MISSED_PLATFORM_USERNAME:
          return Optional.of("Discord username not found!");
      }
      return Optional.empty();
    };
  }

  private void sendEventReply(ApplicationCommandInteractionEvent event, String replyMessage) {
    event.editReply(replyMessage).block();
  }

  private void sendMessage(ResultCommandContainer resultCommandContainer) {
    client
        .getChannelById(Snowflake.of(resultCommandContainer.getReplyChatId()))
        .createMessage(resultCommandContainer.getResultMessage())
        .block();
  }

  private void initCommands(DiscordClient client, AppConfig config) {
    Map<String, ApplicationCommandData> botCommands =
        client
            .getApplicationService()
            .getGlobalApplicationCommands(config.getDiscord().getApplicationId())
            .collectMap(ApplicationCommandData::name)
            .block();
    if (botCommands != null) {
      botCommands
          .values()
          .forEach(
              command ->
                  client
                      .getApplicationService()
                      .deleteGlobalApplicationCommand(
                          config.getDiscord().getApplicationId(), command.id().asLong())
                      .block());
    }

    List<ApplicationCommandRequest> commands =
        Arrays.asList(
            createStatusCommand(client, config),
            createMeCommand(client, config),
            createLogCommand(client, config),
            createUpdateTeamsCommand(client, config));

    client
        .getApplicationService()
        .bulkOverwriteGlobalApplicationCommand(config.getDiscord().getApplicationId(), commands)
        .blockFirst();
  }

  @Override
  public void run() {
    do {
      try {
        ResultCommandContainer resultCommandContainer = messagesQueue.take();
        switch (resultCommandContainer.getPlatformType()) {
          case DISCORD:
            ApplicationCommandInteractionEvent event =
                resultCommandContainer.getUniqueMessageId() != null
                    ? eventCache.remove(resultCommandContainer.getUniqueMessageId())
                    : null;
            if (event != null) {
              sendEventReply(event, resultCommandContainer.getResultMessage());
            } else {
              sendMessage(resultCommandContainer);
            }
            break;
          default:
            break;
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    } while (!Thread.interrupted());
  }
}
