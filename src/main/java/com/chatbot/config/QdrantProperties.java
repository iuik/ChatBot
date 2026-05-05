package com.chatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "qdrant")
public class QdrantProperties {

    private boolean enabled = true;

    private String host = "127.0.0.1";

    private int port = 6334;

    private String collection = "qq_bot_memory";

    private String apiKey;

    private int vectorSize = 1024;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getVectorSize() {
        return vectorSize;
    }

    public void setVectorSize(int vectorSize) {
        this.vectorSize = vectorSize;
    }
}
