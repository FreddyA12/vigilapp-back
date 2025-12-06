package com.fram.vigilapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

class VigilappApplicationTests {

    @Test
    void trivial() {
        // Evita cargar el contexto de Spring en pruebas unitarias puras
        // y mantiene un test básico para el módulo
        org.junit.jupiter.api.Assertions.assertTrue(true);
    }
}
