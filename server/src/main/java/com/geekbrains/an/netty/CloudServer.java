package com.geekbrains.an.netty;

import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class CloudServer  extends  BaseNettyServer{

    public CloudServer() {
        super(
                new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                new ObjectEncoder(),
                new CloudServerHandler()
        );
    }

    public static void main(String[] args) {
        new CloudServer();
    }
}
