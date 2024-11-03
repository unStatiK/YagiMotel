package org.yagi.motel;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.initializer.KernelInitializer;
import org.yagi.motel.initializer.container.CommunicationPlatformsContainer;
import org.yagi.motel.kernel.model.container.NotificationContainer;
import org.yagi.motel.kernel.model.container.ResultCommandContainer;
import org.yagi.motel.kernel.repository.StateRepository;
import org.yagi.motel.kernel.router.CommandResultRouter;
import org.yagi.motel.kernel.router.NotificationRouter;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

@Slf4j
@SuppressWarnings("checkstyle:MissingJavadocType")
public class Kernel {

  private static final String KERNEL_APP_NAME = "YagiMotel";
  private static final String CONFIG_PATH = "config/config.yaml";
  private static final String STATE_DB_FILENAME = "state.db";
  private static final String RESULT_COMMAND_DISPATCHER_THREAD_PREFIX =
      "result-commands-dispatcher-thread";
  private static final String NOTIFICATION_DISPATCHER_THREAD_PREFIX =
      "notification-dispatcher-thread";

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static void main(String[] args) throws TelegramApiException, IOException, SQLException {
    logJdkVersion();
    final ActorSystem actorSystem = ActorSystem.create();
    final AppConfig config = loadConfig();
    final BlockingQueue<ResultCommandContainer> commandResultsQueue =
        new ArrayBlockingQueue<>(config.getCommandResultQueueCapacity());
    final BlockingQueue<NotificationContainer> notificationsQueue =
        new ArrayBlockingQueue<>(config.getNotificationQueueCapacity());
    final BlockingQueue<ResultCommandContainer> tgMessagesQueue =
        new ArrayBlockingQueue<>(config.getTelegram().getMessagesQueueCapacity());
    final BlockingQueue<ResultCommandContainer> discordMessagesQueue =
        new ArrayBlockingQueue<>(config.getDiscord().getMessagesQueueCapacity());
    final StateRepository stateRepository =
        KernelInitializer.initStateRepository(config, STATE_DB_FILENAME);
    final ActorRef commandDispatcherActor =
        KernelInitializer.initKernelCommandDispatcherActor(
            actorSystem, config, stateRepository, commandResultsQueue);
    final ActorRef errorCommandDispatcherActor =
        KernelInitializer.initErrorCommandDispatcherActor(actorSystem, commandResultsQueue);
    final CommunicationPlatformsContainer communicationPlatformsContainer =
        KernelInitializer.createCommunicationPlatformsContainer(
            commandDispatcherActor,
            errorCommandDispatcherActor,
            config,
            stateRepository,
            tgMessagesQueue,
            discordMessagesQueue);

    KernelInitializer.startHandlers(
        config.getCommandResultHandlerThreadNumber(),
        RESULT_COMMAND_DISPATCHER_THREAD_PREFIX,
        getCommandResultHandlerSupplier(
            tgMessagesQueue, discordMessagesQueue, commandResultsQueue));
    KernelInitializer.startHandlers(
        config.getNotificationHandlerThreadNumber(),
        NOTIFICATION_DISPATCHER_THREAD_PREFIX,
        getNotificationHandlerSupplier(
            config, tgMessagesQueue, discordMessagesQueue, notificationsQueue));
    KernelInitializer.runTgBotIfEnabled(config, communicationPlatformsContainer);
    KernelInitializer.runDiscordBotIfEnabled(config, communicationPlatformsContainer);
    KernelInitializer.startCheckNotifications(
        actorSystem, config, stateRepository, notificationsQueue);
    logKernelStarted();
  }

  private static void logJdkVersion() {
    String version = System.getProperty("java.version");
    log.info("JDK: {}", version);
  }

  private static void logKernelStarted() {
    log.info("{} started...", KERNEL_APP_NAME);
  }

  private static Supplier<Runnable> getCommandResultHandlerSupplier(
      final BlockingQueue<ResultCommandContainer> tgMessagesQueue,
      final BlockingQueue<ResultCommandContainer> discordMessagesQueue,
      final BlockingQueue<ResultCommandContainer> commandResultsQueue) {
    return () ->
        new CommandResultRouter(commandResultsQueue, tgMessagesQueue, discordMessagesQueue);
  }

  @SuppressWarnings("checkstyle:LineLength")
  private static Supplier<Runnable> getNotificationHandlerSupplier(
      final AppConfig config,
      final BlockingQueue<ResultCommandContainer> tgMessagesQueue,
      final BlockingQueue<ResultCommandContainer> discordMessagesQueue,
      final BlockingQueue<NotificationContainer> notificationsQueue) {
    return () ->
        new NotificationRouter(notificationsQueue, tgMessagesQueue, discordMessagesQueue, config);
  }

  private static AppConfig loadConfig() throws IOException {
    Yaml yaml = new Yaml(new Constructor(AppConfig.class, new LoaderOptions()));
    InputStream inputStream = Files.newInputStream(Paths.get(CONFIG_PATH));
    return yaml.load(inputStream);
  }
}
