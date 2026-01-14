package com.tagokoder.ops.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ops")
public class OpsProperties {

  private Security security = new Security();
  private Kafka kafka = new Kafka();
  private Notifications notifications = new Notifications();

  public Security getSecurity() { return security; }
  public Kafka getKafka() { return kafka; }
  public Notifications getNotifications() { return notifications; }

  public static class Security {
    private String internalToken;
    private boolean allowManualIngest;

    public String getInternalToken() { return internalToken; }
    public void setInternalToken(String internalToken) { this.internalToken = internalToken; }
    public boolean isAllowManualIngest() { return allowManualIngest; }
    public void setAllowManualIngest(boolean allowManualIngest) { this.allowManualIngest = allowManualIngest; }
  }

  public static class Kafka {
    private String bootstrapServers;
    private String groupId;
    private Topics topics = new Topics();

    public String getBootstrapServers() { return bootstrapServers; }
    public void setBootstrapServers(String bootstrapServers) { this.bootstrapServers = bootstrapServers; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public Topics getTopics() { return topics; }

    public static class Topics {
      private String paymentPosted;
      private String topupPosted;
      private String onboardingWelcome;
      private String auditEvents;

      public String getPaymentPosted() { return paymentPosted; }
      public void setPaymentPosted(String paymentPosted) { this.paymentPosted = paymentPosted; }
      public String getTopupPosted() { return topupPosted; }
      public void setTopupPosted(String topupPosted) { this.topupPosted = topupPosted; }
      public String getOnboardingWelcome() { return onboardingWelcome; }
      public void setOnboardingWelcome(String onboardingWelcome) { this.onboardingWelcome = onboardingWelcome; }
      public String getAuditEvents() { return auditEvents; }
      public void setAuditEvents(String auditEvents) { this.auditEvents = auditEvents; }
    }
  }

  public static class Notifications {
    private Dispatcher dispatcher = new Dispatcher();
    public Dispatcher getDispatcher() { return dispatcher; }

    public static class Dispatcher {
      private int batchSize;
      private long pollIntervalMs;
      private long baseBackoffMs;
      private long maxBackoffMs;
      private int maxRetries;

      public int getBatchSize() { return batchSize; }
      public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
      public long getPollIntervalMs() { return pollIntervalMs; }
      public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
      public long getBaseBackoffMs() { return baseBackoffMs; }
      public void setBaseBackoffMs(long baseBackoffMs) { this.baseBackoffMs = baseBackoffMs; }
      public long getMaxBackoffMs() { return maxBackoffMs; }
      public void setMaxBackoffMs(long maxBackoffMs) { this.maxBackoffMs = maxBackoffMs; }
      public int getMaxRetries() { return maxRetries; }
      public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    }
  }
}
