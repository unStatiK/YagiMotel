package org.yagi.motel.bot;

import akka.actor.ActorRef;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.yagi.motel.bot.handler.AddCommandHandler;
import org.yagi.motel.bot.handler.CloseRegistrationCommandHandler;
import org.yagi.motel.bot.handler.CommandHandler;
import org.yagi.motel.bot.handler.LogCommandHandler;
import org.yagi.motel.bot.handler.MeCommandHandler;
import org.yagi.motel.bot.handler.StartRegistrationCommandHandler;
import org.yagi.motel.bot.handler.StartServeCommandHandler;
import org.yagi.motel.bot.handler.StatusCommandHandler;
import org.yagi.motel.bot.handler.StopServeCommandHandler;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.model.container.ResultCommandContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class TournamentHelper extends TelegramLongPollingBot {
    private static final String WHITESPACE_STR = " ";
    private final Map<String, CommandHandler> handlers;
    private final AppConfig config;

    @Setter
    private boolean processingUpdateIsDisable;

    public TournamentHelper(ActorRef commandDispatcherActor, AppConfig config) {
        this.config = config;
        this.processingUpdateIsDisable = config.getDisableOnStart();
        final List<Function> callbacks = prepareExecuteCallbacks();
        final List<Object> args = new ArrayList<>();
        args.add(config);
        args.add(commandDispatcherActor);
        args.addAll(callbacks);

        this.handlers = registerHandlers(args);
    }

    private Map<String, CommandHandler> registerHandlers(final List<Object> args) {
        final Map<String, CommandHandler> handlers = new HashMap<>();
        final Object[] handlerArgs = args.toArray();
        handlers.putIfAbsent("/log", registerCommandHandler(LogCommandHandler.class, handlerArgs));
        handlers.putIfAbsent("/start_serve", registerCommandHandler(StartServeCommandHandler.class, handlerArgs));
        handlers.putIfAbsent("/stop_serve", registerCommandHandler(StopServeCommandHandler.class, handlerArgs));
        handlers.putIfAbsent("/start_registration", registerCommandHandler(StartRegistrationCommandHandler.class, handlerArgs));
        handlers.putIfAbsent("/close_registration", registerCommandHandler(CloseRegistrationCommandHandler.class, handlerArgs));
        handlers.putIfAbsent("/me", registerCommandHandler(MeCommandHandler.class, handlerArgs));
        handlers.putIfAbsent("/add", registerCommandHandler(AddCommandHandler.class, handlerArgs));
        handlers.putIfAbsent("/status", registerCommandHandler(StatusCommandHandler.class, handlerArgs));
        return Collections.unmodifiableMap(handlers);
    }

    private List<Function> prepareExecuteCallbacks() {
        return Arrays.asList((Function<SendMessage, Void>) o -> {
            try {
                execute(o);
                return null;
            } catch (TelegramApiException ex) {
                //todo handle exception
                log.error("error while execute", ex);
                throw new RuntimeException(ex);
            }
        });
    }

    private CommandHandler registerCommandHandler(Class<? extends CommandHandler> handlerClazz, Object[] args) {
        try {
            return handlerClazz.getDeclaredConstructor(AppConfig.class, ActorRef.class, Function.class).newInstance(args);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void sendReply(ResultCommandContainer resultCommandContainer) {
        try {
            switch (resultCommandContainer.getReplyType()) {
                case SEND_MESSAGE: {
                    sendMessage(resultCommandContainer);
                }
                break;
                case ENABLE_UPDATE_PROCESSING: {
                    setProcessingUpdateIsDisable(false);
                    sendMessage(resultCommandContainer);
                }
                break;
                case DISABLE_UPDATE_PROCESSING: {
                    setProcessingUpdateIsDisable(true);
                    sendMessage(resultCommandContainer);
                }
                break;
                default:
                    break;
            }
        } catch (Exception ex) {
            //todo handle
            log.error("error while execute", ex);
            throw new RuntimeException(ex);
        }
    }

    public void sendNotification(String message, Long chatId) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message);
            execute(sendMessage);
        } catch (Exception ex) {
            //todo handle
            log.error("error while execute", ex);
            throw new RuntimeException(ex);
        }
    }

    private void sendMessage(ResultCommandContainer resultCommandContainer) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(resultCommandContainer.getReplyChatId());
        sendMessage.setText(resultCommandContainer.getResultMessage());
        execute(sendMessage);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText() && update.getMessage().getChatId() != null) {
            final Long chatId = update.getMessage().getChatId();
            if (update.getMessage().getFrom() == null) {
                sendNotification("Вы что-то делаете не так. Обратитесь к администратору.", chatId);
            }
            final String username = extractUsername(update);
            if (chatId.equals(config.getAdminChatId()) || !Boolean.TRUE.equals(processingUpdateIsDisable)) {
                String inputText = StringUtils.normalizeSpace(update.getMessage().getText());
                String[] commandArgs = inputText.split(WHITESPACE_STR);
                if (commandArgs.length >= 1) {
                    String commandPrefix = StringUtils.normalizeSpace(commandArgs[0]);
                    if (handlers.containsKey(commandPrefix)) {
                        handlers.get(commandPrefix).handleCommand(CommandContext.builder()
                                .update(update)
                                .commandArgs(commandArgs)
                                .senderChatId(chatId)
                                .telegramUsername(username)
                                .build());
                    }
                }
            }
        }
    }

    private String extractUsername(Update update) {
        User from = update.getMessage().getFrom();
        if (Boolean.FALSE.equals(from.getIsBot())) {
            return from.getUserName();
        }
        return null;
    }

    @Override
    public String getBotUsername() {
        return config.getBotUsername();
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }
}
