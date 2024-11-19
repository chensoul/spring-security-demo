package com.chensoul.persistence.dao;

import com.chensoul.persistence.model.User;
import com.chensoul.persistence.model.UserLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLocationRepository extends JpaRepository<UserLocation, Long> {
    UserLocation findByCountryAndUser(String country, User user);

}
