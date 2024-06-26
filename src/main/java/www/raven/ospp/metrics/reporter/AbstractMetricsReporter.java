package www.raven.ospp.metrics.reporter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import www.raven.ospp.metrics.collector.MetricsCollector;
import www.raven.ospp.metrics.collector.MetricsCollectorManager;
import www.raven.ospp.metrics.model.CounterMetricsSample;
import www.raven.ospp.metrics.model.GaugeMetricsSample;
import www.raven.ospp.metrics.model.MetricsSample;
import www.raven.ospp.metrics.util.SimpleInjector;

@SuppressWarnings("all")
public abstract class AbstractMetricsReporter implements MetricsReporter {

    private static final Logger log = LoggerFactory.getLogger(AbstractMetricsReporter.class);
    private static ScheduledExecutorService m_executorService;
    protected String url;
    private List<MetricsCollector> collectors;

    public AbstractMetricsReporter(String url) {
        //....
        this.url = url;
        init();
    }

    @Override
    public void init() {
        //...
        doInit();
        initCollectors();
        initScheduleMetricsCollectSync();
    }

    private void initCollectors() {
        MetricsCollectorManager metricsCollectorManager = SimpleInjector.getInstance(MetricsCollectorManager.class);
        collectors = metricsCollectorManager.getCollectors();
    }

    protected abstract void doInit();

    private void initScheduleMetricsCollectSync() {
        log.info("Start to schedule metrics collect sync job");
        m_executorService = Executors.newScheduledThreadPool(1);
        m_executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    log.info("Start to update metrics data job");
                    updateMetricsData();
                } catch (Throwable ex) {
                    //ignore
                }
            }
        }, getInitialDelay(), getPeriod(), TimeUnit.MILLISECONDS);
    }

    private void updateMetricsData() {
        for (MetricsCollector collector : collectors) {
            if (!collector.isSamplesUpdated()) {
                continue;
            }
            List<MetricsSample> export = collector.export();
            for (MetricsSample metricsSample : export) {
                registerSample(metricsSample);
            }
        }

    }

    private void registerSample(MetricsSample sample) {
        try {
            switch (sample.getType()) {
                case GAUGE:
                    registerGaugeSample((GaugeMetricsSample) sample);
                    break;
                case COUNTER:
                    registerCounterSample((CounterMetricsSample) sample);
                    break;
                default:
                    log.warn("UnSupport sample type: {}", sample.getType());
                    break;
            }
        } catch (Exception e) {
            log.error("Register sample error", e);
        }
    }

    protected String[][] getTags(MetricsSample sample) {
        Map<String, String> tags = sample.getTags();
        if (tags == null || tags.isEmpty()) {
            return new String[][] {new String[0], new String[0]};
        }
        String[] labelNames = tags.keySet().toArray(new String[0]);
        String[] labelValues = tags.values().toArray(new String[0]);
        return new String[][] {labelNames, labelValues};
    }

    protected long getPeriod() {
        return 5000;
    }

    protected long getInitialDelay() {
        return 5000;
    }

}
