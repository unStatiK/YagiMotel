package org.yagi.motel.bot.telegram;

import static org.yagi.motel.bot.telegram.utils.TelegramCommandPermissionsProvider.getAddCommandPermissions;
import static org.yagi.motel.bot.telegram.utils.TelegramCommandPermissionsProvider.getCloseRegistrationCommandPermissions;
import static org.yagi.motel.bot.telegram.utils.TelegramCommandPermissionsProvider.getMeCommandPermissions;
import static org.yagi.motel.bot.telegram.utils.TelegramCommandPermissionsProvider.getStartRegistrationCommandPermissions;
import static org.yagi.motel.bot.telegram.utils.TelegramCommandPermissionsProvider.getStartServeCommandPermissions;
import static org.yagi.motel.bot.telegram.utils.TelegramCommandPermissionsProvider.getStopServeCommandPermissions;

import akka.actor.ActorRef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.yagi.motel.bot.PlatformType;
import org.yagi.motel.bot.context.CommandContext;
import org.yagi.motel.bot.context.HandlerErrorContext;
import org.yagi.motel.bot.context.ReplyCallbackContext;
import org.yagi.motel.bot.handler.AddCommandHandler;
import org.yagi.motel.bot.handler.CloseRegistrationCommandHandler;
import org.yagi.motel.bot.handler.CommandHandler;
import org.yagi.motel.bot.handler.LogCommandHandler;
import org.yagi.motel.bot.handler.MeCommandHandler;
import org.yagi.motel.bot.handler.StartRegistrationCommandHandler;
import org.yagi.motel.bot.handler.StartServeCommandHandler;
import org.yagi.motel.bot.handler.StatusCommandHandler;
import org.yagi.motel.bot.handler.StopServeCommandHandler;
import org.yagi.motel.bot.holder.PlatformCallbacksHolder;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.model.container.ResultCommandContainer;
import org.yagi.motel.model.enums.GamePlatformType;
import org.yagi.motel.model.enums.IsProcessedState;
import org.yagi.motel.model.enums.Lang;
import org.yagi.motel.repository.StateRepository;

@Slf4j
@SuppressWarnings("checkstyle:MissingJavadocType")
public class TournamentHelper extends TelegramLongPollingBot {
  private static final String WHITESPACE_STR = " ";
  private final Map<String, CommandHandler> handlers;
  private final AppConfig config;
  private final StateRepository stateRepository;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public TournamentHelper(
      ActorRef commandDispatcherActor, AppConfig config, StateRepository stateRepository) {
    this.config = config;
    this.stateRepository = stateRepository;
    final List<Object> args = new ArrayList<>();
    args.add(config);
    args.add(commandDispatcherActor);
    args.add(registerCallbacks());

    this.handlers = registerHandlers(args, config);
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public void sendReply(ResultCommandContainer resultCommandContainer) {
    try {
      switch (resultCommandContainer.getReplyType()) {
        case SEND_MESSAGE:
          sendMessage(resultCommandContainer);
          break;
        default:
          break;
      }
    } catch (Exception ex) {
      // todo handle
      log.error("error while execute", ex);
      throw new RuntimeException(ex);
    }
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public void sendNotification(String message, Long chatId) {
    try {
      SendMessage sendMessage = new SendMessage();
      sendMessage.setChatId(chatId);
      sendMessage.setText(message);
      execute(sendMessage);
    } catch (Exception ex) {
      // todo handle
      log.error("error while execute", ex);
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasMessage()
        && update.getMessage().hasText()
        && update.getMessage().getChatId() != null) {
      final Long chatId = update.getMessage().getChatId();
      if (update.getMessage().getFrom() == null) {
        sendNotification("Вы что-то делаете не так. Обратитесь к администратору.", chatId);
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
        .platformSendMessageCallback(prepareSendMessageCallback())
        .platformErrorReplySupplier(prepareErrorReplySupplier())
        .platformAdminChatIdSupplier(prepareGetAdminChatIdSupplier())
        .build();
  }

  private Map<String, CommandHandler> registerHandlers(final List<Object> args, AppConfig config) {
    final Map<String, CommandHandler> handlers = new HashMap<>();
    handlers.putIfAbsent("/log", registerCommandHandler(LogCommandHandler.class,
        getHandlerArgs(args, new HashSet<>())));
    handlers.putIfAbsent("/start_serve", registerCommandHandler(StartServeCommandHandler.class,
        getHandlerArgs(args, getStartServeCommandPermissions(config))));
    handlers.putIfAbsent("/stop_serve", registerCommandHandler(StopServeCommandHandler.class,
        getHandlerArgs(args, getStopServeCommandPermissions(config))));
    handlers.putIfAbsent("/start_registration",
        registerCommandHandler(StartRegistrationCommandHandler.class,
            getHandlerArgs(args, getStartRegistrationCommandPermissions(config))));
    handlers.putIfAbsent("/close_registration",
        registerCommandHandler(CloseRegistrationCommandHandler.class,
            getHandlerArgs(args, getCloseRegistrationCommandPermissions(config))));
    handlers.putIfAbsent("/me", registerCommandHandler(MeCommandHandler.class,
        getHandlerArgs(args, getMeCommandPermissions(config))));
    handlers.putIfAbsent("/add", registerCommandHandler(AddCommandHandler.class,
        getHandlerArgs(args, getAddCommandPermissions(config))));
    handlers.putIfAbsent("/status", registerCommandHandler(StatusCommandHandler.class,
        getHandlerArgs(args, new HashSet<>())));
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

  private Consumer<Optional<ReplyCallbackContext>> prepareSendMessageCallback() {
    return replyCallbackContext -> {
      if (replyCallbackContext.isPresent()) {
        try {
          final SendMessage sendMessage = new SendMessage();
          sendMessage.setChatId(replyCallbackContext.get().getTargetChatId());
          sendMessage.setText(replyCallbackContext.get().getText());
          execute(sendMessage);
        } catch (TelegramApiException ex) {
          // todo handle exception
          log.error("error while execute", ex);
          throw new RuntimeException(ex);
        }
      }
    };
  }

  @SuppressWarnings("checkstyle:LineLength")
  private Function<HandlerErrorContext, Optional<ReplyCallbackContext>> prepareErrorReplySupplier() {
    return handlerErrorContext -> {
      switch (handlerErrorContext.getErrorType()) {
        case COMMAND_NOT_ALLOWED:
          return Optional.of(
              ReplyCallbackContext.builder()
                  .targetChatId(handlerErrorContext.getContext().getSenderChatId())
                  .text(
                      String.format(
                          "@%s эта команда недоступна",
                          handlerErrorContext.getContext().getUsername()))
                  .build());
        case MISSED_USERNAME:
          return Optional.of(
              ReplyCallbackContext.builder()
                  .targetChatId(handlerErrorContext.getContext().getSenderChatId())
                  .text("Нужно прописать username в настройках telegram!")
                  .build());
        case MISSED_PLATFORM_USERNAME:
          return Optional.of(
              ReplyCallbackContext.builder()
                  .targetChatId(handlerErrorContext.getContext().getSenderChatId())
                  .text(
                      String.format(
                          "Перед привязкой %s ника нужно установить username в настройках "
                              + "телеграма. Инструкция: http://telegramzy.ru/nik-v-telegramm/",
                          GamePlatformType.fromString(config.getGamePlatform())
                              == GamePlatformType.MAJSOUL
                              ? "mahjongsoul"
                              : "tenhou.net"))
                  .build());
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
          .getDeclaredConstructor(AppConfig.class, ActorRef.class, PlatformCallbacksHolder.class,
              Set.class)
          .newInstance(args);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private void sendMessage(ResultCommandContainer resultCommandContainer)
      throws TelegramApiException {
    SendMessage sendMessage = new SendMessage();
    sendMessage.setChatId(resultCommandContainer.getReplyChatId());
    sendMessage.setText(resultCommandContainer.getResultMessage());
    execute(sendMessage);
  }

  private Optional<String> extractUsername(Update update) {
    User from = update.getMessage().getFrom();
    if (Boolean.FALSE.equals(from.getIsBot())) {
      return Optional.ofNullable(from.getUserName());
    }
    return Optional.empty();
  }
}
