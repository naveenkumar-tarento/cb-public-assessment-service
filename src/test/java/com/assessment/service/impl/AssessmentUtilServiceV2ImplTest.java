package com.assessment.service.impl;

import com.assessment.cache.RedisCacheMgr;
import com.assessment.cassandra.utils.CassandraOperation;
import com.assessment.service.OutboundRequestHandlerServiceImpl;
import com.assessment.util.Constants;
import com.assessment.util.ServerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentUtilServiceV2ImplTest {

    @Mock
    private ServerProperties serverProperties;

    @Mock
    private OutboundRequestHandlerServiceImpl outboundRequestHandlerService;

    @Mock
    private CassandraOperation cassandraOperation;

    @Mock
    private RedisCacheMgr redisCacheMgr;

    private final AssessmentUtilServiceV2Impl service = new AssessmentUtilServiceV2Impl();
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "serverProperties", serverProperties);
        ReflectionTestUtils.setField(service, "outboundRequestHandlerService", outboundRequestHandlerService);
        ReflectionTestUtils.setField(service, "mapper", mapper);
        ReflectionTestUtils.setField(service, "cassandraOperation", cassandraOperation);
        ReflectionTestUtils.setField(service, "redisCacheMgr", redisCacheMgr);
    }

    private Map<String, Object> mcqOption(String index, boolean selected) {
        Map<String, Object> option = new HashMap<>();
        option.put(Constants.INDEX, index);
        option.put(Constants.SELECTED_ANSWER, selected);
        return option;
    }

    private Map<String, Object> mcqAnswerOption(String value, boolean isAnswer) {
        Map<String, Object> option = new HashMap<>();
        Map<String, Object> valueObj = new HashMap<>();
        valueObj.put(Constants.VALUE, value);
        option.put(Constants.VALUE, valueObj);
        option.put(Constants.ANSWER, isAnswer);
        return option;
    }

    private Map<String, Object> userQuestion(String id, String questionType, List<Map<String, Object>> options) {
        Map<String, Object> question = new HashMap<>();
        question.put(Constants.IDENTIFIER, id);
        question.put(Constants.QUESTION_TYPE, questionType);
        Map<String, Object> editorState = new HashMap<>();
        editorState.put(Constants.OPTIONS, options);
        question.put(Constants.EDITOR_STATE, editorState);
        return question;
    }

    private Map<String, Object> answerQuestion(String id, String questionType, List<Map<String, Object>> options) {
        Map<String, Object> question = new HashMap<>();
        question.put(Constants.IDENTIFIER, id);
        question.put(Constants.QUESTION_TYPE, questionType);
        Map<String, Object> editorState = new HashMap<>();
        editorState.put(Constants.OPTIONS, options);
        question.put(Constants.EDITOR_STATE, editorState);
        return question;
    }

    @Test
    void validateQumlAssessmentShouldMarkCorrectAnswer() {
        Map<String, Object> userQ = userQuestion("q1", Constants.MCQ_SCA,
                Collections.singletonList(mcqOption("1", true)));
        Map<String, Object> answerQ = answerQuestion("q1", Constants.MCQ_SCA,
                Collections.singletonList(mcqAnswerOption("1", true)));
        Map<String, Object> questionMap = Collections.singletonMap("q1", answerQ);

        Map<String, Object> result = service.validateQumlAssessment(Collections.singletonList("q1"),
                Collections.singletonList(userQ), questionMap);

        assertEquals(1, result.get(Constants.CORRECT));
        assertEquals(0, result.get(Constants.INCORRECT));
        assertEquals(0, result.get(Constants.BLANK));
        assertEquals(100.0, result.get(Constants.RESULT));
    }

    @Test
    void validateQumlAssessmentShouldMarkIncorrectAnswer() {
        Map<String, Object> userQ = userQuestion("q1", Constants.MCQ_SCA,
                Collections.singletonList(mcqOption("2", true)));
        Map<String, Object> answerQ = answerQuestion("q1", Constants.MCQ_SCA,
                Collections.singletonList(mcqAnswerOption("1", true)));
        Map<String, Object> questionMap = Collections.singletonMap("q1", answerQ);

        Map<String, Object> result = service.validateQumlAssessment(Collections.singletonList("q1"),
                Collections.singletonList(userQ), questionMap);

        assertEquals(0, result.get(Constants.CORRECT));
        assertEquals(1, result.get(Constants.INCORRECT));
        assertEquals(0.0, result.get(Constants.RESULT));
    }

    @Test
    void validateQumlAssessmentShouldMarkBlankWhenNoOptionSelected() {
        Map<String, Object> userQ = userQuestion("q1", Constants.MCQ_SCA,
                Collections.singletonList(mcqOption("1", false)));
        Map<String, Object> answerQ = answerQuestion("q1", Constants.MCQ_SCA,
                Collections.singletonList(mcqAnswerOption("1", true)));
        Map<String, Object> questionMap = Collections.singletonMap("q1", answerQ);

        Map<String, Object> result = service.validateQumlAssessment(Collections.singletonList("q1"),
                Collections.singletonList(userQ), questionMap);

        assertEquals(1, result.get(Constants.BLANK));
        assertEquals(Constants.BLANK, userQ.get(Constants.RESULT));
    }

    @Test
    void validateQumlAssessmentShouldCountSkippedQuestionsAsBlank() {
        Map<String, Object> userQ = userQuestion("q1", Constants.MCQ_SCA,
                Collections.singletonList(mcqOption("1", true)));
        Map<String, Object> answerQ1 = answerQuestion("q1", Constants.MCQ_SCA,
                Collections.singletonList(mcqAnswerOption("1", true)));
        Map<String, Object> answerQ2 = answerQuestion("q2", Constants.MCQ_SCA,
                Collections.singletonList(mcqAnswerOption("1", true)));
        Map<String, Object> questionMap = new HashMap<>();
        questionMap.put("q1", answerQ1);
        questionMap.put("q2", answerQ2);

        Map<String, Object> result = service.validateQumlAssessment(Arrays.asList("q1", "q2"),
                Collections.singletonList(userQ), questionMap);

        assertEquals(1, result.get(Constants.CORRECT));
        assertEquals(1, result.get(Constants.BLANK));
    }

    @Test
    void validateQumlAssessmentShouldHandleMtfQuestionType() {
        Map<String, Object> userOption = new HashMap<>();
        userOption.put(Constants.INDEX, "1");
        userOption.put(Constants.SELECTED_ANSWER, "a");
        Map<String, Object> userQ = userQuestion("q1", Constants.MTF, Collections.singletonList(userOption));

        Map<String, Object> ansOption = new HashMap<>();
        Map<String, Object> valueObj = new HashMap<>();
        valueObj.put(Constants.VALUE, "1");
        ansOption.put(Constants.VALUE, valueObj);
        ansOption.put(Constants.ANSWER, "a");
        Map<String, Object> answerQ = answerQuestion("q1", Constants.MTF, Collections.singletonList(ansOption));

        Map<String, Object> result = service.validateQumlAssessment(Collections.singletonList("q1"),
                Collections.singletonList(userQ), Collections.singletonMap("q1", answerQ));

        assertEquals(1, result.get(Constants.CORRECT));
    }

    @Test
    void validateQumlAssessmentShouldHandleFtbQuestionType() {
        Map<String, Object> userOption = new HashMap<>();
        userOption.put(Constants.SELECTED_ANSWER, "paris");
        Map<String, Object> userQ = userQuestion("q1", Constants.FTB, Collections.singletonList(userOption));

        Map<String, Object> ansOption = new HashMap<>();
        ansOption.put(Constants.ANSWER, true);
        Map<String, Object> valueObj = new HashMap<>();
        valueObj.put(Constants.BODY, "paris");
        ansOption.put(Constants.VALUE, valueObj);
        Map<String, Object> answerQ = answerQuestion("q1", Constants.FTB, Collections.singletonList(ansOption));

        Map<String, Object> result = service.validateQumlAssessment(Collections.singletonList("q1"),
                Collections.singletonList(userQ), Collections.singletonMap("q1", answerQ));

        assertEquals(1, result.get(Constants.CORRECT));
    }

    @Test
    void validateQumlAssessmentShouldHandleLegacyQuestionsWithoutQuestionType() {
        Map<String, Object> userOption = new HashMap<>();
        userOption.put(Constants.INDEX, "1");
        userOption.put(Constants.SELECTED_ANSWER, true);
        Map<String, Object> userQ = new HashMap<>();
        userQ.put(Constants.IDENTIFIER, "q1");
        // No QUESTION_TYPE key -> marked stays empty -> treated as blank.

        Map<String, Object> ansOption = new HashMap<>();
        ansOption.put(Constants.IS_CORRECT, true);
        ansOption.put(Constants.OPTION_ID, "opt1");
        Map<String, Object> answerQ = new HashMap<>();
        answerQ.put(Constants.IDENTIFIER, "q1");
        answerQ.put(Constants.OPTIONS, Collections.singletonList(ansOption));

        Map<String, Object> result = service.validateQumlAssessment(Collections.singletonList("q1"),
                Collections.singletonList(userQ), Collections.singletonMap("q1", answerQ));

        assertEquals(1, result.get(Constants.BLANK));
    }

    @Test
    void validateQumlAssessmentShouldReturnEmptyMapOnException() {
        Map<String, Object> userQ = userQuestion("q1", Constants.MCQ_SCA,
                Collections.singletonList(mcqOption("1", true)));
        Map<String, Object> result = service.validateQumlAssessment(Collections.singletonList("q1"),
                Collections.singletonList(userQ), Collections.emptyMap());

        assertTrue(result.isEmpty());
    }

    @Test
    void filterQuestionMapDetailShouldIncludeEditorStateForPracticeCategory() {
        when(serverProperties.getAssessmentQuestionParams())
                .thenReturn(Arrays.asList(Constants.IDENTIFIER, Constants.PRIMARY_CATEGORY));

        Map<String, Object> question = new HashMap<>();
        question.put(Constants.IDENTIFIER, "q1");
        question.put(Constants.PRIMARY_CATEGORY, "Practice");
        Map<String, Object> editorState = Collections.singletonMap("key", "value");
        question.put(Constants.EDITOR_STATE, editorState);

        Map<String, Object> result = service.filterQuestionMapDetail(question, Constants.PRACTICE_QUESTION_SET);

        assertEquals(editorState, result.get(Constants.EDITOR_STATE));
    }

    @Test
    void filterQuestionMapDetailShouldShuffleChoicesForNonFtbCategory() {
        when(serverProperties.getAssessmentQuestionParams())
                .thenReturn(Arrays.asList(Constants.IDENTIFIER, Constants.PRIMARY_CATEGORY));

        Map<String, Object> question = new HashMap<>();
        question.put(Constants.IDENTIFIER, "q1");
        question.put(Constants.PRIMARY_CATEGORY, "mcq-sca");
        Map<String, Object> choices = new HashMap<>();
        List<Map<String, Object>> options = Arrays.asList(Collections.singletonMap("a", "1"),
                Collections.singletonMap("b", "2"));
        choices.put(Constants.OPTIONS, options);
        question.put(Constants.CHOICES, choices);

        Map<String, Object> result = service.filterQuestionMapDetail(question, "Assessment");

        Map<String, Object> resultChoices = (Map<String, Object>) result.get(Constants.CHOICES);
        List<Map<String, Object>> shuffled = (List<Map<String, Object>>) resultChoices.get(Constants.OPTIONS);
        assertEquals(2, shuffled.size());
    }

    @Test
    void filterQuestionMapDetailShouldShuffleRhsChoicesForMtfCategory() {
        when(serverProperties.getAssessmentQuestionParams())
                .thenReturn(Arrays.asList(Constants.IDENTIFIER, Constants.PRIMARY_CATEGORY));

        Map<String, Object> question = new HashMap<>();
        question.put(Constants.IDENTIFIER, "q1");
        question.put(Constants.PRIMARY_CATEGORY, Constants.MTF_QUESTION);
        question.put(Constants.RHS_CHOICES, new ArrayList<>(Arrays.asList("a", "b", "c")));

        Map<String, Object> result = service.filterQuestionMapDetail(question, "Assessment");

        List<Object> rhs = (List<Object>) result.get(Constants.RHS_CHOICES);
        assertEquals(3, rhs.size());
    }

    @Test
    void filterQuestionMapDetailV2ShouldShuffleChoicesRegardlessOfCategory() {
        when(serverProperties.getAssessmentQuestionParams())
                .thenReturn(Arrays.asList(Constants.IDENTIFIER, Constants.PRIMARY_CATEGORY));

        Map<String, Object> question = new HashMap<>();
        question.put(Constants.IDENTIFIER, "q1");
        question.put(Constants.PRIMARY_CATEGORY, Constants.FTB_QUESTION);
        Map<String, Object> choices = new HashMap<>();
        choices.put(Constants.OPTIONS, Arrays.asList(Collections.singletonMap("a", "1")));
        question.put(Constants.CHOICES, choices);

        Map<String, Object> result = service.filterQuestionMapDetailV2(question, "Assessment");

        assertTrue(result.containsKey(Constants.CHOICES));
    }

    @Test
    void shuffleOptionsShouldReturnNewListWithSameElements() {
        List<Map<String, Object>> original = Arrays.asList(Collections.singletonMap("a", "1"),
                Collections.singletonMap("b", "2"), Collections.singletonMap("c", "3"));

        List<Map<String, Object>> shuffled = AssessmentUtilServiceV2Impl.shuffleOptions(original);

        assertEquals(3, shuffled.size());
        assertTrue(shuffled.containsAll(original));
    }

    @Test
    void readQuestionDetailsShouldChunkRequestsAndCollectResults() {
        List<String> identifiers = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            identifiers.add("q" + i);
        }
        when(serverProperties.getAssessmentHost()).thenReturn("http://host");
        when(serverProperties.getAssessmentQuestionListPath()).thenReturn("/path");
        when(serverProperties.getSbApiKey()).thenReturn("api-key");
        when(outboundRequestHandlerService.fetchResultUsingPost(anyString(), any(), any()))
                .thenReturn(Collections.singletonMap(Constants.RESPONSE_CODE, Constants.OK));

        List<Map<String, Object>> result = service.readQuestionDetails(identifiers);

        assertEquals(2, result.size());
    }

    @Test
    void readQuestionDetailsShouldReturnEmptyListOnException() {
        when(serverProperties.getAssessmentHost()).thenThrow(new RuntimeException("config missing"));

        List<Map<String, Object>> result = service.readQuestionDetails(Collections.singletonList("q1"));

        assertTrue(result.isEmpty());
    }

    @Test
    void getReadHierarchyApiResponseShouldReturnMapFromOutboundCall() {
        when(serverProperties.getAssessmentHost()).thenReturn("http://host");
        when(serverProperties.getAssessmentHierarchyReadPath()).thenReturn("/path/{identifier}".replace("{identifier}", Constants.IDENTIFIER_REPLACER));
        when(serverProperties.getSbApiKey()).thenReturn("api-key");
        Map<String, Object> apiResponse = Collections.singletonMap(Constants.RESPONSE_CODE, Constants.OK);
        when(outboundRequestHandlerService.fetchUsingGetWithHeaders(anyString(), any())).thenReturn(apiResponse);

        Map<String, Object> result = service.getReadHierarchyApiResponse("assess1");

        assertEquals(Constants.OK, result.get(Constants.RESPONSE_CODE));
    }

    @Test
    void fetchHierarchyFromAssessServcShouldReturnQuestionSetOnSuccess() {
        when(serverProperties.getAssessmentHost()).thenReturn("http://host");
        when(serverProperties.getAssessmentHierarchyReadPath()).thenReturn(Constants.IDENTIFIER_REPLACER);
        when(serverProperties.getSbApiKey()).thenReturn("api-key");

        Map<String, Object> questionSet = Collections.singletonMap("key", "value");
        Map<String, Object> result = Collections.singletonMap(Constants.QUESTION_SET, questionSet);
        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put(Constants.RESPONSE_CODE, Constants.OK);
        apiResponse.put(Constants.RESULT, result);
        when(outboundRequestHandlerService.fetchUsingGetWithHeaders(anyString(), any())).thenReturn(apiResponse);

        Map<String, Object> actual = service.fetchHierarchyFromAssessServc("assess1");

        assertEquals(questionSet, actual);
    }

    @Test
    void fetchHierarchyFromAssessServcShouldThrowWhenResponseCodeNotOk() {
        when(serverProperties.getAssessmentHost()).thenReturn("http://host");
        when(serverProperties.getAssessmentHierarchyReadPath()).thenReturn(Constants.IDENTIFIER_REPLACER);
        when(serverProperties.getSbApiKey()).thenReturn("api-key");

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put(Constants.RESPONSE_CODE, "FAILED");
        when(outboundRequestHandlerService.fetchUsingGetWithHeaders(anyString(), any())).thenReturn(apiResponse);

        assertThrows(RuntimeException.class, () -> service.fetchHierarchyFromAssessServc("assess1"));
    }

    @Test
    void readAssessmentHierarchyFromCacheShouldDelegateToFetchHierarchyWhenEditMode() {
        when(serverProperties.getAssessmentHost()).thenReturn("http://host");
        when(serverProperties.getAssessmentHierarchyReadPath()).thenReturn(Constants.IDENTIFIER_REPLACER);
        when(serverProperties.getSbApiKey()).thenReturn("api-key");

        Map<String, Object> questionSet = Collections.singletonMap("key", "value");
        Map<String, Object> result = Collections.singletonMap(Constants.QUESTION_SET, questionSet);
        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put(Constants.RESPONSE_CODE, Constants.OK);
        apiResponse.put(Constants.RESULT, result);
        when(outboundRequestHandlerService.fetchUsingGetWithHeaders(anyString(), any())).thenReturn(apiResponse);

        Map<String, Object> actual = service.readAssessmentHierarchyFromCache("assess1", true);

        assertEquals(questionSet, actual);
    }

    @Test
    void readAssessmentHierarchyFromCacheShouldReturnFromRedisWhenCacheEnabledAndHit() throws Exception {
        when(serverProperties.qListFromCacheEnabled()).thenReturn(true);
        Map<String, Object> cachedHierarchy = Collections.singletonMap("key", "cachedValue");
        when(redisCacheMgr.getCache(anyString())).thenReturn(mapper.writeValueAsString(cachedHierarchy));

        Map<String, Object> result = service.readAssessmentHierarchyFromCache("assess1", false);

        assertEquals("cachedValue", result.get("key"));
    }

    @Test
    void readAssessmentHierarchyFromCacheShouldThrowRuntimeExceptionOnInvalidCachedJson() {
        when(serverProperties.qListFromCacheEnabled()).thenReturn(true);
        when(redisCacheMgr.getCache(anyString())).thenReturn("not-valid-json");

        assertThrows(RuntimeException.class, () -> service.readAssessmentHierarchyFromCache("assess1", false));
    }

    @Test
    void readAssessmentHierarchyFromCacheShouldReadFromCassandraWhenCacheDisabled() throws Exception {
        when(serverProperties.qListFromCacheEnabled()).thenReturn(false);
        when(serverProperties.getAssessmentHierarchyNameSpace()).thenReturn("ns");
        when(serverProperties.getAssessmentHierarchyTable()).thenReturn("tbl");
        when(serverProperties.getRedisQuestionsReadTimeOut()).thenReturn(1000);

        Map<String, Object> hierarchyData = Collections.singletonMap("key", "dbValue");
        Map<String, Object> dbRow = new HashMap<>();
        dbRow.put(Constants.HIERARCHY, mapper.writeValueAsString(hierarchyData));
        when(cassandraOperation.getRecordsByPropertiesWithoutFiltering(anyString(), anyString(), anyMap(), any()))
                .thenReturn(Collections.singletonList(dbRow));

        Map<String, Object> result = service.readAssessmentHierarchyFromCache("assess1", false);

        assertEquals("dbValue", result.get("key"));
        verify(redisCacheMgr).putCache(anyString(), any(), anyInt());
    }

    @Test
    void readAssessmentHierarchyFromCacheShouldReturnEmptyMapWhenNoDbRecords() {
        when(serverProperties.qListFromCacheEnabled()).thenReturn(false);
        when(serverProperties.getAssessmentHierarchyNameSpace()).thenReturn("ns");
        when(serverProperties.getAssessmentHierarchyTable()).thenReturn("tbl");
        when(cassandraOperation.getRecordsByPropertiesWithoutFiltering(anyString(), anyString(), anyMap(), any()))
                .thenReturn(Collections.emptyList());

        Map<String, Object> result = service.readAssessmentHierarchyFromCache("assess1", false);

        assertTrue(result.isEmpty());
    }

    @Test
    void readUserSubmittedAssessmentRecordsShouldDelegateToCassandraOperation() {
        when(serverProperties.getPublicUserAssessmentData()).thenReturn("tbl");
        List<Map<String, Object>> expected = Collections.singletonList(Collections.singletonMap("key", "value"));
        when(cassandraOperation.getRecordsByPropertiesWithoutFiltering(anyString(), anyString(), anyMap(), any()))
                .thenReturn(expected);

        List<Map<String, Object>> result = service.readUserSubmittedAssessmentRecords("user@example.com", "assess1", "ctx1");

        assertEquals(expected, result);
    }

    @Test
    void readQListfromCacheShouldDelegateToQListFromCacheWhenEnabled() throws Exception {
        when(serverProperties.qListFromCacheEnabled()).thenReturn(true);
        Map<String, Object> cachedQuestions = Collections.singletonMap("q1", Collections.singletonMap("id", "q1"));
        when(redisCacheMgr.getCache(anyString())).thenReturn(mapper.writeValueAsString(cachedQuestions));

        Map<String, Object> result = service.readQListfromCache(Collections.singletonList("q1"), "assess1", false);

        assertTrue(result.containsKey("q1"));
    }

    @Test
    void readQListfromCacheShouldDelegateToAssessServiceWhenCacheDisabled() throws Exception {
        when(serverProperties.qListFromCacheEnabled()).thenReturn(false);
        when(serverProperties.getAssessmentHost()).thenReturn("http://host");
        when(serverProperties.getAssessmentQuestionListPath()).thenReturn("/path");
        when(serverProperties.getSbApiKey()).thenReturn("api-key");

        Map<String, Object> questionsResult = new HashMap<>();
        Map<String, Object> question = Collections.singletonMap(Constants.IDENTIFIER, "q1");
        questionsResult.put(Constants.QUESTIONS, Collections.singletonList(question));
        Map<String, Object> apiResponse = Collections.singletonMap(Constants.RESULT, questionsResult);
        when(outboundRequestHandlerService.fetchResultUsingPost(anyString(), any(), any())).thenReturn(apiResponse);

        Map<String, Object> result = service.readQListfromCache(Collections.singletonList("q1"), "assess1", false);

        assertTrue(result.containsKey("q1"));
    }

    @Test
    void qListFromCacheShouldReturnFromRedisWhenHit() throws Exception {
        Map<String, Object> cachedQuestions = Collections.singletonMap("q1", Collections.singletonMap("id", "q1"));
        when(redisCacheMgr.getCache(anyString())).thenReturn(mapper.writeValueAsString(cachedQuestions));

        Map<String, Object> result = service.qListFromCache("assess1", false);

        assertTrue(result.containsKey("q1"));
    }

    @Test
    void qListFromCacheShouldReturnEmptyWhenHierarchyEmpty() throws Exception {
        when(redisCacheMgr.getCache(anyString())).thenReturn(null);
        when(serverProperties.qListFromCacheEnabled()).thenReturn(true);
        when(redisCacheMgr.getCache(Constants.ASSESSMENT_ID + "assess1" + Constants.UNDER_SCORE + Constants.QUESTION_SET))
                .thenReturn(null);

        Map<String, Object> result = service.qListFromCache("assess1", false);

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchWheeboxShouldReturnParsedDataWhenCacheHit() throws Exception {
        when(serverProperties.getRedisWheeboxKey()).thenReturn("wheebox");
        Map<String, Object> cached = Collections.singletonMap("key", "value");
        when(redisCacheMgr.getContentFromCache(anyString())).thenReturn(mapper.writeValueAsString(cached));

        Map<String, Object> result = service.fetchWheebox("user1");

        assertEquals("value", result.get("key"));
    }

    @Test
    void fetchWheeboxShouldReturnEmptyMapWhenCacheMiss() {
        when(serverProperties.getRedisWheeboxKey()).thenReturn("wheebox");
        when(redisCacheMgr.getContentFromCache(anyString())).thenReturn(null);

        Map<String, Object> result = service.fetchWheebox("user1");

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchWheeboxShouldThrowRuntimeExceptionOnInvalidJson() {
        when(serverProperties.getRedisWheeboxKey()).thenReturn("wheebox");
        when(redisCacheMgr.getContentFromCache(anyString())).thenReturn("not-json");

        assertThrows(RuntimeException.class, () -> service.fetchWheebox("user1"));
    }

    @Test
    void validateQumlAssessmentV2ShouldComputeQuestionWeightageScoring() {
        Map<String, Object> questionSetSectionScheme = new HashMap<>();
        questionSetSectionScheme.put("easy", 10);

        Map<String, Object> questionSetDetailsMap = new HashMap<>();
        questionSetDetailsMap.put(Constants.ASSESSMENT_TYPE, Constants.QUESTION_WEIGHTAGE);
        questionSetDetailsMap.put(Constants.MINIMUM_PASS_PERCENTAGE, 40);
        questionSetDetailsMap.put(Constants.TOTAL_MARKS, 10);
        questionSetDetailsMap.put(Constants.QUESTION_SECTION_SCHEME, questionSetSectionScheme);
        questionSetDetailsMap.put(Constants.NEGATIVE_MARKING_PERCENTAGE, "0%");

        Map<String, Object> userQ = userQuestion("q1", Constants.MCQ_SCA,
                Collections.singletonList(mcqOption("1", true)));
        Map<String, Object> answerQ = answerQuestion("q1", Constants.MCQ_SCA,
                Collections.singletonList(mcqAnswerOption("1", true)));
        // getProficiencyMap looks up question level directly on the same questionMap entry.
        answerQ.put(Constants.QUESTION_LEVEL, "easy");

        Map<String, Object> questionMap = new HashMap<>();
        questionMap.put("q1", answerQ);

        Map<String, Object> result = service.validateQumlAssessmentV2(questionSetDetailsMap,
                Collections.singletonList("q1"), Collections.singletonList(userQ), questionMap);

        assertEquals(1, result.get(Constants.CORRECT));
        assertEquals(Constants.PASS, result.get(Constants.SECTION_RESULT));
    }

    @Test
    void validateQumlAssessmentV2ShouldComputeOptionWeightageScoring() {
        Map<String, Object> questionSetDetailsMap = new HashMap<>();
        questionSetDetailsMap.put(Constants.ASSESSMENT_TYPE, Constants.OPTION_WEIGHTAGE);
        questionSetDetailsMap.put(Constants.MINIMUM_PASS_PERCENTAGE, 40);
        questionSetDetailsMap.put(Constants.TOTAL_MARKS, 10);

        // MCQ_MCA_W is not handled by getQumlAnswers' correct-answer extraction switch (falls to
        // default), so it avoids the ANSWER-as-boolean cast that only applies to MCQ_SCA/MCQ_MCA.
        Map<String, Object> userOption = new HashMap<>();
        userOption.put(Constants.INDEX, "1");
        Map<String, Object> userQ = userQuestion("q1", Constants.MCQ_MCA_W, Collections.singletonList(userOption));

        Map<String, Object> answerOption = new HashMap<>();
        Map<String, Object> valueObj = new HashMap<>();
        valueObj.put(Constants.VALUE, "1");
        answerOption.put(Constants.VALUE, valueObj);
        answerOption.put(Constants.ANSWER, 5);
        Map<String, Object> answerQ = answerQuestion("q1", Constants.MCQ_MCA_W,
                Collections.singletonList(answerOption));
        answerQ.put(Constants.QUESTION_LEVEL, "easy");

        Map<String, Object> questionMap = new HashMap<>();
        questionMap.put("q1", answerQ);

        Map<String, Object> result = service.validateQumlAssessmentV2(questionSetDetailsMap,
                Collections.singletonList("q1"), Collections.singletonList(userQ), questionMap);

        assertTrue(result.containsKey(Constants.SECTION_RESULT));
    }

    @Test
    void validateQumlAssessmentV2ShouldReturnEmptyMapOnException() {
        Map<String, Object> questionSetDetailsMap = new HashMap<>();
        // Missing ASSESSMENT_TYPE triggers a NullPointerException internally, which is caught.
        Map<String, Object> result = service.validateQumlAssessmentV2(questionSetDetailsMap,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());

        assertTrue(result.isEmpty());
    }
}
