package org.manu.cards.web;

import java.util.Collection;

import org.manu.cards.CardGameLauncher;
import org.manu.cards.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/bataille")
public class BatailleApi {
    protected static final Logger log = LoggerFactory.getLogger(BatailleApi.class);

    @Autowired
    HttpServletRequest httpServletRequest;

    @Autowired
    CardGameLauncher businessLogic;

    @PostMapping
    void initialize() {
        log.info("initialize");
    }

    @PreDestroy
    void destroy() {
        log.info("destroy");

    }

    @PostMapping("/play")
    ResponseEntity<String> play(@RequestParam(value = "game", defaultValue = "BATAILLE") String game,
            @RequestParam(value = "count", defaultValue = "1") Long count,
            @RequestParam(value = "maxRound", defaultValue = "10000") Long maxRound) {
        businessLogic.play(game.trim().toUpperCase(), count);
        return ResponseEntity.ok("");
    }

    @GetMapping("statistic")
    ResponseEntity<Collection<Statistics.UserStats>> getGlobalStatistics() {
        Cookie[] cookies = httpServletRequest.getCookies();
        return new ResponseEntity<>(Statistics.getStats("BATAILLE"), HttpStatus.OK);
    }

}