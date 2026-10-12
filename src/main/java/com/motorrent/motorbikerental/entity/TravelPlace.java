package com.motorrent.motorbikerental.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="travel_places") @Data @NoArgsConstructor
public class TravelPlace {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private String name,area,category,phone,reviewedAt,sourceStatus;
 @Column(length=1000) private String address;
 @Column(length=5000) private String description;
 @Column(length=250,unique=true) private String externalId;
 @Column(length=2000) private String imageUrl,mapsUrl;
 @Column(length=1600) private String searchText;
 @PrePersist @PreUpdate public void updateSearchText(){searchText=normalize(name)+" | "+normalize(area)+" | "+normalize(address);}
 public static String normalize(String value){return java.text.Normalizer.normalize(java.util.Objects.toString(value,""),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").replace('đ','d').replace('Đ','D').toLowerCase(java.util.Locale.ROOT);}
 @Column(columnDefinition="TEXT") private String imageCredit,imageSourceUrl,imageLicenseUrl;
 public String getSafeImageSourceUrl(){return safeHttps(imageSourceUrl);}
 public String getSafeImageLicenseUrl(){return safeHttps(imageLicenseUrl);}
 private Double lat,lng;
 @Column(nullable=false) private boolean published;
 public String getSafeImageUrl(){return safeHttps(imageUrl);}
 public String getSafeMapsUrl(){return safeHttps(mapsUrl);}
 private static String safeHttps(String value){
  if(value==null || value.isBlank())return null;
  try{var uri=java.net.URI.create(value);return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost()!=null && uri.getUserInfo()==null?value:null;}
  catch(IllegalArgumentException e){return null;}
 }
}
