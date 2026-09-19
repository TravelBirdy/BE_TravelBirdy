package com.travelbird.ai.service;
import com.travelbird.global.error.*;
public class AiResultValidationException extends BusinessException{private final AiJobFailureCode failureCode;public AiResultValidationException(AiJobFailureCode code){super(ErrorCode.INVALID_AI_RESPONSE);failureCode=code;}public AiJobFailureCode failureCode(){return failureCode;}}
