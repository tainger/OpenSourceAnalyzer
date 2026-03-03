package com.ai.analyzer.service;

import com.ai.analyzer.model.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatMemoryService {

    private final Map<String, List<ChatMessage>> conversationHistory;
    private static final int MAX_HISTORY_MESSAGES = 20;

    public ChatMemoryService() {
        this.conversationHistory = new ConcurrentHashMap<>();
        log.info("Chat memory service initialized");
    }

    public void addMessage(String conversationId, String role, String content) {
        ChatMessage message = ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .repositoryId(conversationId)
                .role(role)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();

        conversationHistory.computeIfAbsent(conversationId, k -> new ArrayList<>()).add(message);

        List<ChatMessage> messages = conversationHistory.get(conversationId);
        if (messages.size() > MAX_HISTORY_MESSAGES) {
            messages.remove(0);
            log.debug("Truncated conversation history for {}", conversationId);
        }

        log.debug("Added message to conversation {}, role: {}", conversationId, role);
    }

    public List<ChatMessage> getConversationHistory(String conversationId) {
        return conversationHistory.getOrDefault(conversationId, Collections.emptyList());
    }

    public String buildConversationContext(String conversationId) {
        List<ChatMessage> messages = getConversationHistory(conversationId);
        if (messages.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("## 对话历史\n\n");

        for (ChatMessage message : messages) {
            String roleDisplay = "user".equals(message.getRole()) ? "用户" : "助手";
            context.append(String.format("**%s**: %s\n\n", roleDisplay, message.getContent()));
        }

        return context.toString();
    }

    public void clearConversation(String conversationId) {
        conversationHistory.remove(conversationId);
        log.info("Cleared conversation history for {}", conversationId);
    }

    public void clearAllConversations() {
        conversationHistory.clear();
        log.info("Cleared all conversation histories");
    }
}
