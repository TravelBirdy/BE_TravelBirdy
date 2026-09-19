package com.travelbird.trip.dto.request;
import com.fasterxml.jackson.core.*; import com.fasterxml.jackson.databind.*; import com.travelbird.global.error.ErrorCode; import com.travelbird.common.enums.CompanionType; import java.io.IOException;
public class CompanionTypeDeserializer extends JsonDeserializer<CompanionType>{@Override public CompanionType deserialize(JsonParser p,DeserializationContext c)throws IOException{try{return CompanionType.valueOf(p.getValueAsString());}catch(RuntimeException e){throw new TripEnumDeserializationException(ErrorCode.INVALID_COMPANION_TYPE);}}}

