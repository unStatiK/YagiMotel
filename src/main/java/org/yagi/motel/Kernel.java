package org.yagi.motel;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.routing.SmallestMailboxPool;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.yagi.motel.actor.CommandDispatcherActor;
import org.yagi.motel.actor.blocked.AddCommandDispatcherActor;
import org.yagi.motel.actor.blocked.ChangeStateDispatcherActor;
import org.yagi.motel.actor.blocked.CheckNotificationsDispatcherActor;
import org.yagi.motel.actor.blocked.CloseRegistrationCommandDispatcherActor;
import org.yagi.motel.actor.blocked.LogCommandDispatcherActor;
import org.yagi.motel.actor.blocked.MeCommandDispatcherActor;
import org.yagi.motel.actor.blocked.StartRegistrationCommandDispatcherActor;
import org.yagi.motel.actor.blocked.StatusCommandDispatcherActor;
import org.yagi.motel.bot.discord.DiscordTournamentHelper;
import org.yagi.motel.bot.telegram.TournamentHelper;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.mailbox.PriorityMailbox;
import org.yagi.motel.message.CheckNotificationMessage;
import org.yagi.motel.model.container.CommunicationPlatformsContainer;
import org.yagi.motel.model.container.NotificationContainer;
import org.yagi.motel.model.container.ResultCommandContainer;
import org.yagi.motel.model.enums.IsProcessedState;
import org.yagi.motel.model.holder.CommandDispatchersHolder;
import org.yagi.motel.pipeline.CommandResultHandler;
import org.yagi.motel.pipeline.NotificationHandler;
import org.yagi.motel.repository.StateRepository;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

@Slf4j
@SuppressWarnings("checkstyle:MissingJavadocType")
public class Kernel {

  private static final String CONFIG_PATH = "config/config.yaml";
  private static final String STATE_DB_FILENAME = "state.db";

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static void main(String[] args) throws TelegramApiException, IOException, SQLException {
    String version = System.getProperty("java.version");
    log.info("JDK: {}", version);

    final AppConfig config = loadConfig();
    final StateRepository stateRepository = new StateRepository(STATE_DB_FILENAME);
    final BlockingQueue<ResultCommandContainer> commandResultsQueue =
        new ArrayBlockingQueue<>(config.getCommandResultQueueCapacity());
    final BlockingQueue<NotificationContainer> notificationsQueue =
        new ArrayBlockingQueue<>(config.getNotificationQueueCapacity());
    updateState(config, stateRepository);

    final ActorRef commandDispatcherActor =
        configureActors(config, stateRepository, commandResultsQueue, notificationsQueue);
    final CommunicationPlatformsContainer communicationPlatformsContainer =
        prepareContainer(commandDispatcherActor, config, stateRepository);

    if (Boolean.TRUE.equals(config.getTelegram().getEnable())) {
      runTgBot(communicationPlatformsContainer.getTgBot());
    }
    if (Boolean.TRUE.equals(config.getDiscord().getEnable())) {
      runDiscordBot(communicationPlatformsContainer.getDiscordBot());
    }

    startCommandResultHandlers(config, communicationPlatformsContainer, commandResultsQueue);
    startNotificationHandlers(config, communicationPlatformsContainer, notificationsQueue);
    log.info("YagiMotel started...");
  }

  private static void updateState(AppConfig config, StateRepository stateRepository) {
    stateRepository.updateProcessingState(
        Boolean.TRUE.equals(config.getDisableOnStart())
            ? IsProcessedState.DISABLE
            : IsProcessedState.ENABLE);
  }

  private static CommunicationPlatformsContainer prepareContainer(
      ActorRef commandDispatcherActor, AppConfig config, StateRepository stateRepository) {
    final TournamentHelper tgBot =
        new TournamentHelper(commandDispatcherActor, config, stateRepository);
    final DiscordTournamentHelper discordBot =
        new DiscordTournamentHelper(commandDispatcherActor, config, stateRepository);
    return CommunicationPlatformsContainer.builder().tgBot(tgBot).discordBot(discordBot).build();
  }

  private static void runTgBot(TournamentHelper bot) throws TelegramApiException {
    final TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
    botsApi.registerBot(bot);
  }

  private static void runDiscordBot(DiscordTournamentHelper discordBot) {
    // todo: handle stop thread
    discordBot.run();
  }

  private static void startCommandResultHandlers(
      AppConfig config,
      CommunicationPlatformsContainer communicationPlatformsContainer,
      BlockingQueue<ResultCommandContainer> commandResultsQueue) {
    Function<String, ThreadFactory> threadFactorySupplier = getThreadFactorySupplier();
    int commandResultHandlerThreadNumber = config.getCommandResultHandlerThreadNumber();
    ExecutorService executor =
        Executors.newFixedThreadPool(
            commandResultHandlerThreadNumber,
            threadFactorySupplier.apply("result-commands-dispatcher-thread"));
    for (int i = 0; i < commandResultHandlerThreadNumber; i++) {
      executor.submit(
          new CommandResultHandler(commandResultsQueue, communicationPlatformsContainer));
    }
  }

  private static void startNotificationHandlers(
      AppConfig config,
      CommunicationPlatformsContainer communicationPlatformsContainer,
      BlockingQueue<NotificationContainer> notificationsQueue) {
    Function<String, ThreadFactory> threadFactorySupplier = getThreadFactorySupplier();
    int notificationHandlerThreadNumber = config.getNotificationHandlerThreadNumber();
    ExecutorService executor =
        Executors.newFixedThreadPool(
            notificationHandlerThreadNumber,
            threadFactorySupplier.apply("notification-dispatcher-thread"));
    for (int i = 0; i < notificationHandlerThreadNumber; i++) {
      executor.submit(
          new NotificationHandler(notificationsQueue, communicationPlatformsContainer, config));
    }
  }

  private static ActorRef configureActors(
      AppConfig config,
      StateRepository stateRepository,
      BlockingQueue<ResultCommandContainer> commandResultsQueue,
      BlockingQueue<NotificationContainer> notificationsQueue) {
    final ActorSystem actorSystem = ActorSystem.create();

    final ActorRef checkNotificationsDispatcherActor =
        actorSystem.actorOf(
            CheckNotificationsDispatcherActor.props(config, stateRepository, notificationsQueue)
                .withRouter(new SmallestMailboxPool(1)),
            CheckNotificationsDispatcherActor.ACTOR_NAME);

    final ActorRef logCommandDispatcherActor =
        actorSystem.actorOf(
            LogCommandDispatcherActor.props(config, commandResultsQueue)
                .withRouter(new SmallestMailboxPool(config.getEventsMailboxPool()))
                .withMailbox(PriorityMailbox.DISPATCHER_NAME),
            LogCommandDispatcherActor.ACTOR_NAME);

    final ActorRef meCommandDispatcherActor =
        actorSystem.actorOf(
            MeCommandDispatcherActor.props(config, commandResultsQueue)
                .withRouter(new SmallestMailboxPool(config.getEventsMailboxPool()))
                .withMailbox(PriorityMailbox.DISPATCHER_NAME),
            MeCommandDispatcherActor.ACTOR_NAME);

    final ActorRef addCommandDispatcherActor =
        actorSystem.actorOf(
            AddCommandDispatcherActor.props(config, commandResultsQueue)
                .withRouter(new SmallestMailboxPool(config.getEventsMailboxPool()))
                .withMailbox(PriorityMailbox.DISPATCHER_NAME),
            AddCommandDispatcherActor.ACTOR_NAME);

    final ActorRef startRegistrationCommandDispatcherActor =
        actorSystem.actorOf(
            StartRegistrationCommandDispatcherActor.props(commandResultsQueue, config)
                .withRouter(new SmallestMailboxPool(config.getEventsMailboxPool()))
                .withMailbox(PriorityMailbox.DISPATCHER_NAME),
            StartRegistrationCommandDispatcherActor.ACTOR_NAME);

    final ActorRef closeRegistrationCommandDispatcherActor =
        actorSystem.actorOf(
            CloseRegistrationCommandDispatcherActor.props(commandResultsQueue, config)
                .withRouter(new SmallestMailboxPool(config.getEventsMailboxPool()))
                .withMailbox(PriorityMailbox.DISPATCHER_NAME),
            CloseRegistrationCommandDispatcherActor.ACTOR_NAME);

    final ActorRef statusCommandDispatcherActor =
        actorSystem.actorOf(
            StatusCommandDispatcherActor.props(commandResultsQueue, config)
                .withRouter(new SmallestMailboxPool(config.getEventsMailboxPool()))
                .withMailbox(PriorityMailbox.DISPATCHER_NAME),
            StatusCommandDispatcherActor.ACTOR_NAME);

    final ActorRef changeStateDispatcherActor =
        actorSystem.actorOf(
            ChangeStateDispatcherActor.props(stateRepository, commandResultsQueue)
                .withRouter(new SmallestMailboxPool(config.getEventsMailboxPool()))
                .withMailbox(PriorityMailbox.DISPATCHER_NAME),
            ChangeStateDispatcherActor.ACTOR_NAME);

    final CommandDispatchersHolder commandDispatchersHolder =
        CommandDispatchersHolder.builder()
            .logCommandDispatcherActor(logCommandDispatcherActor)
            .meCommandDispatcherActor(meCommandDispatcherActor)
            .addCommandDispatcherActor(addCommandDispatcherActor)
            .startRegistrationCommandDispatcherActor(startRegistrationCommandDispatcherActor)
            .closeRegistrationCommandDispatcherActor(closeRegistrationCommandDispatcherActor)
            .statusCommandDispatcherActor(statusCommandDispatcherActor)
            .checkNotificationsDispatcherActor(checkNotificationsDispatcherActor)
            .changeStateDispatcherActor(changeStateDispatcherActor)
            .build();

    actorSystem
        .scheduler()
        .scheduleWithFixedDelay(
            Duration.ZERO,
            Duration.ofSeconds(config.getCheckNotificationsIntervalInSeconds()),
            checkNotificationsDispatcherActor,
            new CheckNotificationMessage(),
            actorSystem.dispatcher(),
            ActorRef.noSender());

    return actorSystem.actorOf(
        CommandDispatcherActor.props(commandDispatchersHolder)
            .withMailbox(PriorityMailbox.DISPATCHER_NAME),
        CommandDispatcherActor.ACTOR_NAME);
  }

  private static AppConfig loadConfig() throws IOException {
    Yaml yaml = new Yaml(new Constructor(AppConfig.class, new LoaderOptions()));
    InputStream inputStream = Files.newInputStream(Paths.get(CONFIG_PATH));
    return yaml.load(inputStream);
  }

  private static Function<String, ThreadFactory> getThreadFactorySupplier() {
    return (threadPrefix) ->
        new ThreadFactory() {
          private final AtomicInteger counter = new AtomicInteger(0);

          @Override
          public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(false);
            t.setName(threadPrefix + "-" + counter.incrementAndGet());
            return t;
          }
        };
  }
}
