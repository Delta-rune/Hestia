package com.hestia.vault;

import com.hestia.vault.ai.HestiaAiService;
import com.hestia.vault.ai.HestiaNeuralSimulator;
import com.hestia.vault.ai.HestiaPromptBuilder;
import com.hestia.vault.ai.HestiaTimeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class HestiaTimeRecognitionTest {

    @Autowired(required = false)
    private HestiaAiService hestiaAiService;

    @Test
    public void testZoneResolution() {
        ZoneId ist = HestiaTimeService.resolveZone("Asia/Kolkata");
        assertEquals("Asia/Kolkata", ist.getId());

        ZoneId fallback = HestiaTimeService.resolveZone(null);
        assertEquals("Asia/Kolkata", fallback.getId());

        ZoneId invalidFallback = HestiaTimeService.resolveZone("Invalid/NonExistent");
        assertEquals("Asia/Kolkata", invalidFallback.getId());

        ZoneId utc = HestiaTimeService.resolveZone("UTC");
        assertEquals("UTC", utc.getId());
    }

    @Test
    public void testTimeQueryDetection() {
        assertTrue(HestiaTimeService.isTimeQuery("what time is it?"));
        assertTrue(HestiaTimeService.isTimeQuery("what is the time right now"));
        assertTrue(HestiaTimeService.isTimeQuery("tell me the time"));
        assertTrue(HestiaTimeService.isTimeQuery("what's the time"));
        assertTrue(HestiaTimeService.isTimeQuery("current time"));
        assertTrue(HestiaTimeService.isTimeQuery("time check"));

        assertFalse(HestiaTimeService.isTimeQuery("what is your name?"));
        assertFalse(HestiaTimeService.isTimeQuery("can you help me with my transcript?"));
    }

    @Test
    public void testBuildTimeResponse() {
        HestiaNeuralSimulator.SimulationResult replyCreator = HestiaTimeService.buildTimeResponse("what time is it", "Asia/Kolkata", true, "Nichu");
        assertNotNull(replyCreator);
        assertNotNull(replyCreator.getReply());
        assertTrue(replyCreator.getReply().contains("Nichu") || replyCreator.getReply().contains("time") || replyCreator.getReply().contains(":"));

        HestiaNeuralSimulator.SimulationResult replyUser = HestiaTimeService.buildTimeResponse("what is the time", "Asia/Kolkata", false, "Alex");
        assertNotNull(replyUser);
        assertNotNull(replyUser.getReply());
        assertTrue(replyUser.getReply().contains("time") || replyUser.getReply().contains("IST") || replyUser.getReply().contains(":"));
    }

    @Test
    public void testPromptBuilderContainsAccurateGrounding() {
        String prompt = HestiaPromptBuilder.buildSystemPrompt("Nichu", "nichuag33@gmail.com", 
            Map.of("degreeField", "MCA", "institution", "MBCET", "cgpa", "8.5"), 
            "hestia.paranoia@gmail.com", "", "", null, Collections.emptyList(), "Asia/Kolkata");

        assertNotNull(prompt);
        assertTrue(prompt.contains("TEMPORAL & ENVIRONMENTAL GROUNDING"));
        assertTrue(prompt.contains("CURRENT USER LOCAL TIME"));
        assertTrue(prompt.contains("Asia/Kolkata"));
    }

    @Test
    public void testNeuralSimulatorTimeAwarenessAt1230PM() {
        // Simulate a daytime query at 12:30 PM (hour 12)
        HestiaNeuralSimulator.SimulationResult result = HestiaNeuralSimulator.simulateResponse(
            "hi hestia", Collections.emptyList(), "Nichu", "nichuag33@gmail.com", true, 
            Collections.emptyMap(), "", "", "Asia/Kolkata", "12:30 PM");

        assertNotNull(result);
        assertNotNull(result.getReply());
        // Must NOT falsely claim 4:00 AM
        assertFalse(result.getReply().toLowerCase().contains("4:00 am"));
        assertFalse(result.getReply().toLowerCase().contains("4.00 am"));
    }

    @Test
    public void testAiServiceDirectTimeQueryHandling() {
        if (hestiaAiService != null) {
            Map<String, Object> resp = hestiaAiService.generateEnrichedResponse(
                "what time is it?", "Nichu", "nichuag33@gmail.com", Collections.emptyList(), 
                Collections.emptyMap(), "OFF", "Asia/Kolkata", "12:30 PM");

            assertNotNull(resp);
            String reply = (String) resp.get("reply");
            assertNotNull(reply);
            assertTrue(reply.contains(":") || reply.contains("M"));
            assertFalse(reply.toLowerCase().contains("4:00 am"));
        }
    }
}
