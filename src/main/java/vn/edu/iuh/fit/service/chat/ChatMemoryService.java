package vn.edu.iuh.fit.service.chat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/** Simple in-memory conversation buffer that keeps the last few turns per conversation. */
@Component
public class ChatMemoryService {

  private static final int MAX_MESSAGES = 10; // roughly 5 turns (user+assistant)
  private final Map<String, Deque<ChatMessage>> conversations = new ConcurrentHashMap<>();

  public List<ChatMessage> getRecentMessages(String conversationId) {
    if (!StringUtils.hasText(conversationId)) {
      return Collections.emptyList();
    }
    Deque<ChatMessage> deque = conversations.get(conversationId);
    if (CollectionUtils.isEmpty(deque)) {
      return Collections.emptyList();
    }
    return new ArrayList<>(deque);
  }

  public void append(String conversationId, ChatMessage... messages) {
    if (!StringUtils.hasText(conversationId) || messages == null || messages.length == 0) {
      return;
    }
    conversations.compute(
        conversationId,
        (key, existing) -> {
          Deque<ChatMessage> deque =
              existing == null ? new ArrayDeque<>() : new ArrayDeque<>(existing);
          for (ChatMessage message : messages) {
            if (message == null || !StringUtils.hasText(message.content())) {
              continue;
            }
            deque.addLast(message);
          }
          while (deque.size() > MAX_MESSAGES) {
            deque.pollFirst();
          }
          return deque;
        });
  }

  public record ChatMessage(Role role, String content) {
    public ChatMessage {
      Objects.requireNonNull(role, "role must not be null");
    }
  }

  public enum Role {
    USER,
    ASSISTANT
  }
}
