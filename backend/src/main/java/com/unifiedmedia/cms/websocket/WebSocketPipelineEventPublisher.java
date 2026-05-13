package com.unifiedmedia.cms.websocket;

import com.unifiedmedia.cms.pipeline.payload.NodeLogEvent;
import com.unifiedmedia.cms.pipeline.spi.PipelineEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketPipelineEventPublisher implements PipelineEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onNodeLogCreated(UUID taskId, UUID jobId, NodeLogEvent logEvent) {
        messagingTemplate.convertAndSend("/topic/task/" + taskId, Map.of(
                "type", "nodeLog",
                "taskId", taskId,
                "jobId", jobId,
                "data", logEvent
        ));
        messagingTemplate.convertAndSend("/topic/job/" + jobId, Map.of(
                "type", "nodeLog",
                "taskId", taskId,
                "data", logEvent
        ));
        log.debug("[WS] Pushed nodeLog to task/{} and job/{}", taskId, jobId);
    }

    @Override
    public void onTaskStatusChanged(UUID taskId, UUID jobId, String status, String errorMessage) {
        messagingTemplate.convertAndSend("/topic/task/" + taskId, Map.of(
                "type", "statusChanged",
                "taskId", taskId,
                "status", status,
                "errorMessage", errorMessage != null ? errorMessage : ""
        ));
        messagingTemplate.convertAndSend("/topic/job/" + jobId, Map.of(
                "type", "taskStatusChanged",
                "taskId", taskId,
                "status", status
        ));
        log.debug("[WS] Pushed taskStatusChanged for task/{} to job/{}", taskId, jobId);
    }

    @Override
    public void onJobStatusChanged(UUID jobId, String status) {
        messagingTemplate.convertAndSend("/topic/job/" + jobId, Map.of(
                "type", "jobStatusChanged",
                "jobId", jobId,
                "status", status
        ));
        log.debug("[WS] Pushed jobStatusChanged for job/{}", jobId);
    }

    @Override
    public void onNodeLogsCleared(UUID taskId, UUID jobId) {
        messagingTemplate.convertAndSend("/topic/task/" + taskId, Map.of(
                "type", "logsCleared",
                "taskId", taskId
        ));
        messagingTemplate.convertAndSend("/topic/job/" + jobId, Map.of(
                "type", "logsCleared",
                "taskId", taskId
        ));
        log.debug("[WS] Pushed logsCleared for task/{} and job/{}", taskId, jobId);
    }
}
