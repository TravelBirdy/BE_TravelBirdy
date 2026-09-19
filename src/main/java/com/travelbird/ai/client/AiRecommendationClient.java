package com.travelbird.ai.client;
import com.travelbird.ai.dto.internal.*;
public interface AiRecommendationClient{void sync(PlaceSyncRequest request);AiServerAcceptedResponse recommend(RecommendationJobRequest request);}
