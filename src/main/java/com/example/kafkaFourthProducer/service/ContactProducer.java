package com.example.kafkaFourthProducer.service;

import com.example.kafkaFourthProducer.model.Address;
import com.example.kafkaFourthProducer.model.Player;
import com.example.kafkaFourthProducer.repository.PlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class ContactProducer {

    @Autowired
    private ObjectMapper mapper;

    private static Integer MESSAGE_COUNTER = 0;

    @Autowired
    private KafkaTemplate<String, String> template;

    @Autowired
    private PlayerRepository playerRepository;


    private final static String TOPIC = "testTopic";
    private Address address;
    private Player player;


    public ContactProducer() throws Exception{
        this.address = new Address("Goethestraße 14", "Dresden", 11390);
        this.player = new Player("gerrit", "manning", 123456789, address);

    }

    public Player generateNewPlayer(){
        this.address = new Address("Goethestraße 14", "Dresden", 11390);
        this.player = new Player(RandomStringUtils.random(6), RandomStringUtils.random(7), RandomUtils.nextInt(10000000, 19999999), address);
        return player;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void changePlayer(boolean withError,UUID id, String firstName, String lastName) throws Exception {
        Player foundPlayer = playerRepository.getOne(id);
        foundPlayer.setFirstName(firstName);
        foundPlayer.setLastName(lastName);
        playerRepository.save(foundPlayer);
        if (withError){
            //causes rollback
            throw new Exception("my error");
        }
        this.chat(foundPlayer);
        System.out.println(foundPlayer);
    }

    public void generateFourPlayersEvents(){
        Player hans = generateNewPlayer();
        Player lisa = generateNewPlayer();
        Player otto = generateNewPlayer();
        Player maxi = generateNewPlayer();

        List<Player> playerList = Arrays.asList(hans, lisa, otto, maxi);
        playerList.forEach(E -> {
            try{
                this.chat(E);
            } catch (IOException | ExecutionException | InterruptedException e){
                System.out.println("ERROR");
            }
        });
    }


    public void chat(Player player) throws IOException, ExecutionException, InterruptedException {

        playerRepository.save(player);

        String payload = mapper.writeValueAsString(player);

        ListenableFuture<SendResult<String, String>> sent = template.send(TOPIC,player.getId().toString(), payload);

        SendResult<String, String> sendResult = sent.get();
        Long offset = sendResult.getRecordMetadata().offset();
        Integer partition = sendResult.getRecordMetadata().partition();
        System.out.println(
                String.format("### producer: "+ "topic: %s, key: %s, payload: %s, offset: %d, partition: %d",
                        TOPIC,player.getId().toString(), payload,offset,partition));
    }
}
