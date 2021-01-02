package com.example.kafkaFourthProducer.controller;

import com.example.kafkaFourthProducer.service.ContactProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


@RestController
public class ChatController {

    @Autowired
    private ContactProducer contactProducer;

    @RequestMapping(value = "/chat")
    public ResponseEntity<String> sendChatMessage() throws IOException, InterruptedException, ExecutionException {
        contactProducer.generateFourPlayersEvents();
        Thread.sleep(1000);
        return new ResponseEntity<>("chat successfully", HttpStatus.ACCEPTED);
    }

    @RequestMapping(value = "/chatLoop")
    public ResponseEntity<String> sendChatMessagesInLoop(@RequestParam("loop") boolean loop,
                                                         @RequestParam("duration") int duration,
                                                         @RequestParam("sleep") int sleepMillis) throws IOException, InterruptedException, ExecutionException {
        if (!loop) {
                contactProducer.generateFourPlayersEvents();
            return new ResponseEntity<>("chat successfully", HttpStatus.ACCEPTED);
        } else {
            int start = 0;
            while (start <= duration) {
                contactProducer.generateFourPlayersEvents();

                Thread.sleep(sleepMillis);
                start++;
            }
            return new ResponseEntity<>("chat successfully", HttpStatus.ACCEPTED);
        }
    }

    @RequestMapping(value = "/chatFor")
    public ResponseEntity<String> changePlayers(@RequestParam("withError") boolean withError,
                                                @RequestParam("playerId") String playerId,
                                                @RequestParam("firstName") String firstName,
                                                @RequestParam("lastName") String lastName) throws Exception {

        contactProducer.changePlayer(withError,UUID.fromString(playerId),firstName,lastName);
        return new ResponseEntity<>("player change successfully", HttpStatus.ACCEPTED);
    }

}
