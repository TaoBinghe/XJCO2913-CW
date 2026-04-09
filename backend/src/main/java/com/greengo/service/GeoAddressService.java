package com.greengo.service;

import java.math.BigDecimal;

public interface GeoAddressService {

    String reverseGeocode(BigDecimal longitude, BigDecimal latitude);
}

