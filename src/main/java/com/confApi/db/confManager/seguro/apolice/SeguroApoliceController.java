package com.confApi.db.confManager.seguro.apolice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("manager/seguro/apolice")
public class SeguroApoliceController {

    @Autowired
    private SeguroApoliceService seguroApoliceService;

    @GetMapping("/findAll")
    public List<SeguroApolice>findAllSeguroApolice (){
        return seguroApoliceService.findAll();
    }
}
