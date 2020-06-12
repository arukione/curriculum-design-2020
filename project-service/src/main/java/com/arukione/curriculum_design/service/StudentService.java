package com.arukione.curriculum_design.service;

import com.arukione.curriculum_design.exception.PermissionException;
import com.arukione.curriculum_design.mapper.ApplicationMapper;
import com.arukione.curriculum_design.mapper.TeacherMapper;
import com.arukione.curriculum_design.mapper.TopicInfoMapper;
import com.arukione.curriculum_design.mapper.TopicTypeMapper;
import com.arukione.curriculum_design.model.DTO.Request.TopicInfo;
import com.arukione.curriculum_design.model.DTO.Response.Response;
import com.arukione.curriculum_design.model.DTO.Response.SelectableTeacherResponse;
import com.arukione.curriculum_design.model.DTO.Response.SelectableTopicResponse;
import com.arukione.curriculum_design.model.DTO.Response.UserInfoResponse;
import com.arukione.curriculum_design.model.VO.SelectableTopicInfo;
import com.arukione.curriculum_design.model.entity.*;
import com.arukione.curriculum_design.utils.Generator;
import com.arukione.curriculum_design.utils.HTTPStatus;
import com.arukione.curriculum_design.utils.Message;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.sf.jsqlparser.statement.select.Top;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentService {

    final UserService userService;
    final TopicInfoMapper topicInfoMapper;
    final TeacherMapper teacherMapper;
    final ApplicationMapper applicationMapper;
    final TopicTypeMapper topicTypeMapper;

    @Autowired
    public StudentService(UserService userService, TopicInfoMapper topicInfoMapper, TeacherMapper teacherMapper, ApplicationMapper applicationMapper, TopicTypeMapper topicTypeMapper) {
        this.userService = userService;
        this.topicInfoMapper = topicInfoMapper;
        this.teacherMapper = teacherMapper;
        this.applicationMapper = applicationMapper;
        this.topicTypeMapper = topicTypeMapper;
    }

    Response studentPermission(String accessToken) {
        try {
            userService.permission(accessToken, "Student");
            return null;
        } catch (PermissionException permissionException) {
            return new Response(HTTPStatus.NotAllowed, Message.USER_PERMISSION_ERROR);
        } catch (NullPointerException npe) {
            return new Response(HTTPStatus.Unauthorized, Message.NO_LOGIN_STATUS);
        }
    }

    public SelectableTopicResponse getAllTopic(String accessToken) {
        try {
            Student student = (Student) userService.permission(accessToken, "Student");
            String profId = student.getProfessionId();
            ArrayList<SelectableTopicInfo> selectableTopicInfos = topicInfoMapper.getSelectableTopicInfo(profId);
            return new SelectableTopicResponse(HTTPStatus.OK, selectableTopicInfos);
        } catch (PermissionException permissionException) {
            return new SelectableTopicResponse(HTTPStatus.NotAllowed, Message.USER_PERMISSION_ERROR);
        } catch (NullPointerException npe) {
            return new SelectableTopicResponse(HTTPStatus.Unauthorized, Message.NO_LOGIN_STATUS);
        }
    }

    public SelectableTeacherResponse getAllTeacher(String accessToken) {
        try {
            Student student = (Student) userService.permission(accessToken, "Student");
            String profId = student.getProfessionId();
            QueryWrapper<Teacher> teacherQueryWrapper = new QueryWrapper<>();
            teacherQueryWrapper.eq("GuideProfID", profId);
            List<Teacher> teachers = teacherMapper.selectList(teacherQueryWrapper);
            return new SelectableTeacherResponse(HTTPStatus.OK, teachers);
        } catch (PermissionException permissionException) {
            return new SelectableTeacherResponse(HTTPStatus.NotAllowed, Message.USER_PERMISSION_ERROR);
        } catch (NullPointerException npe) {
            return new SelectableTeacherResponse(HTTPStatus.Unauthorized, Message.NO_LOGIN_STATUS);
        }
    }

    public Response addApply(String accessToken, String topicId) {
        try {
            Student student = (Student) userService.permission(accessToken, "Student");
            return getApplyResult(student, topicId);
        } catch (PermissionException permissionException) {
            return new Response(HTTPStatus.NotAllowed, Message.USER_PERMISSION_ERROR);
        } catch (NullPointerException npe) {
            return new Response(HTTPStatus.Unauthorized, Message.NO_LOGIN_STATUS);
        } catch (Exception e) {
            if (e.getMessage().contains("for key 'PRIMARY'")) return new Response(HTTPStatus.Failed, "无法再次申请此课题");
            return new Response(HTTPStatus.Failed, e.getMessage());
        }
    }

    public Response addApply(String accessToken, String tid, TopicInfo topicInfo) {
        try {
            Student student = (Student) userService.permission(accessToken, "Student");
            String topicId = Generator.generateTopicID();
            Topic topic = Topic.builder()
                    .topicId(topicId)
                    .topicName(topicInfo.getTopicName())
                    .introduction(topicInfo.getIntroduction())
                    .tid(tid)
                    .typeId(topicInfo.getTypeId())
                    .source("1")
                    .build();
            if (topicInfoMapper.insert(topic) != 1) return new Response(HTTPStatus.Failed, Message.DB_NO_DATA);
            return getApplyResult(student, topicId);
        } catch (PermissionException permissionException) {
            return new Response(HTTPStatus.NotAllowed, Message.USER_PERMISSION_ERROR);
        } catch (NullPointerException npe) {
            return new Response(HTTPStatus.Unauthorized, Message.NO_LOGIN_STATUS);
        }
    }

    private Response getApplyResult(Student student, String topicId) {
        String sid = student.getSid();
        Application application = new Application();
        application.setSid(sid);
        application.setTopicId(topicId);
        application.setApplyTime(Generator.getNowTime());
        application.setStatus("0");
        return userService.opsResult(applicationMapper.insert(application));
    }


    //获取我的导师信息
    public UserInfoResponse getMyTeacher(String accessToken) {
        try {
            Student stu = (Student) userService.permission(accessToken, "Student");
            //获取已通过的申请记录
            QueryWrapper<Topic> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("SID", stu.getSid());
            List<Topic> topics = topicInfoMapper.selectList(queryWrapper);
            if(topics == null || topics.size()==0) return new UserInfoResponse(HTTPStatus.Failed, Message.DB_NO_DATA);
            return new UserInfoResponse(200, teacherMapper.selectById(topics.get(0).getTid()));
        } catch (PermissionException permissionException) {
            return new UserInfoResponse(HTTPStatus.NotAllowed, Message.USER_PERMISSION_ERROR);
        } catch (NullPointerException npe) {
            return new UserInfoResponse(HTTPStatus.Unauthorized, Message.NO_LOGIN_STATUS);
        }

    }

    //获得已通过的课题信息
    public Map<String, Object> getAllowTopic(String accessToken) {
        Map<String, Object> response = new HashMap<>();
        try {
            Student stu = (Student) userService.permission(accessToken, "Student");
            QueryWrapper<Topic> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("SID", stu.getSid());
            List<Topic> topics = topicInfoMapper.selectList(queryWrapper);
            if(topics==null || topics.size()==0){
                response.put("status", HTTPStatus.Failed);
                response.put("message","没有获取到数据");
                return response;
            }
            Topic topic = topics.get(0);
            TopicType type = topicTypeMapper.getTopicType(topic.getTypeId());
            response.put("status", HTTPStatus.OK);
            response.put("topic",topic);
            response.put("type",type);
            return response;
        } catch (PermissionException permissionException) {
            response.put("status", HTTPStatus.NotAllowed);
            response.put("message",Message.USER_PERMISSION_ERROR);
            return response;
        } catch (NullPointerException npe) {
            response.put("status", HTTPStatus.Unauthorized);
            response.put("message",Message.NO_LOGIN_STATUS);
            return response;
        }
    }


    public Map<String, Object> getApplicationInfo(String accessToken) {
        Map<String, Object> response = new HashMap<>();
        try {
            Student stu = (Student) userService.permission(accessToken, "Student");
            ArrayList<Application> applies = applicationMapper.getApplicationsOfSID(stu.getSid());
            if (applies == null || applies.size()==0) {
                response.put("status", HTTPStatus.Failed);
                response.put("message","没有获取到数据");
                return response;
            }
            ArrayList<Object> mapData =new ArrayList<>();
            for (Application apply : applies) {
                Map<String, Object> data = new HashMap<>();
                Topic topic =  topicInfoMapper.selectById(apply.getTopicId());
                data.put("topicInfo", topic);
                data.put("teacherName", teacherMapper.selectById(topic.getTid()).getName());
                data.put("applyInfo", apply);
                mapData.add(data);
            }
            response.put("status", HTTPStatus.OK);
            response.put("applyRecord", mapData);
            return response;
        } catch (PermissionException permissionException) {
            response.put("status", HTTPStatus.NotAllowed);
            response.put("message",Message.USER_PERMISSION_ERROR);
            return response;
        } catch (NullPointerException npe) {
            response.put("status", HTTPStatus.Unauthorized);
            response.put("message",Message.NO_LOGIN_STATUS);
            return response;
        }
    }
}
