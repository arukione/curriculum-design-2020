package com.arukione.curriculum_design.controller;

import com.arukione.curriculum_design.model.DTO.Request.TopicInfo;
import com.arukione.curriculum_design.model.DTO.Response.Response;
import com.arukione.curriculum_design.service.TeacherService;
import com.arukione.curriculum_design.utils.HTTPStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class TeacherController {

    final TeacherService teacherService;

    @Autowired
    TeacherController(TeacherService teacherService){
        this.teacherService = teacherService;
    }

    @PostMapping("/teacher/changeInfo")
    public Response changeInfo(@RequestParam("accessToken")String accessToken,
                               @RequestParam("phone")String phone,
                               @RequestParam("email")String email,
                               @RequestParam("topicDemand") String topicDemand){
        return teacherService.changeInfo(accessToken, phone, email, topicDemand);
    }

    @PostMapping("addTopic")
    public Response addTopic(@RequestParam("accessToken")String accessToken, @RequestBody TopicInfo topicInfo){
        return teacherService.addTopic(accessToken, topicInfo);
    }
}
