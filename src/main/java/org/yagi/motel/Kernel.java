package org.yagi.motel;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.routing.SmallestMailboxPool;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.yagi.motel.actor.CommandDispatcherActor;
import org.yagi.motel.actor.blocked.AddCommandDispatcherActor;
import org.yagi.motel.actor.blocked.CheckNotificationsDispatcherActor;
import org.yagi.motel.actor.blocked.CloseRegistrationCommandDispatcherActor;
import org.yagi.motel.actor.blocked.LogCommandDispatcherActor;
import org.yagi.motel.actor.blocked.MeCommandDispatcherActor;
import org.yagi.motel.actor.blocked.StartRegistrationCommandDispatcherActor;
import org.yagi.motel.actor.blocked.StatusCommandDispatcherActor;
import org.yagi.motel.bot.TournamentHelper;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.mailbox.PriorityMailbox;
import org.yagi.motel.message.CheckNotificationMessage;
import org.yagi.motel.model.container.NotificationContainer;
import org.yagi.motel.model.container.ResultCommandContainer;
import org.yagi.motel.model.holder.CommandDispatchersHolder;
import org.yagi.motel.pipeline.CommandResultHandler;
import org.yagi.motel.pipeline.NotificationHandler;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Slf4j
public class Kernel {

    private static final String CONFIG_PATH = "config/config.yaml";

    public static void main(String[] args) throws TelegramApiException, IOException {
        String version = System.getProperty("java.version");
        log.info("JDK: {}", version);

        final AppConfig config = loadConfig();
        final BlockingQueue<ResultCommandContainer> commandResultsQueue = new ArrayBlockingQueue<>(config.getCommandResultQueueCapacity());
        final BlockingQueue<NotificationContainer> notificationsQueue = new ArrayBlockingQueue<>(config.getNotificationQueueCapacity());
        final ActorRef commandDispatcherActor = configureActors(config, commandResultsQueue, notificationsQueue);
        final TournamentHelper bot = new TournamentHelper(commandDispatcherActor, config);
        final TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);

        startCommandResultHandlers(config, bot, commandResultsQueue);
        startNotificationHandlers(config, bot, notificationsQueue);
    }

    private static void startCommandResultHandlers(AppConfig config, TournamentHelper bot,
                                                   BlockingQueue<ResultCommandContainer> commandResultsQueue) {
        Function<String, ThreadFactory> threadFactorySupplier = getThreadFactorySupplier();
        int commandResultHandlerThreadNumber = config.getCommandResultHandlerThreadNumber();
        ExecutorService executor = Executors.newFixedThreadPool(commandResultHandlerThreadNumber,
                threadFactorySupplier.apply("result-commands-dispatcher-thread"));
        for (int i = 0; i < commandResultHandlerThreadNumber; i++) {
            executor.submit(new CommandResultHandler(commandResultsQueue, bot));
        }
    }

    private static void startNotificationHandlers(AppConfig config, TournamentHelper bot,
                                                  BlockingQueue<NotificationContainer> notificationsQueue) {
        Function<String, ThreadFactory> threadFactorySupplier = getThreadFactorySupplier();
        int notificationHandlerThreadNumber = config.getNotificationHandlerThreadNumber();
        ExecutorService executor = Executors.newFixedThreadPool(notificationHandlerThreadNumber,
                threadFactorySupplier.apply("notification-dispatcher-thread"));
        for (int i = 0; i < notificationHandlerThreadNumber; i++) {
            executor.submit(new NotificationHandler(notificationsQueue, bot, config));
        }
    }

    private static ActorRef configureActors(AppConfig config, BlockingQueue<ResultCommandContainer> commandResultsQueue,
                                            BlockingQueue<NotificationContainer> notificationsQueue) {
        ActorSystem actorSystem = ActorSystem.create();

        ActorRef checkNotificationsDispatcherActor =
                actorSystem.actorOf(CheckNotificationsDispatcherActor.props(config, notificationsQueue)
                        .withRouter(new SmallestMailboxPool(1)), CheckNotificationsDispatcherActor.ACTOR_NAME);

        actorSystem.scheduler()
                .scheduleWithFixedDelay(
                        Duration.ZERO,
                        Duration.ofSeconds(config.getCheckNotificationsIntervalInSeconds()),
                        checkNotificationsDispatcherActor,
                        new CheckNotificationMessage(),
                        actorSystem.dispatcher(),
                        ActorRef.noSender());

        ActorRef logCommandDispatcherActor = actorSystem.actorOf(LogCommandDispatcherActor.props(config, commandResultsQueue)
                .withRouter(new SmallestMailboxPool(config.getEventsMailboxPool()))
                .withMailbox(PriorityMailbox.DISPATCHER_NAME), LogCommandDispatcherActor.ACTOR_NAME);

        ActorRef meCommandDispatcherActor = actorSystem.actorOf(MeCommandDispatcherActor.props(config, commandResultsQueue)
                .withRouter(new SmallestMailboxPool(config.getEventsMailboxPool()))
                .withMailbox(PriorityMailbox.DISPATCHER_NAME), MeCommandDispatcherActor.ACTOR_NAME);

        ActorRef addCommandDispatcherActor = actorSystem.actorOf(AddCommandDispatcherActor.props(config, commandResultsQueue)
                .withRouter(new SmallestMailboxPool(config.getEventsMailboxPool()))
                .withMailbox(PriorityMailbox.DISPATCHER_NAME), AddCommandDispatcherActor.ACTOR_NAME);

        ActorRef startRegistrationCommandDispatcherActor = actorSystem.actorOf(
                StartRegistrationCommandDispatcherActor.props(commandResultsQueue, config)
                        .withRouter(new SmallestMailboxPool(config.getEventsMailboxPool()))
                        .withMailbox(PriorityMailbox.DISPATCHER_NAME), StartRegistrationCommandDispatcherActor.ACTOR_NAME);

        ActorRef closeRegistrationCommandDispatcherActor = actorSystem.actorOf(
                CloseRegistrationCommandDispatcherActor.props(commandResultsQueue, config)
                        .withRouter(new SmallestMailboxPool(config.getEventsMailboxPool()))
                        .withMailbox(PriorityMailbox.DISPATCHER_NAME), CloseRegistrationCommandDispatcherActor.ACTOR_NAME);

        ActorRef statusCommandDispatcherActor = actorSystem.actorOf(
                StatusCommandDispatcherActor.props(commandResultsQueue, config)
                        .withRouter(new SmallestMailboxPool(config.getEventsMailboxPool()))
                        .withMailbox(PriorityMailbox.DISPATCHER_NAME), StatusCommandDispatcherActor.ACTOR_NAME);

        CommandDispatchersHolder commandDispatchersHolder = CommandDispatchersHolder.builder()
                .logCommandDispatcherActor(logCommandDispatcherActor)
                .meCommandDispatcherActor(meCommandDispatcherActor)
                .addCommandDispatcherActor(addCommandDispatcherActor)
                .startRegistrationCommandDispatcherActor(startRegistrationCommandDispatcherActor)
                .closeRegistrationCommandDispatcherActor(closeRegistrationCommandDispatcherActor)
                .statusCommandDispatcherActor(statusCommandDispatcherActor)
                .checkNotificationsDispatcherActor(checkNotificationsDispatcherActor)
                .build();

        return actorSystem.actorOf(CommandDispatcherActor.props(commandDispatchersHolder, commandResultsQueue)
                .withMailbox(PriorityMailbox.DISPATCHER_NAME), CommandDispatcherActor.ACTOR_NAME);
    }

    private static AppConfig loadConfig() throws IOException {
        Yaml yaml = new Yaml(new Constructor(AppConfig.class, new LoaderOptions()));
        InputStream inputStream = Files.newInputStream(Paths.get(CONFIG_PATH));
        return yaml.load(inputStream);
    }

    private static Function<String, ThreadFactory> getThreadFactorySupplier() {
        return (threadPrefix) -> new ThreadFactory() {
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
