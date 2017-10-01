package com.cloudnative.reservationclient.apigateway;

import com.cloudnative.reservationclient.channels.ReservationChannels;
import com.cloudnative.reservationclient.dto.Reservation;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reservations")
public class ReservationApiGatewayRestController {

    private final RestTemplate restTemplate;
    private final MessageChannel out;

    public ReservationApiGatewayRestController(RestTemplate restTemplate, ReservationChannels channels) {
        this.restTemplate = restTemplate;
        this.out = channels.output();
    }

    @PostMapping("/write")
    public void write(@RequestBody Reservation reservation) {
        Message<String> msg = MessageBuilder.withPayload(reservation.getReservationName()).build();
        this.out.send(msg);
    }

    public Collection<String> backup() {
        return new ArrayList<>();
    }

    @HystrixCommand(fallbackMethod = "backup")
    @GetMapping("/names")
    public Collection<String> names() {

        ParameterizedTypeReference<Resources<Reservation>> ptr = new ParameterizedTypeReference<Resources<Reservation>>() {
                  };
        ResponseEntity<Resources<Reservation>> responseEntity = this.restTemplate.exchange("http://reservation-service/reservations", HttpMethod.GET,
                null, ptr);

        return responseEntity
                .getBody()
                .getContent()
                .stream()
                .map(reservation -> reservation.getReservationName())
                .collect(Collectors.toList());
    }
}
