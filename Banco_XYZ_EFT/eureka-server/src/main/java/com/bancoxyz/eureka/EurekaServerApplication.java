package com.bancoxyz.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Servidor de descubrimiento (Service Discovery) para Banco XYZ.
 * Todos los microservicios y BFFs se registran aqui dinamicamente,
 * permitiendo balanceo de carga y enrutamiento sin hostnames fijos.
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
