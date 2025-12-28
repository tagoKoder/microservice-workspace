package com.tagokoder.ops.domain.port.out;

import com.tagokoder.ops.domain.model.Channel;

import java.util.Map;

public interface NotificationSenderPort {
  Channel channel();
  void send(String topic, Map<String, Object> payload) throws Exception;
}
