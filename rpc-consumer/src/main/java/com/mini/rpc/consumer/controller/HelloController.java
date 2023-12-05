package com.mini.rpc.consumer.controller;

import com.mini.rpc.consumer.annotation.RpcReference;
import com.mini.rpc.provider.facade.HelloFacade;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @SuppressWarnings({"SpringJavaAutowiredFieldsWarningInspection", "SpringJavaInjectionPointsAutowiringInspection"})
    @RpcReference(serviceVersion = "1.0.0", timeout = 3000)
    private HelloFacade helloFacade;

    @RpcReference(serviceVersion = "1.1.0", timeout = 3000)
    @SuppressWarnings({"SpringJavaAutowiredFieldsWarningInspection", "SpringJavaInjectionPointsAutowiringInspection"})
    private HelloFacade helloFacade2;

    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    public String sayHello() {
        return helloFacade.hello("mini rpc");
    }


    @RequestMapping(value = "/hello2", method = RequestMethod.GET)
    public String sayHello2() {
        return helloFacade2.hello("mini rpc");
    }
}
