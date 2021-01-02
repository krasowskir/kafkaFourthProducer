package com.example.kafkaFourthProducer.repository;

import com.example.kafkaFourthProducer.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public interface PlayerRepository extends JpaRepository<Player, UUID> {

}
