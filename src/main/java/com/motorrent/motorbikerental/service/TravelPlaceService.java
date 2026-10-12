package com.motorrent.motorbikerental.service;
import com.motorrent.motorbikerental.entity.TravelPlace;
import com.motorrent.motorbikerental.repository.TravelPlaceRepository;
import com.motorrent.motorbikerental.security.CurrentUser;
import com.motorrent.motorbikerental.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;

@Service @RequiredArgsConstructor
public class TravelPlaceService {
 private final TravelPlaceRepository places;private final CurrentUser current;
 private void admin(){if(current.get().getRole()!=UserRole.ADMIN)throw new AccessDeniedException("Chỉ quản trị viên được cập nhật địa điểm");}
 @Transactional(readOnly=true) public List<TravelPlace> featured(){var result=new ArrayList<>(places.featured(org.springframework.data.domain.PageRequest.of(0,6)));for(var p:published("",0,6).getContent())if(result.size()<6 && result.stream().noneMatch(x->x.getId().equals(p.getId())))result.add(p);return result;}
 private String pattern(String query){String q=TravelPlace.normalize(Objects.toString(query,"").trim());return q.isEmpty()?"":"%"+q.replace("!","!!").replace("%","!%").replace("_","!_")+"%";}
 @Transactional(readOnly=true) public org.springframework.data.domain.Page<TravelPlace> published(String query,int page,int size){return places.search(true,pattern(query),org.springframework.data.domain.PageRequest.of(Math.max(0,page),Math.min(50,Math.max(1,size))));}
 @Transactional(readOnly=true) public List<Map<String,Object>> mapSearch(String query){return places.mapSearch(pattern(query),org.springframework.data.domain.PageRequest.of(0,12)).stream().map(p->OperationsService.row("name",p.getName(),"lat",p.getLat(),"lng",p.getLng())).toList();}
 @Transactional(readOnly=true) public Map<String,Object> list(String query,int page){
  admin();var result=places.search(false,pattern(query),org.springframework.data.domain.PageRequest.of(Math.max(0,page),50));
  var rows=result.getContent().stream().map(p->OperationsService.row("id",p.getId(),"name",p.getName(),"area",p.getArea(),"category",p.getCategory(),"address",p.getAddress(),"description",p.getDescription(),"imageUrl",p.getImageUrl(),"imageCredit",p.getImageCredit(),"imageSourceUrl",p.getImageSourceUrl(),"imageLicenseUrl",p.getImageLicenseUrl(),"mapsUrl",p.getMapsUrl(),"phone",p.getPhone(),"lat",p.getLat(),"lng",p.getLng(),"published",Boolean.toString(p.isPublished()))).toList();
  return Map.of("items",rows,"total",result.getTotalElements(),"pages",result.getTotalPages(),"page",result.getNumber());
 }
 private String text(Map<String,Object>d,String key,int max,boolean required){var v=Objects.toString(d.get(key),"").trim();if((required && v.isBlank())||v.length()>max)throw new IllegalArgumentException("Thông tin "+key+" không hợp lệ");return v;}
 @Transactional public void save(String id,Map<String,Object>d){
  admin();var p=id==null?new TravelPlace():places.findById(Long.valueOf(id)).orElseThrow();
  p.setName(text(d,"name",255,true));p.setAddress(text(d,"address",1000,true));p.setArea(text(d,"area",255,false));p.setCategory(text(d,"category",255,false));p.setDescription(text(d,"description",5000,false));p.setPhone(text(d,"phone",255,false));
  if(!Objects.equals(p.getImageUrl(),Objects.toString(d.get("imageUrl"),"").trim())){p.setImageCredit(null);p.setImageSourceUrl(null);p.setImageLicenseUrl(null);}
  if(d.containsKey("imageCredit"))p.setImageCredit(text(d,"imageCredit",2000,false));
  if(d.containsKey("imageSourceUrl"))p.setImageSourceUrl(text(d,"imageSourceUrl",2000,false));
  if(d.containsKey("imageLicenseUrl"))p.setImageLicenseUrl(text(d,"imageLicenseUrl",2000,false));
  if(p.getImageSourceUrl()!=null && !p.getImageSourceUrl().isBlank() && p.getSafeImageSourceUrl()==null || p.getImageLicenseUrl()!=null && !p.getImageLicenseUrl().isBlank() && p.getSafeImageLicenseUrl()==null)throw new IllegalArgumentException("Nguồn và giấy phép ảnh phải dùng HTTPS");
  p.setMapsUrl(text(d,"mapsUrl",2000,false));p.setImageUrl(text(d,"imageUrl",2000,false));
  if((!p.getMapsUrl().isBlank() && p.getSafeMapsUrl()==null)||(!p.getImageUrl().isBlank() && p.getSafeImageUrl()==null))throw new IllegalArgumentException("Liên kết ảnh và bản đồ phải là địa chỉ HTTPS hợp lệ");
  String state=text(d,"published",5,true);if(!Set.of("true","false").contains(state))throw new IllegalArgumentException("Trạng thái xuất bản không hợp lệ");p.setPublished(Boolean.parseBoolean(state));
  if(d.containsKey("lat") || d.containsKey("lng")){
   String lat=text(d,"lat",40,false),lng=text(d,"lng",40,false);
   if(lat.isEmpty() && lng.isEmpty()){p.setLat(null);p.setLng(null);}
   else try{double latitude=Double.parseDouble(lat),longitude=Double.parseDouble(lng);if(!Double.isFinite(latitude)||!Double.isFinite(longitude)||Math.abs(latitude)>90||Math.abs(longitude)>180)throw new IllegalArgumentException();p.setLat(latitude);p.setLng(longitude);}
   catch(IllegalArgumentException e){throw new IllegalArgumentException("Nhập đủ vĩ độ và kinh độ hợp lệ hoặc để trống cả hai");}
  }
  // Preserve source verification metadata: editing content does not imply field verification.
  places.save(p);
 }
}
