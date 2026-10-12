package com.motorrent.motorbikerental.repository;
import com.motorrent.motorbikerental.entity.TravelPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TravelPlaceRepository extends JpaRepository<TravelPlace,Long>{
 @org.springframework.data.jpa.repository.Query("select p from TravelPlace p where p.published=true and p.imageUrl is not null and p.imageUrl<>'' order by p.name,p.id")
 List<TravelPlace> featured(org.springframework.data.domain.Pageable page);
 @org.springframework.data.jpa.repository.Query("select p from TravelPlace p where (:onlyPublished=false or p.published=true) and (:q='' or p.searchText like :q escape '!' or lower(p.name) like :q escape '!') order by p.name,p.id")
 org.springframework.data.domain.Page<TravelPlace> search(boolean onlyPublished,String q,org.springframework.data.domain.Pageable page);
 @org.springframework.data.jpa.repository.Query("select p from TravelPlace p where p.published=true and p.lat between -90 and 90 and p.lng between -180 and 180 and (:q='' or p.searchText like :q escape '!') order by p.name,p.id")
 List<TravelPlace> mapSearch(String q,org.springframework.data.domain.Pageable page);
}
