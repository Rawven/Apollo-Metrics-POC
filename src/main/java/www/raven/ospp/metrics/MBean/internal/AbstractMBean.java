package www.raven.ospp.metrics.MBean.internal;

import www.raven.ospp.metrics.util.JMXUtil;

public abstract class AbstractMBean {
    public AbstractMBean() {
        JMXUtil.register("www.raven.ospp:type=" + this.getClass().getSimpleName(), this);
    }
}
