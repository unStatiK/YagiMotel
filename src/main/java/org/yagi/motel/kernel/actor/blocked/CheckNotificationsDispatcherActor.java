package org.yagi.motel.kernel.actor.blocked;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.http.RestClient;
import org.yagi.motel.http.request.CheckNotificationRequest;
import org.yagi.motel.http.request.ProcessNotificationRequest;
import org.yagi.motel.http.response.BaseResponse;
import org.yagi.motel.http.response.CheckNotificationResponse;
import org.yagi.motel.http.response.Notification;
import org.yagi.motel.kernel.enums.NotificationType;
import org.yagi.motel.kernel.enums.PlatformType;
import org.yagi.motel.kernel.enums.PortalNotificationLang;
import org.yagi.motel.kernel.message.CheckNotificationMessage;
import org.yagi.motel.kernel.model.container.NotificationContainer;
import org.yagi.motel.kernel.model.enums.IsProcessedState;
import org.yagi.motel.kernel.repository.StateRepository;
import org.yagi.motel.utils.UrlHelper;

@Slf4j
@SuppressWarnings("checkstyle:MissingJavadocType")
public class CheckNotificationsDispatcherActor extends AbstractActor {

  public static String ACTOR_NAME = "check-notifications-dispatcher-actor";

  private final BlockingQueue<NotificationContainer> notificationsQueue;
  private final String portalCheckNotificationUrl;
  private final String portalProcessNotificationUrl;
  private final AppConfig config;
  private final ObjectMapper mapper;
  private final StateRepository stateRepository;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public CheckNotificationsDispatcherActor(
      AppConfig config,
      StateRepository stateRepository,
      BlockingQueue<NotificationContainer> notificationsQueue) {
    this.config = config;
    this.notificationsQueue = notificationsQueue;
    this.mapper = new ObjectMapper();
    this.stateRepository = stateRepository;
    this.portalCheckNotificationUrl =
        UrlHelper.normalizeUrl(
            String.format("%s/api/v0/autobot/check_notifications", config.getPortalUrl()));
    this.portalProcessNotificationUrl =
        UrlHelper.normalizeUrl(
            String.format("%s/api/v0/autobot/process_notification", config.getPortalUrl()));
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static Props props(
      AppConfig config,
      StateRepository stateRepository,
      BlockingQueue<NotificationContainer> notificationsQueue) {
    return Props.create(
        CheckNotificationsDispatcherActor.class, config, stateRepository, notificationsQueue);
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(
            CheckNotificationMessage.class,
            message -> {
              Optional<IsProcessedState> isProcessedState = stateRepository.getIsProcessedState();
              if (isProcessedState.isPresent()
                  && IsProcessedState.ENABLE == isProcessedState.get()) {
                CheckNotificationRequest request =
                    CheckNotificationRequest.builder()
                        .apiToken(config.getAutobotApiToken())
                        .tournamentId(config.getTournamentId())
                        .lobbyId(config.getLobbyId())
                        .build();

                Optional<CheckNotificationResponse> checkNotificationResponse =
                    RestClient.sendPost(
                        mapper,
                        RestClient.preparePostRequest(portalCheckNotificationUrl, request, mapper),
                        CheckNotificationResponse.class);

                if (checkNotificationResponse.isPresent()
                    && !checkNotificationResponse.get().getNotifications().isEmpty()) {
                  for (final Notification notification :
                      checkNotificationResponse.get().getNotifications()) {
                    processNotification(notification);
                  }
                }
              }
            })
        .matchAny(o -> log.warn("received unknown message"))
        .build();
  }

  private void processNotification(Notification notification)
      throws IOException, InterruptedException {
    if (notification != null) {
      ProcessNotificationRequest processNotificationRequest =
          ProcessNotificationRequest.builder()
              .apiToken(config.getAutobotApiToken())
              .tournamentId(config.getTournamentId())
              .lobbyId(config.getLobbyId())
              .notificationId(notification.getNotificationId())
              .build();

      Optional<PlatformType> notificationPlatformType =
          PlatformType.getPlatformTypeFromCode(notification.getDestination());

      if (notificationPlatformType.isPresent()) {
        PlatformType platformType = notificationPlatformType.get();

        Optional<BaseResponse> baseResponse =
            RestClient.sendPost(
                mapper,
                RestClient.preparePostRequest(
                    portalProcessNotificationUrl, processNotificationRequest, mapper),
                BaseResponse.class);

        if (baseResponse.isPresent() && Boolean.TRUE.equals(baseResponse.get().getSuccess())) {
          String notificationMessage = notification.getMessage();
          PortalNotificationLang portalNotificationLang =
              PortalNotificationLang.valueOf(notification.getLang());
          notificationsQueue.put(
              NotificationContainer.builder()
                  .notificationType(NotificationType.TOURNAMENT)
                  .message(notificationMessage)
                  .platformType(platformType)
                  .lang(portalNotificationLang)
                  .messageType(notification.getType())
                  .build());
        }
      }
    }
  }
}
