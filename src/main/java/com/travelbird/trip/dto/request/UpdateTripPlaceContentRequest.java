package com.travelbird.trip.dto.request;
import java.util.List;
public class UpdateTripPlaceContentRequest {
 private String memo; private List<Long> imageFileIds; private boolean memoPresent; private boolean imagesPresent;
 public String memo(){return memo;} public List<Long> imageFileIds(){return imageFileIds;} public boolean memoPresent(){return memoPresent;} public boolean imagesPresent(){return imagesPresent;}
 public void setMemo(String value){memo=value;memoPresent=true;} public void setImageFileIds(List<Long> value){imageFileIds=value;imagesPresent=true;}
}
