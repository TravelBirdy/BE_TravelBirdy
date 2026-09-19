package com.travelbird.ai.dto.request;

import java.util.List;

public class UpdateAiTripPreviewRequest {
  private String tripTitle;
  private String summary;
  private List<String> hashtags;
  private List<UpdateAiPreviewDayRequest> days;
  private Long version;
  private boolean tripTitlePresent;
  private boolean summaryPresent;
  private boolean hashtagsPresent;
  private boolean daysPresent;

  public void setTripTitle(String value) { tripTitle = value; tripTitlePresent = true; }
  public void setSummary(String value) { summary = value; summaryPresent = true; }
  public void setHashtags(List<String> value) { hashtags = value; hashtagsPresent = true; }
  public void setDays(List<UpdateAiPreviewDayRequest> value) { days = value; daysPresent = true; }
  public void setVersion(Long value) { version = value; }
  public String tripTitle() { return tripTitle; }
  public String summary() { return summary; }
  public List<String> hashtags() { return hashtags; }
  public List<UpdateAiPreviewDayRequest> days() { return days; }
  public Long version() { return version; }
  public boolean tripTitlePresent() { return tripTitlePresent; }
  public boolean summaryPresent() { return summaryPresent; }
  public boolean hashtagsPresent() { return hashtagsPresent; }
  public boolean daysPresent() { return daysPresent; }
  public boolean hasExplicitNull() { return (tripTitlePresent && tripTitle == null) || (summaryPresent && summary == null) || (hashtagsPresent && hashtags == null) || (daysPresent && days == null); }
}