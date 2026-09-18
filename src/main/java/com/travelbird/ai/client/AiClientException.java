package com.travelbird.ai.client;
import com.travelbird.global.error.ErrorCode;
public class AiClientException extends RuntimeException {
  private final ErrorCode errorCode;
  public AiClientException(ErrorCode errorCode,String message,Throwable cause){super(message,cause);this.errorCode=errorCode;}
  public ErrorCode errorCode(){return errorCode;}
}
