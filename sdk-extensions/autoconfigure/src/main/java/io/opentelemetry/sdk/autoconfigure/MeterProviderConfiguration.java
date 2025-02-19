/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.autoconfigure;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.internal.SdkMeterProviderUtil;
import io.opentelemetry.sdk.metrics.internal.exemplar.ExemplarFilter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

final class MeterProviderConfiguration {
  private static final Logger LOGGER = Logger.getLogger(MeterProviderConfiguration.class.getName());

  @SuppressWarnings("fallthrough")
  static void configureMeterProvider(
      SdkMeterProviderBuilder meterProviderBuilder,
      ConfigProperties config,
      ClassLoader serviceClassLoader,
      BiFunction<? super MetricExporter, ConfigProperties, ? extends MetricExporter>
          metricExporterCustomizer) {

    // Configure default exemplar filters.
    String exemplarFilter =
        config.getString("otel.metrics.exemplar.filter", "trace_based").toLowerCase(Locale.ROOT);
    switch (exemplarFilter) {
      case "none": // DEPRECATED: replaced by always_off
        LOGGER.log(
            Level.WARNING,
            "otel.metrics.exemplar.filter option \"none\" is deprecated for removal. Use \"always_off\" instead.");
      case "always_off":
        SdkMeterProviderUtil.setExemplarFilter(meterProviderBuilder, ExemplarFilter.alwaysOff());
        break;
      case "all": // DEPRECATED: replaced by always_on
        LOGGER.log(
            Level.WARNING,
            "otel.metrics.exemplar.filter option \"all\" is deprecated for removal. Use \"always_on\" instead.");
      case "always_on":
        SdkMeterProviderUtil.setExemplarFilter(meterProviderBuilder, ExemplarFilter.alwaysOn());
        break;
      case "with_sampled_trace": // DEPRECATED: replaced by trace_based
        LOGGER.log(
            Level.WARNING,
            "otel.metrics.exemplar.filter option \"with_sampled_trace\" is deprecated for removal. Use \"trace_based\" instead.");
      case "trace_based":
      default:
        SdkMeterProviderUtil.setExemplarFilter(meterProviderBuilder, ExemplarFilter.traceBased());
        break;
    }

    configureMetricReaders(config, serviceClassLoader, metricExporterCustomizer)
        .forEach(meterProviderBuilder::registerMetricReader);
  }

  static List<MetricReader> configureMetricReaders(
      ConfigProperties config,
      ClassLoader serviceClassLoader,
      BiFunction<? super MetricExporter, ConfigProperties, ? extends MetricExporter>
          metricExporterCustomizer) {
    Set<String> exporterNames = DefaultConfigProperties.getSet(config, "otel.metrics.exporter");
    if (exporterNames.contains("none")) {
      if (exporterNames.size() > 1) {
        throw new ConfigurationException(
            "otel.metrics.exporter contains none along with other exporters");
      }
      return Collections.emptyList();
    }

    if (exporterNames.isEmpty()) {
      exporterNames = Collections.singleton("otlp");
    }
    return exporterNames.stream()
        .map(
            exporterName ->
                MetricExporterConfiguration.configureReader(
                    exporterName, config, serviceClassLoader, metricExporterCustomizer))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private MeterProviderConfiguration() {}
}
