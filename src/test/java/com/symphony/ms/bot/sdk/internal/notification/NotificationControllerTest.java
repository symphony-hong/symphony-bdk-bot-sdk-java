package com.symphony.ms.bot.sdk.internal.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.util.function.BiFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.symphony.ms.bot.sdk.internal.message.MessageService;
import com.symphony.ms.bot.sdk.internal.message.model.SymphonyMessage;
import com.symphony.ms.bot.sdk.internal.notification.model.NotificationRequest;

@ExtendWith(MockitoExtension.class)
public class NotificationControllerTest {

  private InterceptorChain interceptorChain;

  private TestNotificationInterceptor notificationInterceptor;

  private MessageService messageService;

  private NotificationController notificationController;

  static class TestNotificationInterceptor extends NotificationInterceptor {

    private BiFunction<NotificationRequest, SymphonyMessage, Boolean> internalProcess;

    @Override
    public boolean process(NotificationRequest notificationRequest,
        SymphonyMessage notificationMessage) {
      if (internalProcess != null) {
        return internalProcess.apply(notificationRequest, notificationMessage);
      }
      return false;
    }

    // Helper to ease changing the behavior of intercept method on each test
    private void setInternalProcess(
        BiFunction<NotificationRequest, SymphonyMessage, Boolean> func) {
      this.internalProcess = func;
    }
  }

  @BeforeEach
  public void setup() {
    notificationInterceptor = new TestNotificationInterceptor();
    interceptorChain = spy(new InterceptorChainImpl());
    interceptorChain.register(notificationInterceptor);

    messageService = mock(MessageService.class);

    notificationController = new NotificationController(
        interceptorChain, messageService);
  }

  @Test
  public void requestRejectedTest() {
    ResponseEntity<String> response = notificationController
        .receiveNotification(null, null, null);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void requestAcceptedWithContentTest() {
    notificationInterceptor.setInternalProcess((request, response) -> {
      request.setStreamId("1234");
      response.setMessage("notification received");
      return true;
    });

    ResponseEntity<String> response = notificationController
        .receiveNotification(null, null, null);

    verify(messageService, times(1)).sendMessage(anyString(), any(SymphonyMessage.class));
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void requestAcceptedNoContentTest() {
    notificationInterceptor.setInternalProcess((request, response) -> {
      request.setStreamId("1234");
      return true;
    });

    ResponseEntity<String> response = notificationController
        .receiveNotification(null, null, null);

    verify(messageService, never()).sendMessage(anyString(), any(SymphonyMessage.class));
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void requestAcceptedNoStreamIdTest() {
    notificationInterceptor.setInternalProcess((request, response) -> {
      response.setMessage("notification received");
      return true;
    });

    ResponseEntity<String> response = notificationController
        .receiveNotification(null, null, null);

    verify(messageService, never()).sendMessage(anyString(), any(SymphonyMessage.class));
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  public void errorProcessingRequestTest() {
    notificationInterceptor.setInternalProcess((request, response) -> {
      request.setStreamId("1234");
      response.setMessage("notification received");
      return true;
    });

    doThrow(new RuntimeException())
      .when(messageService)
      .sendMessage(anyString(), any(SymphonyMessage.class));

    ResponseEntity<String> response = notificationController
        .receiveNotification(null, null, null);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

}