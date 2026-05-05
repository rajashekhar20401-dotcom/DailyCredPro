//package com.unqiuehire.kashflow.entity;
//
//import com.unqiuehire.kashflow.constant.Role;
//import jakarta.persistence.*;
//import lombok.*;
//
//@Entity
//@Table(name = "users")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//public class User {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "phone_number")
//    private String phoneNumber;
//
//    private String password;
//
//    @Enumerated(EnumType.STRING)
//    private Role role;
//}