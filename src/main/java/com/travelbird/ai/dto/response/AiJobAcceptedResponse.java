package com.travelbird.ai.dto.response;import java.time.LocalDateTime;
public record AiJobAcceptedResponse(Long jobId,String status,LocalDateTime requestedAt,String statusUrl){}
