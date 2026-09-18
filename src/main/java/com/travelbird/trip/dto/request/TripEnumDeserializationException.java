package com.travelbird.trip.dto.request;
import com.travelbird.global.error.ErrorCode; import java.io.IOException;
public class TripEnumDeserializationException extends IOException {private final ErrorCode errorCode;public TripEnumDeserializationException(ErrorCode code){super(code.name());errorCode=code;}public ErrorCode errorCode(){return errorCode;}}
