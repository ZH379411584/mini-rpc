package com.mini.rpc.provider.facade;

import com.mini.rpc.provider.annotation.RpcService;

@RpcService(serviceInterface = HelloFacade.class, serviceVersion = "1.1.0")
public class HelloFacade2Impl implements HelloFacade {
    @Override
    public String hello(String name) {
        return "hello version1.1.0" + name;
    }
}
