package com.motorrent.motorbikerental.controller.api;
import com.motorrent.motorbikerental.service.OperationsService;
import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*;import java.util.*;import java.time.LocalDate;
@RestController @RequiredArgsConstructor
public class OperationsController {
 private final OperationsService service;
 private final com.motorrent.motorbikerental.service.TravelPlaceService travelPlaces;
 @GetMapping("/api/operations/data/travel-places") public Object travelPlaces(@RequestParam(defaultValue="") String query,@RequestParam(defaultValue="0") int page){return travelPlaces.list(query,page);}
 @GetMapping("/api/travel-places/search") public Object mapPlaces(@RequestParam(defaultValue="") String query){return travelPlaces.mapSearch(query);}
 @PostMapping("/api/operations/data/travel-places") public Object createTravelPlace(@RequestBody Map<String,Object> body){travelPlaces.save(null,body);return Map.of("message","Đã lưu địa điểm");}
 @PutMapping("/api/operations/data/travel-places/{id}") public Object updateTravelPlace(@PathVariable String id,@RequestBody Map<String,Object> body){travelPlaces.save(id,body);return Map.of("message","Đã lưu địa điểm");}
 @GetMapping("/api/operations/data/{module}") public Object list(@PathVariable String module){return service.list(module);}
 @GetMapping("/api/operations/lookups") public Object lookups(){return service.lookups();}
 @PostMapping("/api/operations/data/{module}") public Object create(@PathVariable String module,@RequestBody Map<String,Object> body){service.save(module,null,body);return Map.of("message","Đã lưu dữ liệu");}
 @PutMapping("/api/operations/data/{module}/{id}") public Object update(@PathVariable String module,@PathVariable String id,@RequestBody Map<String,Object> body){service.save(module,id,body);return Map.of("message","Đã lưu dữ liệu");}
 @GetMapping("/api/admin/reports") public Object reports(@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to){return service.reports(from,to);}
 @PutMapping("/api/admin/settings") public Object settings(@RequestBody Map<String,Object> body){service.settings(body);return Map.of("message","Đã lưu cấu hình cửa hàng");}
}
