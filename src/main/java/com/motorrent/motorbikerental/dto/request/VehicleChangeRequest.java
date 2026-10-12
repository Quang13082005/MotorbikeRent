package com.motorrent.motorbikerental.dto.request;
public record VehicleChangeRequest(Long motorbikeId,String reason,String pickupType,String pickupAddress,Double pickupLat,Double pickupLng,String returnType,String returnAddress,Double returnLat,Double returnLng) {}
