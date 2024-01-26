package org.yagi.motel.actor.blocked;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.yagi.motel.bot.NotificationType;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.http.RestClient;
import org.yagi.motel.message.CheckNotificationMessage;
import org.yagi.motel.model.container.NotificationContainer;
import org.yagi.motel.request.CheckNotificationRequest;
import org.yagi.motel.request.ProcessNotificationRequest;
import org.yagi.motel.response.BaseResponse;
import org.yagi.motel.response.CheckNotificationResponse;
import org.yagi.motel.response.Notification;
import org.yagi.motel.utils.UrlHelper;

import java.util.concurrent.BlockingQueue;

@Slf4j
public class CheckNotificationsDispatcherActor extends AbstractActor {

    public static String ACTOR_NAME = "check-notifications-dispatcher-actor";

    private final BlockingQueue<NotificationContainer> notificationsQueue;
    private final String portalCheckNotificationUrl;
    private final String portalProcessNotificationUrl;
    private final AppConfig config;
    private final ObjectMapper mapper;

    public CheckNotificationsDispatcherActor(AppConfig config, BlockingQueue<NotificationContainer> notificationsQueue) {
        this.config = config;
        this.notificationsQueue = notificationsQueue;
        this.mapper = new ObjectMapper();
        this.portalCheckNotificationUrl =
                UrlHelper.normalizeUrl(String.format("%s/api/v0/autobot/check_notifications", config.getPortalUrl()));
        this.portalProcessNotificationUrl =
                UrlHelper.normalizeUrl(String.format("%s/api/v0/autobot/process_notification", config.getPortalUrl()));
    }

    public static Props props(AppConfig config, BlockingQueue<NotificationContainer> notificationsQueue) {
        return Props.create(CheckNotificationsDispatcherActor.class, config, notificationsQueue);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        CheckNotificationMessage.class,
                        message -> {
                            CheckNotificationRequest request = CheckNotificationRequest.builder()
                                    .apiToken(config.getAutobotApiToken())
                                    .tournamentId(config.getTournamentId())
                                    .lobbyId(config.getLobbyId())
                                    .build();

                            CheckNotificationResponse checkNotificationResponse =
                                    RestClient.sendPost(mapper,
                                            RestClient.preparePostRequest(portalCheckNotificationUrl, request, mapper),
                                            CheckNotificationResponse.class);

                            if (checkNotificationResponse != null && !checkNotificationResponse.getNotifications().isEmpty()) {
                                Notification notification = checkNotificationResponse.getNotifications()
                                        .stream().findFirst().orElse(null);

                                if (notification != null) {
                                    ProcessNotificationRequest processNotificationRequest =
                                            ProcessNotificationRequest.builder()
                                                    .apiToken(config.getAutobotApiToken())
                                                    .tournamentId(config.getTournamentId())
                                                    .lobbyId(config.getLobbyId())
                                                    .notificationId(notification.getNotificationId())
                                                    .build();
                                    BaseResponse baseResponse = RestClient.sendPost(mapper,
                                            RestClient.preparePostRequest(portalProcessNotificationUrl, processNotificationRequest, mapper),
                                            BaseResponse.class);

                                    if (baseResponse != null && Boolean.TRUE.equals(baseResponse.getSuccess())) {
                                        String notificationMessage = notification.getMessage();
                                        notificationsQueue.put(NotificationContainer.builder()
                                                .notificationType(NotificationType.TOURNAMENT)
                                                .message(notificationMessage)
                                                .build());
                                    }
                                }
                            }
                        })
                .matchAny(o -> log.warn("received unknown message"))
                .build();
    }
}
