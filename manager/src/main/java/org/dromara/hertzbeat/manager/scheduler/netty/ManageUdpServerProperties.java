package org.dromara.hertzbeat.manager.scheduler.netty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "udp")
public class ManageUdpServerProperties {

    private boolean enabled = false;

    private Integer serverPort;

    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getServerPort() {
        return serverPort;
    }
    public void setServerPort(Integer getServerPort) {
        this.serverPort = getServerPort;
    }
}