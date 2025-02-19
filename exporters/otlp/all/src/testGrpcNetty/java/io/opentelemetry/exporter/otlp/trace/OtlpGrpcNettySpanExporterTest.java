/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.exporter.otlp.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.grpc.inprocess.InProcessChannelBuilder;
import io.opentelemetry.exporter.internal.grpc.UpstreamGrpcExporter;
import io.opentelemetry.exporter.internal.marshal.Marshaler;
import io.opentelemetry.exporter.internal.otlp.traces.ResourceSpansMarshaler;
import io.opentelemetry.exporter.internal.retry.RetryPolicy;
import io.opentelemetry.exporter.internal.retry.RetryUtil;
import io.opentelemetry.exporter.otlp.testing.internal.AbstractGrpcTelemetryExporterTest;
import io.opentelemetry.exporter.otlp.testing.internal.FakeTelemetryUtil;
import io.opentelemetry.exporter.otlp.testing.internal.ManagedChannelTelemetryExporterBuilder;
import io.opentelemetry.exporter.otlp.testing.internal.TelemetryExporterBuilder;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.Closeable;
import java.util.List;
import org.junit.jupiter.api.Test;

class OtlpGrpcNettySpanExporterTest
    extends AbstractGrpcTelemetryExporterTest<SpanData, ResourceSpans> {

  OtlpGrpcNettySpanExporterTest() {
    super("span", ResourceSpans.getDefaultInstance());
  }

  @Test
  void testSetRetryPolicyOnDelegate() {
    assertThatCode(
            () ->
                RetryUtil.setRetryPolicyOnDelegate(
                    OtlpGrpcSpanExporter.builder(), RetryPolicy.getDefault()))
        .doesNotThrowAnyException();
  }

  @Test
  @SuppressWarnings("deprecation") // testing deprecated feature
  void usingGrpc() throws Exception {
    try (Closeable exporter =
        OtlpGrpcSpanExporter.builder()
            .setChannel(InProcessChannelBuilder.forName("test").build())
            .build()) {
      assertThat(exporter).extracting("delegate").isInstanceOf(UpstreamGrpcExporter.class);
    }
  }

  @Override
  protected TelemetryExporterBuilder<SpanData> exporterBuilder() {
    return ManagedChannelTelemetryExporterBuilder.wrap(
        TelemetryExporterBuilder.wrap(OtlpGrpcSpanExporter.builder()));
  }

  @Override
  protected SpanData generateFakeTelemetry() {
    return FakeTelemetryUtil.generateFakeSpanData();
  }

  @Override
  protected Marshaler[] toMarshalers(List<SpanData> telemetry) {
    return ResourceSpansMarshaler.create(telemetry);
  }
}
