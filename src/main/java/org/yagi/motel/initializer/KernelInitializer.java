package org.yagi.motel.initializer;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.routing.SmallestMailboxPool;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.yagi.motel.bot.discord.DiscordTournamentHelper;
import org.yagi.motel.bot.telegram.TgTournamentHelper;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.initializer.container.CommunicationPlatformsContainer;
import org.yagi.motel.kernel.actor.CommandDispatcherActor;
import org.yagi.motel.kernel.actor.ErrorCommandDispatcherActor;
import org.yagi.motel.kernel.actor.blocked.AddCommandDispatcherActor;
import org.yagi.motel.kernel.actor.blocked.ChangeStateDispatcherActor;
import org.yagi.motel.kernel.actor.blocked.CheckNotificationsDispatcherActor;
import org.yagi.motel.kernel.actor.blocked.CloseRegistrationCommandDispatcherActor;
import org.yagi.motel.kernel.actor.blocked.LogCommandDispatcherActor;
import org.yagi.motel.kernel.actor.blocked.MeCommandDispatcherActor;
import org.yagi.motel.kernel.actor.blocked.StartRegistrationCommandDispatcherActor;
import org.yagi.motel.kernel.actor.blocked.StatusCommandDispatcherActor;
import org.yagi.motel.kernel.actor.blocked.UpdateTeamsCommandDispatcherActor;
import org.yagi.motel.kernel.enums.PlatformType;
import org.yagi.motel.kernel.mailbox.PriorityMailbox;
import org.yagi.motel.kernel.message.CheckNotificationMessage;
import org.yagi.motel.kernel.model.container.NotificationContainer;
import org.yagi.motel.kernel.model.container.ResultCommandContainer;
import org.yagi.motel.kernel.model.enums.IsProcessedState;
import org.yagi.motel.kernel.model.holder.CommandDispatchersHolder;
import org.yagi.motel.kernel.repository.StateRepository;
import org.yagi.motel.utils.PlatformUtils;

@UtilityClass
@SuppressWarnings("checkstyle:MissingJavadocType")
public class KernelInitializer {

  private static final String TG_MESSAGES_THREAD_PREFIX = "tg-messages-thread";
  private static final String DISCORD_MESSAGES_THREAD_PREFIX = "discord-messages-thread";

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static StateRepository initStateRepository(
      final AppConfig config, final String stateDbFilename) throws SQLException {
    final StateRepository stateRepository = new StateRepository(stateDbFilename);
    updateState(config, stateRepository);
    return stateRepository;
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static void startCheckNotifications(
      final ActorSystem actorSystem,
      final AppConfig config,
      final StateRepository stateRepository,
      final BlockingQueue<NotificationContainer> notificationsQueue) {
    final ActorRef checkNotificationsDispatcherActor =
        actorSystem.actorOf(
            CheckNotificationsDispatcherActor.props(config, stateRepository, notificationsQueue)
                .withRouter(new SmallestMailboxPool(1)),
            CheckNotificationsDispatcherActor.ACTOR_NAME);
    actorSystem
        .scheduler()
        .scheduleWithFixedDelay(
            Duration.ZERO,
            Duration.ofSeconds(config.getCheckNotificationsIntervalInSeconds()),
            checkNotificationsDispatcherActor,
            new CheckNotificationMessage(),
            actorSystem.dispatcher(),
            ActorRef.noSender());
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static ActorRef initKernelCommandDispatcherActor(
      final ActorSystem actorSystem,
      final AppConfig config,
      final StateRepository stateRepository,
      final BlockingQueue<ResultCommandContainer> commandResultsQueue) {
    return configureCommandDispatcherActor(
        actorSystem, config, stateRepository, commandResultsQueue);
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static CommunicationPlatformsContainer createCommunicationPlatformsContainer(
      final ActorRef commandDispatcherActor,
      final ActorRef errorCommandDispatcherActor,
      final AppConfig config,
      final StateRepository stateRepository,
      BlockingQueue<ResultCommandContainer> tgMessagesQueue,
      BlockingQueue<ResultCommandContainer> discordMessagesQueue) {
    return prepareContainer(
        commandDispatcherActor,
        errorCommandDispatcherActor,
        config,
        stateRepository,
        tgMessagesQueue,
        discordMessagesQueue);
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static ActorRef initErrorCommandDispatcherActor(
      ActorSystem actorSystem, BlockingQueue<ResultCommandContainer> commandResultsQueue) {
    return actorSystem.actorOf(
        ErrorCommandDispatcherActor.props(commandResultsQueue)
            .withMailbox(PriorityMailbox.DISPATCHER_NAME),
        ErrorCommandDispatcherActor.ACTOR_NAME);
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static <T> void startHandlers(
      int handlerThreadsNumber, String handlerThreadPrefix, Supplier<Runnable> handlerSupplier) {
    Function<String, ThreadFactory> threadFactorySupplier = getThreadFactorySupplier();
    ExecutorService executor =
        Executors.newFixedThreadPool(
            handlerThreadsNumber, threadFactorySupplier.apply(handlerThreadPrefix));
    for (int i = 0; i < handlerThreadsNumber; i++) {
      executor.submit(handlerSupplier.get());
    }
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static void runTgBotIfEnabled(
      AppConfig config, CommunicationPlatformsContainer communicationPlatformsContainer)
      throws TelegramApiException {
    if (PlatformUtils.isPlatformEnable(config, PlatformType.TG)) {
      runTgBot(communicationPlatformsContainer.getTgBot(), config);
    }
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static void runDiscordBotIfEnabled(
      AppConfig config, CommunicationPlatformsContainer communicationPlatformsContainer) {
    if (PlatformUtils.isPlatformEnable(config, PlatformType.DISCORD)) {
      runDiscordBot(communicationPlatformsContainer.getDiscordBot(), config);
    }
  }

  private static void runTgBot(TgTournamentHelper bot, AppConfig config)
      throws TelegramApiException {
    final TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
    botsApi.registerBot(bot);
    startHandlers(
        config.getTelegram().getMessagesThreadNumber(), TG_MESSAGES_THREAD_PREFIX, () -> bot);
  }

  private static void runDiscordBot(DiscordTournamentHelper discordBot, AppConfig config) {
    // todo: handle stop thread
    discordBot.start();
    startHandlers(
        config.getDiscord().getMessagesThreadNumber(),
        DISCORD_MESSAGES_THREAD_PREFIX,
        () -> discordBot);
  }

  private static void updateState(AppConfig config, StateRepository stateRepository) {
    stateRepository.updateProcessingState(
        Boolean.TRUE.equals(config.getDisableOnStart())
            ? IsProcessedState.DISABLE
            : IsProcessedState.ENABLE);
  }

  private static CommunicationPlatformsContainer prepareContainer(
      ActorRef commandDispatcherActor,
      ActorRef errorCommandDispatcherActor,
      AppConfig config,
      StateRepository stateRepository,
      BlockingQueue<ResultCommandContainer> tgMessagesQueue,
      BlockingQueue<ResultCommandContainer> discordMessagesQueue) {
    final TgTournamentHelper tgBot =
        PlatformUtils.isPlatformEnable(config, PlatformType.TG)
            ? new TgTournamentHelper(
                commandDispatcherActor,
                errorCommandDispatcherActor,
                config,
                stateRepository,
                tgMessagesQueue)
            : null;
    final DiscordTournamentHelper discordBot =
        PlatformUtils.isPlatformEnable(config, PlatformType.DISCORD)
            ? new DiscordTournamentHelper(
                commandDispatcherActor,
                errorCommandDispatcherActor,
                config,
                stateRepository,
                discordMessagesQueue)
            : null;
    return CommunicationPlatformsContainer.builder().tgBot(tgBot).discordBot(discordBot).build();
  }

  private static ActorRef configureCommandDispatcherActor(
      ActorSystem actorSystem,
      AppConfig config,
      StateRepository stateRepository,
      BlockingQueue<ResultCommandContainer> commandResultsQueue) {
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

    final ActorRef updateTeamsCommandDispatcherActor =
        actorSystem.actorOf(
            UpdateTeamsCommandDispatcherActor.props(config, commandResultsQueue)
                .withRouter(new SmallestMailboxPool(config.getEventsMailboxPool()))
                .withMailbox(PriorityMailbox.DISPATCHER_NAME),
            UpdateTeamsCommandDispatcherActor.ACTOR_NAME);

    final CommandDispatchersHolder commandDispatchersHolder =
        CommandDispatchersHolder.builder()
            .logCommandDispatcherActor(logCommandDispatcherActor)
            .meCommandDispatcherActor(meCommandDispatcherActor)
            .addCommandDispatcherActor(addCommandDispatcherActor)
            .startRegistrationCommandDispatcherActor(startRegistrationCommandDispatcherActor)
            .closeRegistrationCommandDispatcherActor(closeRegistrationCommandDispatcherActor)
            .statusCommandDispatcherActor(statusCommandDispatcherActor)
            .changeStateDispatcherActor(changeStateDispatcherActor)
            .updateTeamsDispatcherActor(updateTeamsCommandDispatcherActor)
            .build();

    return actorSystem.actorOf(
        CommandDispatcherActor.props(commandDispatchersHolder)
            .withMailbox(PriorityMailbox.DISPATCHER_NAME),
        CommandDispatcherActor.ACTOR_NAME);
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
