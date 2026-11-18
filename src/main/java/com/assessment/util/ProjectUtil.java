package com.assessment.util;

import com.assessment.cassandra.utils.CassandraOperation;
import com.assessment.model.SBApiResponse;
import com.assessment.model.SunbirdApiRespParam;
import com.assessment.util.exceptions.ProjectCommonException;
import com.assessment.util.exceptions.ResponseCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Key;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * This class will contains all the common utility methods.
 *
 * @author Manzarul
 */
public class ProjectUtil {

    public static AssessmentServiceLogger logger = new AssessmentServiceLogger(ProjectUtil.class.getName());

    /**
     * This method will check incoming value is null or empty it will do empty check
     * by doing trim method. in case of null or empty it will return true else
     * false.
     *
     * @param value
     * @return
     */
    public static boolean isStringNullOREmpty(String value) {
        return (value == null || "".equals(value.trim()));
    }

    /**
     * This method will create and return server exception to caller.
     *
     * @param responseCode ResponseCode
     * @return ProjectCommonException
     */
    public static ProjectCommonException createServerError(ResponseCode responseCode) {
        return new ProjectCommonException(responseCode.getErrorCode(), responseCode.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode());
    }

    public static ProjectCommonException createClientException(ResponseCode responseCode) {
        return new ProjectCommonException(responseCode.getErrorCode(), responseCode.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    public static SBApiResponse createDefaultResponse(String api) {
        SBApiResponse response = new SBApiResponse();
        response.setId(api);
        response.setVer(Constants.API_VERSION_1);
        response.setParams(new SunbirdApiRespParam(UUID.randomUUID().toString()));
        response.getParams().setStatus(Constants.SUCCESS);
        response.setResponseCode(HttpStatus.OK);
        response.setTs(DateTime.now().toString());
        return response;
    }

    public static Map<String, String> getDefaultHeaders() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
        return headers;
    }

    public enum Method {
        GET, POST, PUT, DELETE, PATCH
    }

    public static String convertSecondsToHrsAndMinutes(int seconds) {
        String time = "";
        if (seconds > 60) {
            int min = (seconds / 60) % 60;
            int hours = (seconds / 60) / 60;
            String minutes = (min < 10) ? "0" + min : Integer.toString(min);
            String strHours = (hours < 10) ? "0" + hours : Integer.toString(hours);
            if (min > 0 && hours > 0)
                time = strHours + "h " + minutes + "m";
            else if (min == 0 && hours > 0)
                time = strHours + "h";
            else if (min > 0) {
                time = minutes + "m";
            }
        }
        return time;
    }

    public static String firstLetterCapitalWithSingleSpace(final String words) {
        return Stream.of(words.trim().split("\\s")).filter(word -> word.length() > 0)
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1)).collect(Collectors.joining(" "));
    }

    /**
     * Check the email id is valid or not
     *
     * @param email String
     * @return Boolean
     */

    public static Boolean validateEmailPattern(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\." + "[a-zA-Z0-9_+&*-]+)*@" + "(?:[a-zA-Z0-9-]+\\.)+[a-z"
                + "A-Z]{2,7}$";
        Pattern pat = Pattern.compile(emailRegex);
        if (pat.matcher(email).matches()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    public static Boolean validateFullName(String firstName) {
        return firstName.matches("^(?!.*\\n)[a-zA-Z]+(?:['\\s][a-zA-Z]+)*(?<!\\.|\\s)$");
    }


    public static void updateErrorDetails(SBApiResponse response, String errMsg, HttpStatus responseCode) {
        response.getParams().setStatus(Constants.FAILED);
        response.getParams().setErrmsg(errMsg);
        response.setResponseCode(responseCode);

    }

    /**
     * Shared by AssessmentServiceImpl/AssessmentServiceV4Impl/AssessmentServiceV5Impl, which were
     * each carrying an identical private copy of this calculation.
     */
    public static Timestamp calculateAssessmentSubmitTime(int expectedDuration, Timestamp assessmentStartTime,
                                                            int bufferTime, ServerProperties serverProperties) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(assessmentStartTime.getTime());
        if (bufferTime > 0) {
            cal.add(Calendar.SECOND,
                    expectedDuration + Integer.parseInt(serverProperties.getUserAssessmentSubmissionDuration()));
        } else {
            cal.add(Calendar.SECOND, expectedDuration);
        }
        return new Timestamp(cal.getTime().getTime());
    }

    /**
     * Shared by AssessmentServiceV4Impl/AssessmentServiceV5Impl, which were each carrying an
     * identical private copy of this validation.
     */
    public static String validateIfQuestionIdsAreSame(Map<String, Object> submitRequest,
            List<Map<String, Object>> sectionListFromSubmitRequest, List<String> desiredKeys,
            Map<String, Object> existingAssessmentData, ObjectMapper mapper) throws Exception {
        String questionSetFromAssessmentString = (String) existingAssessmentData
                .get(Constants.ASSESSMENT_READ_RESPONSE_KEY);
        if (StringUtils.isNotBlank(questionSetFromAssessmentString)) {
            Map<String, Object> questionSetFromAssessment = mapper.readValue(questionSetFromAssessmentString,
                    new TypeReference<Map<String, Object>>() {
                    });
            if (questionSetFromAssessment != null && questionSetFromAssessment.get(Constants.CHILDREN) != null) {
                List<Map<String, Object>> sections = (List<Map<String, Object>>) questionSetFromAssessment
                        .get(Constants.CHILDREN);
                List<String> desiredKey = Lists.newArrayList(Constants.CHILD_NODES);
                List<Object> questionList = sections.stream()
                        .flatMap(x -> desiredKey.stream().filter(x::containsKey).map(x::get)).collect(toList());
                List<Object> questionIdsFromAssessmentHierarchy = new ArrayList<>();
                List<Map<String, Object>> questionsListFromSubmitRequest = new ArrayList<>();
                for (Object question : questionList) {
                    questionIdsFromAssessmentHierarchy.addAll((List<String>) question);
                }
                for (Map<String, Object> userSectionData : sectionListFromSubmitRequest) {
                    if (userSectionData.containsKey(Constants.CHILDREN)
                            && !ObjectUtils.isEmpty(userSectionData.get(Constants.CHILDREN))) {
                        questionsListFromSubmitRequest
                                .addAll((List<Map<String, Object>>) userSectionData.get(Constants.CHILDREN));
                    }
                }
                List<Object> userQuestionIdsFromSubmitRequest = questionsListFromSubmitRequest.stream()
                        .flatMap(x -> desiredKeys.stream().filter(x::containsKey).map(x::get))
                        .collect(Collectors.toList());
                if (!new HashSet<>(questionIdsFromAssessmentHierarchy).containsAll(userQuestionIdsFromSubmitRequest)) {
                    return Constants.ASSESSMENT_SUBMIT_INVALID_QUESTION;
                }
            }
        } else {
            return Constants.ASSESSMENT_SUBMIT_QUESTION_READ_FAILED;
        }
        return "";
    }

    /**
     * Shared by AssessmentServiceV4Impl and AssessmentServiceV5Impl (which also used this twice
     * internally), which were each carrying an identical copy of this content-hierarchy lookup.
     */
    public static Map<String, String> fetchCourseMetadata(Object identifier, CassandraOperation cassandraOperation,
            ServerProperties serverProperties, ObjectMapper mapper) throws java.io.IOException {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.IDENTIFIER, identifier);
        List<Map<String, Object>> contentHierarchyDetails = cassandraOperation.getRecordsByProperties(
                serverProperties.getContentHierarchyNamespace(), serverProperties.getContentHierarchyTable(),
                propertyMap, null);
        String contentHierarchyStr = (String) contentHierarchyDetails.get(0).get(Constants.HIERARCHY);
        Map<String, Object> contentHierarchyObj = mapper.readValue(contentHierarchyStr, HashMap.class);
        Map<String, String> courseMetadata = new HashMap<>();
        courseMetadata.put(Constants.SOURCE, (String) contentHierarchyObj.get(Constants.SOURCE));
        courseMetadata.put(Constants.NAME, (String) contentHierarchyObj.get(Constants.NAME));
        courseMetadata.put(Constants.POSTER_IMAGE, (String) contentHierarchyObj.get(Constants.POSTER_IMAGE));
        return courseMetadata;
    }

    /**
     * Shared by AssessmentServiceImpl and AssessmentServiceV5Impl, which each carried an identical
     * copy of this request-body parsing logic.
     */
    public static List<String> getQuestionIdList(Map<String, Object> questionListRequest, Logger logger) {
        try {
            if (questionListRequest.containsKey(Constants.REQUEST)) {
                Map<String, Object> request = (Map<String, Object>) questionListRequest.get(Constants.REQUEST);
                if ((!ObjectUtils.isEmpty(request)) && request.containsKey(Constants.SEARCH)) {
                    Map<String, Object> searchObj = (Map<String, Object>) request.get(Constants.SEARCH);
                    if (!ObjectUtils.isEmpty(searchObj) && searchObj.containsKey(Constants.IDENTIFIER)
                            && !CollectionUtils.isEmpty((List<String>) searchObj.get(Constants.IDENTIFIER))) {
                        return (List<String>) searchObj.get(Constants.IDENTIFIER);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(String.format("Failed to process the questionList request body. %s", e.getMessage()));
        }
        return Collections.emptyList();
    }

    /**
     * Shared by EncryptionServiceImpl and DecryptionServiceImpl, which each carried an identical
     * copy of this algorithm/key-generation logic.
     */
    public static final String ENCRYPTION_ALGORITHM = "AES";
    private static final byte[] ENCRYPTION_KEY_VALUE =
            new byte[]{'T', 'h', 'i', 's', 'A', 's', 'I', 'S', 'e', 'r', 'c', 'e', 'K', 't', 'e', 'y'};

    public static Key generateEncryptionKey() {
        return new SecretKeySpec(ENCRYPTION_KEY_VALUE, ENCRYPTION_ALGORITHM);
    }

}