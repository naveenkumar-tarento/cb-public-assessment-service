package com.assessment.repo;

import com.assessment.cassandra.utils.CassandraOperation;
import com.assessment.model.SBApiResponse;
import com.assessment.util.Constants;
import com.assessment.util.ServerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentRepositoryImplTest {

    @Mock
    private CassandraOperation cassandraOperation;

    @Mock
    private ServerProperties serverProperties;

    private AssessmentRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new AssessmentRepositoryImpl();
        repository.cassandraOperation = cassandraOperation;
        repository.serverProperties = serverProperties;
        lenient().when(serverProperties.getPublicUserAssessmentData()).thenReturn("public_user_assessment_data_test");
    }

    @Test
    void addUserAssesmentDataToDBShouldReturnTrueOnSuccess() {
        SBApiResponse response = new SBApiResponse();
        response.put(Constants.RESPONSE, Constants.SUCCESS);
        when(cassandraOperation.insertRecord(anyString(), anyString(), anyMap())).thenReturn(response);

        boolean result = repository.addUserAssesmentDataToDB("user@example.com", "assess1",
                new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()),
                Collections.singletonMap("q1", "a1"), "SUBMITTED", "John Doe", "ctx1");

        assertTrue(result);
    }

    @Test
    void addUserAssesmentDataToDBShouldReturnFalseOnFailure() {
        SBApiResponse response = new SBApiResponse();
        response.put(Constants.RESPONSE, "FAILED");
        when(cassandraOperation.insertRecord(anyString(), anyString(), anyMap())).thenReturn(response);

        boolean result = repository.addUserAssesmentDataToDB("user@example.com", "assess1",
                new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()),
                Collections.singletonMap("q1", "a1"), "SUBMITTED", "John Doe", "ctx1");

        assertEquals(false, result);
    }

    @Test
    void fetchUserAssessmentDataFromDBShouldDelegateToCassandraOperation() {
        List<Map<String, Object>> expected = Collections.singletonList(Collections.singletonMap("key", "value"));
        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap(), ArgumentMatchers.isNull()))
                .thenReturn(expected);

        List<Map<String, Object>> result = repository.fetchUserAssessmentDataFromDB("user@example.com", "assess1");

        assertEquals(expected, result);
    }

    @Test
    void updateUserAssesmentDataToDBShouldAlwaysReturnTrue() {
        Map<String, Object> submitRequest = Collections.singletonMap("req", "val");
        Map<String, Object> submitResponse = new HashMap<>();
        submitResponse.put(Constants.PASS, true);
        Map<String, Object> saveRequest = Collections.singletonMap("save", "val");

        Boolean result = repository.updateUserAssesmentDataToDB("user@example.com", "assess1", submitRequest,
                submitResponse, "SUBMITTED", new Date(), saveRequest, "ctx1");

        assertTrue(result);
    }

    @Test
    void updateUserAssesmentDataToDBShouldHandleEmptyOptionalFields() {
        Map<String, Object> submitResponse = new HashMap<>();

        Boolean result = repository.updateUserAssesmentDataToDB("user@example.com", "assess1", Collections.emptyMap(),
                submitResponse, "", new Date(), Collections.emptyMap(), "ctx1");

        assertTrue(result);
    }
}
