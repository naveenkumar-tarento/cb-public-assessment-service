package com.assessment.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerPropertiesTest {

    private ServerProperties props;

    @BeforeEach
    void setUp() {
        props = new ServerProperties();
    }

    @Test
    void shouldGetAndSetSimpleStringProperties() {
        props.setAssessmentHost("host1");
        assertEquals("host1", props.getAssessmentHost());

        props.setAssessmentHierarchyReadPath("path1");
        assertEquals("path1", props.getAssessmentHierarchyReadPath());

        props.setSbApiKey("key1");
        assertEquals("key1", props.getSbApiKey());

        props.setAssessmentQuestionListPath("qpath");
        assertEquals("qpath", props.getAssessmentQuestionListPath());

        props.setAssessmentHierarchyNameSpace("ns1");
        assertEquals("ns1", props.getAssessmentHierarchyNameSpace());

        props.setAssessmentHierarchyTable("table1");
        assertEquals("table1", props.getAssessmentHierarchyTable());

        props.setAssessmentMinQuestionParams("minQ");
        assertEquals("minQ", props.getAssessmentMinQuestionParams());

        props.setAssessmentUserSubmitDataTable("submitTable");
        assertEquals("submitTable", props.getAssessmentUserSubmitDataTable());

        props.setUserAssessmentSubmissionDuration("30");
        assertEquals("30", props.getUserAssessmentSubmissionDuration());

        props.setRedisDataHostName("redisDataHost");
        assertEquals("redisDataHost", props.getRedisDataHostName());

        props.setRedisDataPort("6380");
        assertEquals("6380", props.getRedisDataPort());

        props.setRedisTimeout("5000");
        assertEquals("5000", props.getRedisTimeout());

        props.setRedisWheeboxKey("wheeboxKey");
        assertEquals("wheeboxKey", props.getRedisWheeboxKey());

        props.setRedisHostName("redisHost");
        assertEquals("redisHost", props.getRedisHostName());

        props.setRedisPort("6379");
        assertEquals("6379", props.getRedisPort());

        props.setEncryptionKey("encKey");
        assertEquals("encKey", props.getEncryptionKey());

        props.setSvgTemplate("svg");
        assertEquals("svg", props.getSvgTemplate());

        props.setContentHierarchyNamespace("contentNs");
        assertEquals("contentNs", props.getContentHierarchyNamespace());

        props.setContentHierarchyTable("contentTable");
        assertEquals("contentTable", props.getContentHierarchyTable());

        props.setKafkaTopicsPublicAssessmentCertificate("topic1");
        assertEquals("topic1", props.getKafkaTopicsPublicAssessmentCertificate());

        props.setPublicUserAssessmentData("pubData");
        assertEquals("pubData", props.getPublicUserAssessmentData());

        props.setNotifyServiceHost("notifyHost");
        assertEquals("notifyHost", props.getNotifyServiceHost());

        props.setNotifyServicePathAsync("asyncPath");
        assertEquals("asyncPath", props.getNotifyServicePathAsync());

        props.setSupportEmail("support@example.com");
        assertEquals("support@example.com", props.getSupportEmail());

        props.setPublicAssessmentCertificateTemplate("certTemplate");
        assertEquals("certTemplate", props.getPublicAssessmentCertificateTemplate());

        props.setKafkaTopicCertificateAssessmentGroup("group1");
        assertEquals("group1", props.getKafkaTopicCertificateAssessmentGroup());

        props.setKafkaTopicCertificateRequest("requestTopic");
        assertEquals("requestTopic", props.getKafkaTopicCertificateRequest());

        props.setPublicAssessmentCompletionTemplate("completionTemplate");
        assertEquals("completionTemplate", props.getPublicAssessmentCompletionTemplate());

        props.setPublicAccessUrl("http://public.url");
        assertEquals("http://public.url", props.getPublicAccessUrl());

        props.setSpringKafkaPublicAssessmentNotificationTopicName("springTopic");
        assertEquals("springTopic", props.getSpringKafkaPublicAssessmentNotificationTopicName());

        props.setPublicAssessmentCertGenerationPostProcessTopic("postProcessTopic");
        assertEquals("postProcessTopic", props.getPublicAssessmentCertGenerationPostProcessTopic());

        props.setCloudStorageUrl("http://cloud.url");
        assertEquals("http://cloud.url", props.getCloudStorageUrl());
    }

    @Test
    void shouldGetAndSetBooleanFlag() {
        props.setqListFromCacheEnabled(true);
        assertTrue(props.qListFromCacheEnabled());
    }

    @Test
    void shouldGetAndSetIntegerTimeout() {
        props.setRedisQuestionsReadTimeOut(5000);
        assertEquals(5000, props.getRedisQuestionsReadTimeOut());
    }

    @Test
    void shouldSplitCommaSeparatedLevelParams() {
        props.setAssessmentLevelParams("a,b,c");
        assertEquals(Arrays.asList("a", "b", "c"), props.getAssessmentLevelParams());
    }

    @Test
    void shouldSplitCommaSeparatedSectionParams() {
        props.setAssessmentSectionParams("x,y");
        assertEquals(Arrays.asList("x", "y"), props.getAssessmentSectionParams());
    }

    @Test
    void shouldSplitCommaSeparatedQuestionParams() {
        props.setAssessmentQuestionParams("p,q,r");
        assertEquals(Arrays.asList("p", "q", "r"), props.getAssessmentQuestionParams());
    }

    @Test
    void shouldSplitCommaSeparatedPrimaryKeyNotAllowedCertificate() {
        props.setAssessmentPrimaryKeyNotAllowedCertificate("id,userId");
        assertEquals(Arrays.asList("id", "userId"), props.getAssessmentPrimaryKeyNotAllowedCertificate());
    }
}
