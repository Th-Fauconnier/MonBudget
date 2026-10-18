package com.monbudget.model;

public record PaymentMethod(
        Long id,
        String name // CARTE, VIREMENT, ESPECES, etc.
) {}