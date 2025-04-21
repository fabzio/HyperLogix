package com.hyperlogix.server.infrastructure;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class MainController {

    @GetMapping("")
    public String sayHello() {
        return "Hello World! From HyperLogix Server!";
    }

}
