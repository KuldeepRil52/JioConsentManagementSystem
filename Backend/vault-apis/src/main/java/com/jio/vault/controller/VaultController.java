package com.jio.vault.controller;
import com.jio.vault.service.VaultService;
import com.jio.vault.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;



@RestController
public class VaultController {

    private final VaultService certService;

    public VaultController(VaultService certService) {
        this.certService = certService;
    }

    @GetMapping("/health")
    public String hello() {
        return certService.checkHealth();
    }

}

