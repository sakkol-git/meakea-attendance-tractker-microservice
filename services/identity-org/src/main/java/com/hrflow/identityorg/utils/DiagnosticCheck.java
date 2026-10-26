package com.hrflow.identityorg.utils;

import jakarta.persistence.*;

@Entity
@Table(name = "diagnostic_check")
public class DiagnosticCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "check_name", nullable = false, unique = true)
    String checkName;

    protected DiagnosticCheck() {}

    DiagnosticCheck(String checkName) {
        this.checkName = checkName;
    }
}