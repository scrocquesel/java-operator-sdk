package io.javaoperatorsdk.operator;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;
import io.javaoperatorsdk.operator.junit.OperatorExtension;
import io.javaoperatorsdk.operator.processing.retry.GenericRetry;
import io.javaoperatorsdk.operator.sample.errorstatushandler.ErrorStatusHandlerTestCustomResource;
import io.javaoperatorsdk.operator.sample.errorstatushandler.ErrorStatusHandlerTestReconciler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ErrorStatusHandlerIT {

  public static final int MAX_RETRY_ATTEMPTS = 3;
  ErrorStatusHandlerTestReconciler reconciler = new ErrorStatusHandlerTestReconciler();

  @RegisterExtension
  OperatorExtension operator =
      OperatorExtension.builder()
          .withConfigurationService(DefaultConfigurationService.instance())
          .withReconciler(reconciler,
              new GenericRetry().setMaxAttempts(MAX_RETRY_ATTEMPTS).withLinearRetry())
          .build();

  @Test
  public void testErrorMessageSetEventually() {
    ErrorStatusHandlerTestCustomResource resource =
        operator.create(ErrorStatusHandlerTestCustomResource.class, createCustomResource());

    await()
        .atMost(10, TimeUnit.SECONDS)
        .pollInterval(250, TimeUnit.MICROSECONDS)
        .untilAsserted(
            () -> {
              ErrorStatusHandlerTestCustomResource res =
                  operator.get(ErrorStatusHandlerTestCustomResource.class,
                      resource.getMetadata().getName());
              assertThat(res.getStatus()).isNotNull();
              for (int i = 0; i < MAX_RETRY_ATTEMPTS + 1; i++) {
                assertThat(res.getStatus().getMessages())
                    .contains(ErrorStatusHandlerTestReconciler.ERROR_STATUS_MESSAGE + i);
              }
            });
  }

  public ErrorStatusHandlerTestCustomResource createCustomResource() {
    ErrorStatusHandlerTestCustomResource resource = new ErrorStatusHandlerTestCustomResource();
    resource.setMetadata(
        new ObjectMetaBuilder()
            .withName("error-status-test")
            .withNamespace(operator.getNamespace())
            .build());
    return resource;
  }

}