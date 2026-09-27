package com.library.util;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class JpaUtil {

    private static final EntityManagerFactory EMF = Persistence.createEntityManagerFactory("libraryPU");

    private JpaUtil() {
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        return EMF;
    }

    public static void close() {
        if (EMF.isOpen()) {
            EMF.close();
        }
    }
}
