//package com.uet.agent_simulation_worker.controllers;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.uet.agent_simulation_worker.models.AppUser;
//import com.uet.agent_simulation_worker.repositories.ProjectRepository;
//import com.uet.agent_simulation_worker.repositories.UserRepository;
//import com.uet.agent_simulation_worker.responses.ResponseHandler;
//import com.uet.agent_simulation_worker.responses.SuccessResponse;
//import com.uet.agent_simulation_worker.services.impl.S3Service;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.env.Environment;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/v1")
//@RequiredArgsConstructor
//@Slf4j
//public class Controller {
//    private final ResponseHandler ResponseHandler;
//    private final Environment env;
//    private final S3Service s3Service;
//    private final UserRepository userRepository;
//    private final ProjectRepository projectRepository;
//
//    @Value("${server.port}")
//    private int port;
//
//    @Value("${spring.profiles.active}")
//    private String profile;
//
//    @GetMapping
//    public ResponseEntity<SuccessResponse> health() throws JsonProcessingException {
////        s3Service.uploadDirectory("./storage/outputs/1_simulator-01/snapshot", "snapshot");
////        s3Service.uploadFile("snapshot_download.png", "./storage/outputs/download.png");
////        s3Service.clearDirectory("snapshot");
////        final AppUser user = AppUser.builder()
////                .email("abcd@gmail.com")
////                .fullname("Nguyen Van A")
////                .password("123456")
////                .role(1)
////                .build();
//
//        final AppUser user2 = AppUser.builder()
//                .email("aasaaa@gmail.com")
//                .fullname("Nguyen Van B")
//                .password("123456")
//                .role(1)
//                .build();
////
//        userRepository.saveAll(List.of(user2));
//
//
////        userRepository.save(user);
////        user.setId(BigInteger.valueOf(1));
////        userRepository.save(user);
////        final Project project = projectRepository.findById(BigInteger.valueOf(1)).orElse(null);
////        AppUser user = userRepository.findById(project.getUserId()).orElse(null);
////        return ResponseHandler.respondSuccess(project);
////        userRepository.delete(user);
////        projectRepository.delete(project);
//
////        final Project project = Project.builder()
////                .name("Project 1")
////                .location("/tmp")
////                .user(user)
////                .build();
////        projectRepository.save(project);
//
//
////        final AppUser user = userRepository.findById(BigInteger.valueOf(1)).orElse(null);
//////        return ResponseHandler.respondSuccess(user);
////        user.setCreatedBy("adminn");
////        userRepository.save(user);
//
////        return ResponseHandler.respondSuccess(user);
//
//        return ResponseHandler.respondSuccess(
//                "Server is running on port " + port + " with profiles: " + profile + ", thread: " + Thread.currentThread()
//        );
//    }
//
//    @GetMapping("/test")
//    public ResponseEntity<SuccessResponse> test() {
////        final var message = Message.builder().nodeId(BigInteger.ONE).type("test").build();
////
////        final var simulation = RunSimulation.builder().experiments()
//
//        return ResponseHandler.respondSuccess("Test");
//    }
//}
