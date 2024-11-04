package org.yagi.motel.bot.telegram;

import static org.yagi.motel.bot.telegram.utils.TelegramCommandPermissionsProvider.getAddCommandPermissions;
import static org.yagi.motel.bot.telegram.utils.TelegramCommandPermissionsProvider.getCloseRegistrationCommandPermissions;
import static org.yagi.motel.bot.telegram.utils.TelegramCommandPermissionsProvider.getMeCommandPermissions;
import static org.yagi.motel.bot.telegram.utils.TelegramCommandPermissionsProvider.getStartRegistrationCommandPermissions;
import static org.yagi.motel.bot.telegram.utils.TelegramCommandPermissionsProvider.getStartServeCommandPermissions;
import static org.yagi.motel.bot.telegram.utils.TelegramCommandPermissionsProvider.getStopServeCommandPermissions;
import static org.yagi.motel.bot.telegram.utils.TelegramCommandPermissionsProvider.getUpdateTeamsCommandPermissions;

import akka.actor.ActorRef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
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
import org.yagi.motel.handler.holder.PlatformCallbacksHolder;
import org.yagi.motel.kernel.enums.PlatformType;
import org.yagi.motel.kernel.model.container.ResultCommandContainer;
import org.yagi.motel.kernel.model.enums.GamePlatformType;
import org.yagi.motel.kernel.model.enums.IsProcessedState;
import org.yagi.motel.kernel.model.enums.Lang;
import org.yagi.motel.kernel.repository.StateRepository;

@Slf4j
@SuppressWarnings("checkstyle:MissingJavadocType")
public class TgTournamentHelper extends TelegramLongPollingBot implements Runnable {
  private static final String WHITESPACE_STR = " ";
  private static final Integer COMMAND_UNIQUE_ID_LENGTH = 10;
  private final Map<String, CommandHandler> handlers;
  private final AppConfig config;
  private final StateRepository stateRepository;
  private final BlockingQueue<ResultCommandContainer> messagesQueue;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public TgTournamentHelper(
      ActorRef commandDispatcherActor,
      ActorRef errorCommandDispatcherActor,
      AppConfig config,
      StateRepository stateRepository,
      BlockingQueue<ResultCommandContainer> messagesQueue) {
    this.config = config;
    this.stateRepository = stateRepository;
    this.messagesQueue = messagesQueue;
    final List<Object> args = new ArrayList<>();
    args.add(config);
    args.add(commandDispatcherActor);
    args.add(errorCommandDispatcherActor);
    args.add(registerCallbacks());

    this.handlers = registerHandlers(args, config);
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasMessage()
        && update.getMessage().hasText()
        && update.getMessage().getChatId() != null) {
      final Long chatId = update.getMessage().getChatId();
      if (update.getMessage().getFrom() == null) {
        sendMessage("Вы что-то делаете не так. Обратитесь к администратору.", chatId);
      }
      final Optional<String> username = extractUsername(update);
      Optional<IsProcessedState> isProcessedState = stateRepository.getIsProcessedState();
      if (chatId.equals(config.getTelegram().getTgAdminChatId())
          || (isProcessedState.isPresent() && IsProcessedState.ENABLE == isProcessedState.get())) {
        String inputText = StringUtils.normalizeSpace(update.getMessage().getText());
        String[] commandArgs = inputText.split(WHITESPACE_STR);
        if (commandArgs.length >= 1) {
          String commandPrefix = StringUtils.normalizeSpace(commandArgs[0]);
          if (handlers.containsKey(commandPrefix)) {
            handlers
                .get(commandPrefix)
                .handleCommand(
                    CommandContext.builder()
                        .commandUniqueId(
                            Long.valueOf(RandomStringUtils.randomNumeric(COMMAND_UNIQUE_ID_LENGTH)))
                        .commandArgs(commandArgs)
                        .senderChatId(chatId)
                        .username(username.get())
                        .platformType(PlatformType.TG)
                        .requestedResponseLang(Lang.RU.getLang())
                        .build());
          }
        }
      }
    }
  }

  @Override
  public String getBotUsername() {
    return config.getTelegram().getTgBotUsername();
  }

  @Override
  public String getBotToken() {
    return config.getTelegram().getTgBotToken();
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
        registerCommandHandler(LogCommandHandler.class, getHandlerArgs(args, new HashSet<>())));
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
        registerCommandHandler(StatusCommandHandler.class, getHandlerArgs(args, new HashSet<>())));
    handlers.putIfAbsent(
        "/update_teams",
        registerCommandHandler(
            UpdateTeamsCommandHandler.class,
            getHandlerArgs(args, getUpdateTeamsCommandPermissions(config))));
    return Collections.unmodifiableMap(handlers);
  }

  private Object[] getHandlerArgs(final List<Object> args, final Set<Long> allowedChatIds) {
    List<Object> currentArgs = new ArrayList<>(args);
    currentArgs.add(allowedChatIds);
    return currentArgs.toArray();
  }

  private Supplier<Long> prepareGetAdminChatIdSupplier() {
    return () -> config.getTelegram().getTgAdminChatId();
  }

  @SuppressWarnings("checkstyle:LineLength")
  private Function<HandlerErrorContext, Optional<String>> prepareErrorMessageSupplier() {
    return handlerErrorContext -> {
      switch (handlerErrorContext.getErrorType()) {
        case COMMAND_NOT_ALLOWED:
          return Optional.of(
              String.format(
                  "@%s эта команда недоступна", handlerErrorContext.getContext().getUsername()));
        case MISSED_USERNAME:
          return Optional.of("Нужно прописать username в настройках telegram!");
        case MISSED_PLATFORM_USERNAME:
          return Optional.of(
              String.format(
                  "Перед привязкой %s ника нужно установить username в настройках "
                      + "телеграма. Инструкция: http://telegramzy.ru/nik-v-telegramm/",
                  GamePlatformType.fromString(config.getGamePlatform()) == GamePlatformType.MAJSOUL
                      ? "mahjongsoul"
                      : "tenhou.net"));
        default:
          break;
      }
      return Optional.empty();
    };
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

  private Optional<String> extractUsername(Update update) {
    User from = update.getMessage().getFrom();
    if (Boolean.FALSE.equals(from.getIsBot())) {
      return Optional.ofNullable(from.getUserName());
    }
    return Optional.empty();
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  private void sendMessage(String message, Long chatId) {
    try {
      SendMessage sendMessage = new SendMessage();
      sendMessage.setChatId(chatId);
      sendMessage.setText(message);
      sendMessage.enableMarkdownV2(true);
      execute(sendMessage);
    } catch (Exception ex) {
      // todo handle
      log.error("error while execute", ex);
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void run() {
    do {
      try {
        ResultCommandContainer resultCommandContainer = messagesQueue.take();
        switch (resultCommandContainer.getPlatformType()) {
          case TG:
            sendMessage(
                resultCommandContainer.getResultMessage(), resultCommandContainer.getReplyChatId());
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
