/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.as;

import io.opentelemetry.internal.testing.slf4j.SuppressLogger;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.internal.exemplar.AlwaysOffFilter;
import io.opentelemetry.sdk.metrics.internal.exemplar.AlwaysOnFilter;
import io.opentelemetry.sdk.metrics.internal.exemplar.ExemplarFilter;
import io.opentelemetry.sdk.metrics.internal.exemplar.TraceBasedExemplarFilter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;

class MeterProviderConfigurationTest {

  @Test
  // Suppress log warnings for deprecated exemplar filter options
  @SuppressLogger(MeterProviderConfiguration.class)
  void configureMeterProvider_ConfiguresExemplarFilter() {
    assertExemplarFilter(Collections.emptyMap()).isInstanceOf(TraceBasedExemplarFilter.class);
    assertExemplarFilter(Collections.singletonMap("otel.metrics.exemplar.filter", "foo"))
        .isInstanceOf(TraceBasedExemplarFilter.class);
    assertExemplarFilter(Collections.singletonMap("otel.metrics.exemplar.filter", "trace_based"))
        .isInstanceOf(TraceBasedExemplarFilter.class);
    assertExemplarFilter(Collections.singletonMap("otel.metrics.exemplar.filter", "Trace_based"))
        .isInstanceOf(TraceBasedExemplarFilter.class);
    assertExemplarFilter(
            Collections.singletonMap("otel.metrics.exemplar.filter", "with_sampled_trace"))
        .isInstanceOf(TraceBasedExemplarFilter.class);
    assertExemplarFilter(Collections.singletonMap("otel.metrics.exemplar.filter", "always_off"))
        .isInstanceOf(AlwaysOffFilter.class);
    assertExemplarFilter(Collections.singletonMap("otel.metrics.exemplar.filter", "always_Off"))
        .isInstanceOf(AlwaysOffFilter.class);
    assertExemplarFilter(Collections.singletonMap("otel.metrics.exemplar.filter", "none"))
        .isInstanceOf(AlwaysOffFilter.class);
    assertExemplarFilter(Collections.singletonMap("otel.metrics.exemplar.filter", "always_on"))
        .isInstanceOf(AlwaysOnFilter.class);
    assertExemplarFilter(Collections.singletonMap("otel.metrics.exemplar.filter", "ALWAYS_ON"))
        .isInstanceOf(AlwaysOnFilter.class);
    assertExemplarFilter(Collections.singletonMap("otel.metrics.exemplar.filter", "all"))
        .isInstanceOf(AlwaysOnFilter.class);
  }

  private static ObjectAssert<ExemplarFilter> assertExemplarFilter(Map<String, String> config) {
    Map<String, String> configWithDefault = new HashMap<>(config);
    configWithDefault.put("otel.metrics.exporter", "none");
    SdkMeterProviderBuilder builder = SdkMeterProvider.builder();
    MeterProviderConfiguration.configureMeterProvider(
        builder,
        DefaultConfigProperties.createForTest(configWithDefault),
        MeterProviderConfigurationTest.class.getClassLoader(),
        (a, b) -> a);
    return assertThat(builder)
        .extracting("exemplarFilter", as(InstanceOfAssertFactories.type(ExemplarFilter.class)));
  }
}
