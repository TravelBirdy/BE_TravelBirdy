package com.travelbird.trip.dto.request;
import com.fasterxml.jackson.core.*; import com.fasterxml.jackson.databind.*; import com.travelbird.global.error.ErrorCode; import com.travelbird.common.enums.Pace; import java.io.IOException;
public class PaceDeserializer extends JsonDeserializer<Pace>{@Override public Pace deserialize(JsonParser p,DeserializationContext c)throws IOException{try{return Pace.valueOf(p.getValueAsString());}catch(RuntimeException e){throw new TripEnumDeserializationException(ErrorCode.INVALID_TRIP_PACE);}}}

