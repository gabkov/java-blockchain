package com.gabkov.blockchain.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ApiController {

    @RequestMapping(value = "/")
    public String index() {
        return "index";
    }

}
