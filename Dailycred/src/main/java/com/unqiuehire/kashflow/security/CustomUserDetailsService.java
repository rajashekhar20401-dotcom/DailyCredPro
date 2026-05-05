//package com.unqiuehire.kashflow.security;
//
//import com.unqiuehire.kashflow.entity.User;
//import com.unqiuehire.kashflow.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.userdetails.*;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class CustomUserDetailsService implements UserDetailsService {
//
//    private final UserRepository userRepository;
//
//    @Override
//    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
//        User user = userRepository.findByPhoneNumber(phone)
//                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
//
//        return org.springframework.security.core.userdetails.User
//                .withUsername(user.getPhoneNumber())
//                .password(user.getPassword())
//                .roles(user.getRole().name())
//                .build();
//    }
//}