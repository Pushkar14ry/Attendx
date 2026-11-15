package com.attendx.controller;

import com.attendx.dto.StudentRequest;
import com.attendx.dto.StudentResponse;
import com.attendx.exception.DuplicateStudentException;
import com.attendx.exception.StudentNotFoundException;
import com.attendx.service.StudentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockBean StudentService service;

    private StudentResponse sampleResponse() {
        return new StudentResponse(1, "Nihan", LocalDateTime.now());
    }

    @Test
    void addStudent_returns201() throws Exception {
        when(service.addStudent(any())).thenReturn(sampleResponse());
        mvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new StudentRequest(1, "Nihan"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rollNo").value(1))
                .andExpect(jsonPath("$.name").value("Nihan"));
    }

    @Test
    void addStudent_returns409OnDuplicate() throws Exception {
        when(service.addStudent(any())).thenThrow(new DuplicateStudentException(1));
        mvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new StudentRequest(1, "Nihan"))))
                .andExpect(status().isConflict());
    }

    @Test
    void addStudent_returns400OnInvalidInput() throws Exception {
        mvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rollNo\": -1, \"name\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listStudents_returns200() throws Exception {
        when(service.listStudents()).thenReturn(List.of(sampleResponse()));
        mvc.perform(get("/api/v1/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rollNo").value(1));
    }

    @Test
    void getStudent_returns404WhenNotFound() throws Exception {
        when(service.getStudent(99)).thenThrow(new StudentNotFoundException(99));
        mvc.perform(get("/api/v1/students/99"))
                .andExpect(status().isNotFound());
    }
}
