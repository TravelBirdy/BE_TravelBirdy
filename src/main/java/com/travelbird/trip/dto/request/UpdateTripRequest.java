package com.travelbird.trip.dto.request;
import com.travelbird.trip.entity.*;
import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
public class UpdateTripRequest {
 private String title,summary,regionCode; private LocalDate startDate,endDate; private CompanionType companionType; private List<TravelTheme> themes; private Pace pace; private Visibility visibility; private Boolean confirmDayRemoval=false; private Long version;
 private boolean titlePresent,summaryPresent,regionPresent,startPresent,endPresent,companionPresent,themesPresent,pacePresent,visibilityPresent,confirmPresent;
 public void setTitle(String v){title=v;titlePresent=true;} public void setSummary(String v){summary=v;summaryPresent=true;} public void setRegionCode(String v){regionCode=v;regionPresent=true;} public void setStartDate(LocalDate v){startDate=v;startPresent=true;} public void setEndDate(LocalDate v){endDate=v;endPresent=true;} @JsonDeserialize(using=CompanionTypeDeserializer.class) public void setCompanionType(CompanionType v){companionType=v;companionPresent=true;} public void setThemes(List<TravelTheme> v){themes=v;themesPresent=true;} @JsonDeserialize(using=PaceDeserializer.class) public void setPace(Pace v){pace=v;pacePresent=true;} public void setVisibility(Visibility v){visibility=v;visibilityPresent=true;} public void setConfirmDayRemoval(Boolean v){confirmDayRemoval=v;confirmPresent=true;} public void setVersion(Long v){version=v;}
 public String title(){return title;} public String summary(){return summary;} public String regionCode(){return regionCode;} public LocalDate startDate(){return startDate;} public LocalDate endDate(){return endDate;} public CompanionType companionType(){return companionType;} public List<TravelTheme> themes(){return themes;} public Pace pace(){return pace;} public Visibility visibility(){return visibility;} public Boolean confirmDayRemoval(){return confirmDayRemoval;} public Long version(){return version;}
 public boolean titlePresent(){return titlePresent;} public boolean summaryPresent(){return summaryPresent;} public boolean regionPresent(){return regionPresent;} public boolean startPresent(){return startPresent;} public boolean endPresent(){return endPresent;} public boolean companionPresent(){return companionPresent;} public boolean themesPresent(){return themesPresent;} public boolean pacePresent(){return pacePresent;} public boolean visibilityPresent(){return visibilityPresent;} public boolean confirmPresent(){return confirmPresent;}
 public boolean hasInvalidRequiredFields(){return version==null||(confirmPresent&&confirmDayRemoval==null)||(regionPresent&&regionCode==null);}
 public boolean routeChange(){return regionPresent||startPresent||endPresent;}
}
