package com.travelbird.ai.controller;
import com.travelbird.ai.dto.internal.*;import com.travelbird.ai.service.AiCallbackService;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/internal/ai-callbacks") public class InternalAiCallbackController{private final AiCallbackService service;public InternalAiCallbackController(AiCallbackService s){service=s;}@PostMapping("/trip-recommendations") public AiCallbackResponse callback(@RequestBody AiCallbackRequest request){return service.handle(request);}}
