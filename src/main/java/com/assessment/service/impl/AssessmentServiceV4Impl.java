package com.assessment.service.impl;

import com.assessment.cassandra.utils.CassandraOperation;
import com.assessment.datasecurity.DecryptionService;
import com.assessment.datasecurity.EncryptionService;
import com.assessment.kafka.Producer;
import com.assessment.kafka.service.KafkaCertificateProducerService;
import com.assessment.model.SBApiResponse;
import com.assessment.repo.AssessmentRepository;
import com.assessment.service.AssessmentServiceV4;
import com.assessment.service.AssessmentUtilServiceV2;
import com.assessment.util.Constants;
import com.assessment.util.ProjectUtil;
import com.assessment.util.ServerProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.mortbay.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.assessment.util.ProjectUtil.updateErrorDetails;
import static java.util.stream.Collectors.toList;

@Service
public class AssessmentServiceV4Impl implements AssessmentServiceV4 {


    private final Logger logger = LoggerFactory.getLogger(AssessmentServiceV4Impl.class);

    @Autowired
    private AssessmentUtilServiceV2 assessmentUtilService;

    @Autowired
    private ServerProperties serverProperties;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    CassandraOperation cassandraOperation;

    @Autowired
    ResourceLoader resourceLoader;

    @Autowired
    DecryptionService decryptionService;

    @Autowired
    KafkaCertificateProducerService kafkaCertificateProducerService;

    @Autowired
    Producer producer;

    public SBApiResponse submitAssessmentAsync(Map<String, Object> submitRequest, boolean editMode) {
        logger.info("AssessmentServiceV4Impl::submitAssessmentAsync.. started");
        SBApiResponse outgoingResponse = ProjectUtil.createDefaultResponse(Constants.API_SUBMIT_ASSESSMENT);
        try {

            String email = (String) submitRequest.get(Constants.EMAIL);
            if (!ProjectUtil.validateEmailPattern(email)) {
                updateErrorDetails(outgoingResponse, Constants.INVALID_EMAIL, HttpStatus.BAD_REQUEST);
                return outgoingResponse;
            }
            String contextId = (String) submitRequest.get(Constants.CONTEXT_ID);
            email = encryptionService.encryptData(email);
            String assessmentIdFromRequest = (String) submitRequest.get(Constants.IDENTIFIER);
            String errMsg;
            List<Map<String, Object>> sectionListFromSubmitRequest = new ArrayList<>();
            List<Map<String, Object>> hierarchySectionList = new ArrayList<>();
            Map<String, Object> assessmentHierarchy = new HashMap<>();
            Map<String, Object> existingAssessmentData = new HashMap<>();

            errMsg = validateSubmitAssessmentRequest(submitRequest, email, contextId, hierarchySectionList,
                    sectionListFromSubmitRequest, assessmentHierarchy, existingAssessmentData, editMode);

            if (StringUtils.isNotBlank(errMsg)) {
                updateErrorDetails(outgoingResponse, errMsg, HttpStatus.BAD_REQUEST);
                return outgoingResponse;
            }
            String assessmentPrimaryCategory = (String) assessmentHierarchy.get(Constants.PRIMARY_CATEGORY);

            String scoreCutOffType = ((String) assessmentHierarchy.get(Constants.SCORE_CUTOFF_TYPE)).toLowerCase();
            List<Map<String, Object>> sectionLevelsResults = new ArrayList<>();
            for (Map<String, Object> hierarchySection : hierarchySectionList) {
                List<Map<String, Object>> questionsListFromSubmitRequest = ProjectUtil.findUserSectionQuestions(
                        hierarchySection, sectionListFromSubmitRequest, scoreCutOffType);
                List<String> desiredKeys = Lists.newArrayList(Constants.IDENTIFIER);
                List<Object> questionsList = questionsListFromSubmitRequest.stream()
                        .flatMap(x -> desiredKeys.stream().filter(x::containsKey).map(x::get)).collect(toList());
                List<String> questionsListFromAssessmentHierarchy = questionsList.stream()
                        .map(object -> Objects.toString(object, null)).collect(Collectors.toList());
                Map<String, Object> result = new HashMap<>();
                switch (scoreCutOffType) {
                    case Constants.ASSESSMENT_LEVEL_SCORE_CUTOFF: {
                        result.putAll(createResponseMapWithProperStructure(hierarchySection,
                                assessmentUtilService.validateQumlAssessment(questionsListFromAssessmentHierarchy,
                                        questionsListFromSubmitRequest, assessmentUtilService.readQListfromCache(questionsListFromAssessmentHierarchy, assessmentIdFromRequest, editMode))));
                        Map<String, Object> finalRes = ProjectUtil.calculateAssessmentFinalResults(result, false, logger);
                        outgoingResponse.getResult().putAll(finalRes);
                        outgoingResponse.getResult().put(Constants.PRIMARY_CATEGORY, assessmentPrimaryCategory);
                        if (!Constants.PRACTICE_QUESTION_SET.equalsIgnoreCase(assessmentPrimaryCategory) && !editMode) {

                            Map<String, Object> questionSetFromAssessment = ProjectUtil
                                    .extractQuestionSetFromAssessment(existingAssessmentData, mapper);
                            writeDataToDatabaseAndTriggerKafkaEvent(submitRequest, email, questionSetFromAssessment, finalRes,
                                    (String) assessmentHierarchy.get(Constants.PRIMARY_CATEGORY),contextId);
                        }

                        return outgoingResponse;
                    }
                    case Constants.SECTION_LEVEL_SCORE_CUTOFF: {
                        result.putAll(createResponseMapWithProperStructure(hierarchySection,
                                assessmentUtilService.validateQumlAssessment(questionsListFromAssessmentHierarchy,
                                        questionsListFromSubmitRequest, assessmentUtilService.readQListfromCache(questionsListFromAssessmentHierarchy, assessmentIdFromRequest, editMode))));
                        sectionLevelsResults.add(result);
                    }
                    break;
                    default:
                        break;
                }
            }
            if (Constants.SECTION_LEVEL_SCORE_CUTOFF.equalsIgnoreCase(scoreCutOffType)) {
                Map<String, Object> result = calculateSectionFinalResults(sectionLevelsResults);
                outgoingResponse.getResult().putAll(result);
                outgoingResponse.getParams().setStatus(Constants.SUCCESS);
                outgoingResponse.setResponseCode(HttpStatus.OK);
                outgoingResponse.getResult().put(Constants.PRIMARY_CATEGORY, assessmentPrimaryCategory);
                if (!Constants.PRACTICE_QUESTION_SET.equalsIgnoreCase(assessmentPrimaryCategory) && !editMode) {
                    Map<String, Object> questionSetFromAssessment = ProjectUtil
                            .extractQuestionSetFromAssessment(existingAssessmentData, mapper);
                    writeDataToDatabaseAndTriggerKafkaEvent(submitRequest, email, questionSetFromAssessment, result,
                            (String) assessmentHierarchy.get(Constants.PRIMARY_CATEGORY),contextId);
                }
                return outgoingResponse;
            }

        } catch (Exception e) {
            String errMsg = String.format("Failed to process assessment submit request. Exception: ", e.getMessage());
            logger.error(errMsg, e);
            updateErrorDetails(outgoingResponse, errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return outgoingResponse;
    }

    private String validateSubmitAssessmentRequest(Map<String, Object> submitRequest, String email,String contextId,
                                                   List<Map<String, Object>> hierarchySectionList, List<Map<String, Object>> sectionListFromSubmitRequest,
                                                   Map<String, Object> assessmentHierarchy, Map<String, Object> existingAssessmentData, boolean editMode) throws Exception {

        if (StringUtils.isEmpty((String) submitRequest.get(Constants.IDENTIFIER))) {
            return Constants.INVALID_ASSESSMENT_ID;
        }
        String assessmentIdFromRequest = (String) submitRequest.get(Constants.IDENTIFIER);
        assessmentHierarchy.putAll(assessmentUtilService.readAssessmentHierarchyFromCache(assessmentIdFromRequest, editMode));
        if (MapUtils.isEmpty(assessmentHierarchy)) {
            return Constants.READ_ASSESSMENT_FAILED;
        }

        hierarchySectionList.addAll((List<Map<String, Object>>) assessmentHierarchy.get(Constants.CHILDREN));
        sectionListFromSubmitRequest.addAll((List<Map<String, Object>>) submitRequest.get(Constants.CHILDREN));
        if (((String) (assessmentHierarchy.get(Constants.PRIMARY_CATEGORY)))
                .equalsIgnoreCase(Constants.PRACTICE_QUESTION_SET) || editMode)
            return "";

        List<Map<String, Object>> existingDataList = assessmentUtilService.readUserSubmittedAssessmentRecords(
                email, (String) submitRequest.get(Constants.IDENTIFIER), contextId);
        return ProjectUtil.validateExistingAssessmentSubmission(existingDataList, existingAssessmentData,
                assessmentHierarchy, hierarchySectionList, sectionListFromSubmitRequest, serverProperties, mapper);
    }

    public Map<String, Object> createResponseMapWithProperStructure(Map<String, Object> hierarchySection,
                                                                    Map<String, Object> resultMap) {
        Map<String, Object> sectionLevelResult = new HashMap<>();
        sectionLevelResult.put(Constants.IDENTIFIER, hierarchySection.get(Constants.IDENTIFIER));
        sectionLevelResult.put(Constants.OBJECT_TYPE, hierarchySection.get(Constants.OBJECT_TYPE));
        sectionLevelResult.put(Constants.PRIMARY_CATEGORY, hierarchySection.get(Constants.PRIMARY_CATEGORY));
        sectionLevelResult.put(Constants.PASS_PERCENTAGE, hierarchySection.get(Constants.MINIMUM_PASS_PERCENTAGE));
        Double result;
        if (!ObjectUtils.isEmpty(resultMap)) {
            result = (Double) resultMap.get(Constants.RESULT);
            sectionLevelResult.put(Constants.RESULT, result);
            sectionLevelResult.put(Constants.TOTAL, resultMap.get(Constants.TOTAL));
            sectionLevelResult.put(Constants.BLANK, resultMap.get(Constants.BLANK));
            sectionLevelResult.put(Constants.CORRECT, resultMap.get(Constants.CORRECT));
            sectionLevelResult.put(Constants.INCORRECT, resultMap.get(Constants.INCORRECT));
            sectionLevelResult.put(Constants.CHILDREN, resultMap.get(Constants.CHILDREN));
        } else {
            result = 0.0;
            sectionLevelResult.put(Constants.RESULT, result);
            List<String> childNodes = (List<String>) hierarchySection.get(Constants.CHILDREN);
            sectionLevelResult.put(Constants.TOTAL, childNodes.size());
            sectionLevelResult.put(Constants.BLANK, childNodes.size());
            sectionLevelResult.put(Constants.CORRECT, 0);
            sectionLevelResult.put(Constants.INCORRECT, 0);
        }
        sectionLevelResult.put(Constants.PASS,
                result >= ((Integer) hierarchySection.get(Constants.MINIMUM_PASS_PERCENTAGE)));
        sectionLevelResult.put(Constants.OVERALL_RESULT, result);
        return sectionLevelResult;
    }

    private void writeDataToDatabaseAndTriggerKafkaEvent(Map<String, Object> submitRequest, String email,
                                                         Map<String, Object> questionSetFromAssessment, Map<String, Object> result, String primaryCategory,String contextId) {
        try {
            if (questionSetFromAssessment.get(Constants.START_TIME) != null) {
                Long existingAssessmentStartTime = (Long) questionSetFromAssessment.get(Constants.START_TIME);
                Timestamp startTime = new Timestamp(existingAssessmentStartTime);
                Boolean isAssessmentUpdatedToDB = assessmentRepository.updateUserAssesmentDataToDB(email,
                        (String) submitRequest.get(Constants.IDENTIFIER), submitRequest, result, Constants.SUBMITTED,
                        startTime, null,contextId);

                List<String> notAllowedForKafkaEvent = serverProperties.getAssessmentPrimaryKeyNotAllowedCertificate();
                if (Boolean.TRUE.equals(isAssessmentUpdatedToDB) && Boolean.TRUE.equals(result.get(Constants.PASS)) && notAllowedForKafkaEvent.stream()
                        .noneMatch(key -> key.equals(primaryCategory))) {

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    String completionDate = dateFormat.format(new Date());

                    List<Map<String, Object>> submitedAssessmentDetails = assessmentRepository.fetchUserAssessmentDataFromDB(email, (String) submitRequest.get(Constants.IDENTIFIER));
                    String recipientName = (String) submitedAssessmentDetails.get(0).get(Constants.NAME);

                    Map<String, String> courseMetadata = ProjectUtil.fetchCourseMetadata(
                            submitRequest.get(Constants.COURSE_ID), cassandraOperation, serverProperties, mapper);
                    String courseProvider = courseMetadata.get(Constants.SOURCE);
                    String courseName = courseMetadata.get(Constants.NAME);
                    String coursePosterImage = courseMetadata.get(Constants.POSTER_IMAGE);


                    Resource resource = resourceLoader.getResource("classpath:certificate-kafka-json.json");
                    InputStream inputStream = resource.getInputStream();
                    JsonNode jsonNode = mapper.readTree(inputStream);

                    Map<String, Object> certificateRequest = new HashMap<>();
                    certificateRequest.put(Constants.USER_ID, decryptionService.decryptData(email));
                    certificateRequest.put(Constants.ASSESSMENT_ID_KEY, submitRequest.get(Constants.IDENTIFIER));
                    certificateRequest.put(Constants.COURSE_ID, submitRequest.get(Constants.COURSE_ID));
                    certificateRequest.put(Constants.COMPLETION_DATE, completionDate);
                    certificateRequest.put(Constants.PROVIDER_NAME, courseProvider);
                    certificateRequest.put(Constants.COURSE_NAME, courseName);
                    certificateRequest.put(Constants.COURSE_POSTER_IMAGE, coursePosterImage);
                    certificateRequest.put(Constants.RECIPIENT_NAME, recipientName);
                    kafkaCertificateProducerService.replacePlaceholders(jsonNode, certificateRequest);
                    producer.push(serverProperties.getKafkaTopicsPublicAssessmentCertificate(), jsonNode);

                }
            }
        } catch (Exception e) {
            logger.error("Failed to write data for assessment submit response. Exception: ", e);
        }
    }

    private Map<String, Object> calculateSectionFinalResults(List<Map<String, Object>> sectionLevelResults) {
        Map<String, Object> res = new HashMap<>();
        Double result;
        Integer correct = 0;
        Integer blank = 0;
        Integer inCorrect = 0;
        Integer total = 0;
        int pass = 0;
        Double totalResult = 0.0;
        try {
            for (Map<String, Object> sectionChildren : sectionLevelResults) {
                res.put(Constants.CHILDREN, sectionLevelResults);
                result = (Double) sectionChildren.get(Constants.RESULT);
                totalResult += result;
                total += (Integer) sectionChildren.get(Constants.TOTAL);
                blank += (Integer) sectionChildren.get(Constants.BLANK);
                correct += (Integer) sectionChildren.get(Constants.CORRECT);
                inCorrect += (Integer) sectionChildren.get(Constants.INCORRECT);
                Integer minimumPassPercentage = (Integer) sectionChildren.get(Constants.PASS_PERCENTAGE);
                if (result >= minimumPassPercentage) {
                    pass++;
                }
            }
            res.put(Constants.OVERALL_RESULT, totalResult / sectionLevelResults.size());
            res.put(Constants.TOTAL, total);
            res.put(Constants.BLANK, blank);
            res.put(Constants.CORRECT, correct);
            res.put(Constants.INCORRECT, inCorrect);
            res.put(Constants.PASS, (pass == sectionLevelResults.size()));
        } catch (Exception e) {
            logger.error("Failed to calculate assessment score. Exception: ", e);
        }
        return res;
    }

}
