package www.raven.ospp.metrics.reporter.internal;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.PushGateway;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import www.raven.ospp.metrics.model.CounterMetricsSample;
import www.raven.ospp.metrics.model.GaugeMetricsSample;
import www.raven.ospp.metrics.reporter.AbstractMetricsReporter;
import www.raven.ospp.metrics.reporter.MetricsReporter;

@SuppressWarnings("all")
public class PrometheusMetricReporter extends AbstractMetricsReporter implements MetricsReporter {
    private static final Logger logger = LoggerFactory.getLogger(
        PrometheusMetricReporter.class);
    private final static ScheduledExecutorService m_executorService;
    private final CollectorRegistry registry;
    private PushGateway pushGateway;

    static {
        m_executorService = Executors.newScheduledThreadPool(1);
    }

    public PrometheusMetricReporter(String url) {
        super(url);
        this.registry = new CollectorRegistry();
    }

    @Override
    public void doInit() {
        if(url != null && !url.isEmpty()) {
            pushGateway = new PushGateway(url);
            initSchdulePushJob();
        }
    }

    protected void initSchdulePushJob() {
        m_executorService.scheduleAtFixedRate(() -> {
            try {
                pushGateway.pushAdd(registry, "apollo");
            } catch (Throwable ex) {
                logger.error("Push metrics to Prometheus failed", ex);
            }
        }, getInitialDelay(), getPeriod(), TimeUnit.MILLISECONDS);
    }

    protected void openMetricsPort() {
        // open metrics port
    }

    @Override
    public void registerCounterSample(CounterMetricsSample sample) {
        Counter counter = Counter.build()
            .name(sample.getName())
            .help("Counter for " + sample.getName())
            .register(registry);
        counter.inc(sample.getValue());
    }

    @Override
    public void registerGaugeSample(GaugeMetricsSample sample) {
        Gauge gauge = Gauge.build()
            .name(sample.getName())
            .help("Gauge for " + sample.getName())
            .register(registry);
        gauge.set(sample.getApplyValue());
    }

    @Override
    public String response() {
        StringWriter writer = new StringWriter();
        try {
            TextFormat.write004(writer, registry.metricFamilySamples());
        } catch (IOException e) {
            logger.error("Write metrics to Prometheus format failed", e);
        }
        return writer.toString();
    }
}